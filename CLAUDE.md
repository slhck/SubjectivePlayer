# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SubjectivePlayer is an Android application for conducting subjective video quality evaluation tests. It plays a playlist of videos and collects user ratings using ITU-standard methodologies (ACR, continuous rating).

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install debug APK directly to connected device
./gradlew installDebug
```

Output APK location: `app/build/outputs/apk/debug/app-debug.apk`

## Project Structure

- **Android Gradle project** using Kotlin DSL (`build.gradle.kts`)
- Target SDK 35 (Android 15), minimum SDK 24 (Android 7.0)
- Java 17, uses AndroidX and Material Design components
- View binding enabled

## Architecture

The app follows a simple activity-based architecture with static utility classes:

**Activities:**
- `SubjectivePlayerActivity` - Main activity: participant ID entry, loads config file (`subject_<ID>.json` or `.cfg`)
- `SessionActivity` - Video playback: `MediaPlayer` with `SurfaceView`, rating dialogs after each video
- `PreferencesActivity` - Settings: storage paths, UI options
- `ConfigValidatorActivity` - Validates all config files and checks for missing videos

**Config File Parsing (supports JSON and legacy text formats):**
- `BaseConfigFile` - Abstract base class with common fields and getters
- `TextConfigFile` - Parses legacy `.cfg` format (line-based directives)
- `JsonConfigFile` - Parses `.json` format using Gson (preferred format)
- `ConfigFileFactory` - Factory to create appropriate parser by file extension
- `Question` / `Questionnaire` - Data models for pre/post questionnaires (JSON only)

**Static Utility Classes:**
- `Configuration` - Storage paths and app preferences. Uses app-specific external storage (`/storage/emulated/0/Android/data/org.univie.subjectiveplayer/files/`)
- `Session` - Current test session state (participant ID, video list, ratings). Also contains directive parsing utilities.
- `CsvLogger` - Writes CSV result files to `SubjectiveLogs/`
- `Methods` - Constants for rating methodologies (ACR Categorical, Continuous, DSIS)

Strings are localized in `res/values/strings.xml`.
Translations are in `res/values-<lang>/strings.xml`. If you add/modify/remove strings, **update translations accordingly**.
Layouts are in `res/layout/`.

## Rating Methods

Defined in `Methods.java`:

- `TYPE_ACR_CATEGORICAL` (0) - 5-point quality scale (Excellent/Good/Fair/Poor/Bad)
- `TYPE_CONTINUOUS` (1) - Slider-based rating after each video
- `TYPE_DSIS_CATEGORICAL` (2) - Impairment scale (not fully implemented)
- `TYPE_TIME_CONTINUOUS` (3) - Real-time rating during playback using volume buttons

Enabled via playlist config file (e.g., `subject_1.cfg`).

## Config File Formats

The app supports two formats. **JSON is preferred** and will be extended with new features.

### JSON Format (`subject_<id>.json`)

Schema: `json-schema/subject-config.schema.json`

```json
{
  "method": "ACR",
  "custom_messages": {
    "start_message": "Welcome!",
    "finish_message": "Thank you!",
    "training_message": "Training time",
    "pre_questionnaire_message": "Please answer a few questions.",
    "post_questionnaire_message": "Please provide feedback."
  },
  "playlist": ["TRAINING_START", "training.mp4", "TRAINING_END", "video1.mp4", "BREAK 30", "video2.mp4"],
  "pre_questionnaire": [{"question": "Age?", "type": "number"}],
  "post_questionnaire": [{"question": "Comments?", "type": "text", "required": false}]
}
```

**Question types**: `number`, `radio`, `multiple-choice`, `text`. Radio/multiple-choice require `options` array.

### Legacy Text Format (`subject_<id>.cfg`)

```
METHOD ACR
START_MESSAGE Welcome!\nClick Continue.
TRAINING_START
training.mp4
TRAINING_END
video1.mp4
BREAK 30
video2.mp4
FINISH_MESSAGE Thank you!
```

**Directives**: `METHOD`, `START_MESSAGE`, `FINISH_MESSAGE`, `TRAINING_MESSAGE`, `TRAINING_START`, `TRAINING_END`, `BREAK [seconds]`

Use `\n` for line breaks in messages.

## Session Flow

1. **Pre-Questionnaire** → If defined in JSON config, shown before start screen (each question in a separate dialog)
2. **Start Screen** → Shown before first video (Continue button)
3. **Training Intro** → If training section defined, shown before first training video
4. **Training Videos** → Training videos play with ratings (same as main test)
5. **Training Complete** → Shown after last training video
6. **Video Playback** → Video plays fullscreen
7. **Rating Dialog** → User rates quality (ACR buttons or continuous slider)
8. **Break Dialog** → If `BREAK` command encountered (optional timed countdown)
9. **Repeat 6-8** → Until all videos rated
10. **Post-Questionnaire** → If defined in JSON config, shown after last video rating (each question in a separate dialog)
11. **Finish Screen** → Shown after post-questionnaire or last rating (OK button)

Questionnaire dialogs are rendered programmatically by `QuestionDialogBuilder` and support all question types (number input, radio buttons, checkboxes, text input). Each question is shown in its own dialog with a question counter.

Start/finish/training screens use custom dialogs (`dialog_start.xml`, `dialog_finish.xml`, `dialog_training_intro.xml`, `dialog_training_complete.xml`) with the same button styling as rating dialogs (120×48dp, white text on gray background).

## Storage Directories

All under app-specific storage (`ctx.getExternalFilesDir(null)`):
- `SubjectiveCfg/` - Config files (`subject_1.json`, `subject_2.cfg`, etc.). JSON preferred.
- `SubjectiveMovies/` - Video files (H.264, H.265, VP9, VP8, AV1 in MP4/WebM/MKV containers)
- `SubjectiveLogs/` - Output CSV files with ratings

## Logging Behavior

Ratings are logged continuously to the CSV file as they are collected. This prevents data loss if the test is cancelled before completion.

### Rating Log

**File naming**: `<participant_id>_<start_time>_<method>.csv` (e.g., `1_20231215-143052_ACR.csv`)

**CSV format**: `video_position,video_name,rating,rated_at,rating_duration`

The `rating_duration` column contains the time in seconds the user took to submit the rating. It is empty for time-continuous ratings (which are logged automatically during playback) and for BREAK entries.

### Questionnaire Log

Questionnaire answers are logged to separate CSV files when questionnaires are completed. Uses tidy data format with one row per answer.

**File naming**: `<participant_id>_<timestamp>_questionnaire_<type>.csv` (e.g., `1_20231215-143050_questionnaire_pre.csv`)

**CSV format**: `question_number,question_type,question,answer,answered_at,answer_duration`

- `answered_at`: ISO8601 timestamp when the question was answered
- `answer_duration`: Time in seconds the user took to answer this question

For multiple-choice questions, each selected option produces a separate row (all with the same timestamp and duration).

## Testing with Device

```bash
# Push example test files
cd examples
./push_to_device.sh
```

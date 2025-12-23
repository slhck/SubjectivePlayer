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

- `SubjectivePlayer` - Main activity: participant ID entry, loads playlist config file (`playlist<ID>.cfg`)
- `SubjectivePlayerSession` - Video playback activity: uses `MediaPlayer` with `SurfaceView`, shows rating dialogs after each video
- `Configuration` - Static class managing storage paths and app preferences. Uses app-specific external storage (`/storage/emulated/0/Android/data/org.univie.subjectiveplayer/files/`)
- `Session` - Static class holding current test session state (participant ID, video list, ratings)
- `Logger` - Static class writing CSV result files to `SubjectiveLogs/`
- `Methods` - Constants for rating methodologies (ACR Categorical, Continuous, DSIS)

Strings are localized in `res/values/strings.xml`.
Translations are in `res/values-<lang>/strings.xml`. If you add/modify/remove strings, **update translations accordingly**.
Layouts are in `res/layout/`.

## Rating Methods

Defined in `Methods.java`:
- `TYPE_ACR_CATEGORICAL` (0) - 5-point quality scale (Excellent/Good/Fair/Poor/Bad)
- `TYPE_CONTINUOUS` (1) - Slider-based rating after each video
- `TYPE_DSIS_CATEGORICAL` (2) - Impairment scale (not fully implemented)
- `TYPE_CONTINUOUS_RATING` (3) - Real-time rating during playback using volume buttons

Enabled via playlist config file (e.g., `subject_1.cfg`).

## Config File Syntax

Config files (`subject_<id>.cfg`) support the following directives:

```
METHOD ACR
START_MESSAGE Welcome to the test!\nClick Continue to begin.
TRAINING_START
training1.mp4
training2.mp4
TRAINING_END
video1.mp4
video2.mp4
BREAK 30
video3.mp4
FINISH_MESSAGE Thank you!\nPlease notify the supervisor.
```

**Directives** (parsed in `Session.readVideosFromFile()`):
- `METHOD <type>` - Rating method: `ACR`, `CONTINUOUS`, `DSIS`, `CONTINUOUS_RATING`
- `START_MESSAGE <text>` - Custom start screen message (optional)
- `FINISH_MESSAGE <text>` - Custom finish screen message (optional)
- `BREAK [seconds]` - Insert a break; if seconds specified, shows countdown timer
- `TRAINING_START` - Marks beginning of training section (must be paired with `TRAINING_END`)
- `TRAINING_END` - Marks end of training section (must be paired with `TRAINING_START`)
- `TRAINING_MESSAGE <text>` - Custom training intro message (optional)

**Newlines**: Use `\n` in message strings to insert line breaks.

If `START_MESSAGE`, `FINISH_MESSAGE`, or `TRAINING_MESSAGE` are omitted, default messages from `strings.xml` are used.

## Session Flow

1. **Start Screen** → Shown before first video (Continue button)
2. **Training Intro** → If training section defined, shown before first training video
3. **Training Videos** → Training videos play with ratings (same as main test)
4. **Training Complete** → Shown after last training video
5. **Video Playback** → Video plays fullscreen
6. **Rating Dialog** → User rates quality (ACR buttons or continuous slider)
7. **Break Dialog** → If `BREAK` command encountered (optional timed countdown)
8. **Repeat 5-7** → Until all videos rated
9. **Finish Screen** → Shown after last rating (OK button)

Start/finish/training screens use custom dialogs (`dialog_start.xml`, `dialog_finish.xml`, `dialog_training_intro.xml`, `dialog_training_complete.xml`) with the same button styling as rating dialogs (120×48dp, white text on gray background).

## Storage Directories

All under app-specific storage (`ctx.getExternalFilesDir(null)`):
- `SubjectiveCfg/` - Playlist files (`playlist1.cfg`, `playlist2.cfg`, etc.)
- `SubjectiveMovies/` - Video files (H.264, H.265, VP9, VP8, AV1 in MP4/WebM/MKV containers)
- `SubjectiveLogs/` - Output CSV files with ratings

## Testing with Device

```bash
# Push example test files
cd examples
./push_to_device.sh
```

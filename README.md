# Subjective Player

![](app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp)

A video player for Android, enabling subjective quality assessment.

Author: Werner Robitza

**Contents:**

- [Features](#features)
- [Recent Changes](#recent-changes)
- [Requirements](#requirements)
- [Installation](#installation)
- [Usage Guide](#usage-guide)
  - [Prepare Video Files](#prepare-video-files)
  - [Prepare Config Files](#prepare-config-files)
  - [Move the Files to the Device](#move-the-files-to-the-device)
  - [Validate the Config Files](#validate-the-config-files)
  - [Run the Test](#run-the-test)
  - [Obtain the Results](#obtain-the-results)
- [Output Format](#output-format)
  - [Rating Output](#rating-output)
  - [Questionnaire Output](#questionnaire-output)
- [In-App Configuration Options](#in-app-configuration-options)
  - [Display](#display)
  - [Configuration Files](#configuration-files)
  - [Time-Continuous Rating](#time-continuous-rating)
- [Developer Guide](#developer-guide)
  - [Building](#building)
  - [Testing](#testing)
- [Contributing](#contributing)
- [License](#license)

## Features

- Playback:
  - Multiple video codecs: H.264, H.265/HEVC, VP9, VP8, AV1
  - Multiple container formats: MP4, WebM, MKV, 3GP
  - Edge-to-edge playback, utilizing the full screen, or avoiding display cutouts and rounded corners
  - Config files to define which videos to show to which subjects – different playlists for different subjects may be used
- Rating:
  - After each video, subjects are asked for their opinion using different methodologies:
    - 5-point ACR (Absolute Category Rating) categorical scale (Excellent/Good/Fair/Poor/Bad) from ITU-T Rec. P.910
    - Continuous slider-based rating (0-100)
    - Continuous real-time rating using volume buttons
    - (DSIS impairment scale – ⚠️ not fully implemented)
- General:
  - Subject ratings are based on IDs, so we can identify different participants later
  - Logging of the subject ratings to CSV files
  - On-device validation of config files
  - Internationalization (i18n) support:
    - 🇺🇸 English
    - 🇩🇪 German
    - 🇫🇷 French
    - 🇮🇹 Italian
    - 🇪🇸 Spanish
    - 🇯🇵 Japanese
    - 🇰🇷 Korean
    - 🇨🇳 Chinese (Simplified)
- Session management:
  - Training sessions to familiarize subjects with the rating procedure
  - Breaks between videos to prevent viewer fatigue
  - Pre- and post-questionnaires to collect participant data

Here the ACR rating screen is shown after a video playback:

<img src="docs/subjective_player_acr.png" alt="Subjective Player ACR Rating Screen" width="500"/>

## Recent Changes

- v2.x – Complete rewrite using new Android dependencies and many more features.
- v1.1 – Commit [323123](https://github.com/slhck/SubjectivePlayer/commit/323123c43480ca5d846ac2fe56bd405bf03a3425) – updates for ITU-T Rec. P.1203 model development in 2017
- v1.0 – Commit [742593b69cc4096368d9cbcafce362a3d9d2236c](https://github.com/slhck/SubjectivePlayer/commit/742593b69cc4096368d9cbcafce362a3d9d2236c) – initial commit in 2012

For more details, see the [Changelog](CHANGELOG.md).

## Requirements

For running the app, you need:

- Android device running Android 7.0 (Nougat) or higher (API level 24+)
- Storage access for video files and playlists

Pre-built APKs are available from the [Releases page](https://github.com/slhck/SubjectivePlayer/releases/latest). These are unsigned builds.

If you want to make changes to the functionality, you need to build the app yourself. Check the [Developer Guide](#developer-guide) for more information.

## Installation

First, downlaod the latest APK from the [Releases page](https://github.com/slhck/SubjectivePlayer/releases/latest).
Then, install it on your Android device.

- Make sure you have [`adb` installed](https://developer.android.com/tools/adb) on your computer.
- [Enable developer mode](https://developer.android.com/studio/debug/dev-options) on the device.
- Connect the device to your computer via USB.
- When the device is recognized, allow debugging access.
- Then run:

    ```bash
    adb install <path-to-apk>
    ```

Open the app once to create the necessary storage directories.

To run a test, you need to put a playlist file (with the videos to be rated) and the actual videos onto the device.
The details of this will be explained below, but for a quick start, you can use the provided sample files.

Push sample test files with:

```bash
# First, open the app on your device to create storage directories
# Then push the example files:
cd examples
./push_to_device.sh
```

Now, in the app, enter a valid subject ID (`1` through `5`), and run a test.
It will show the videos, ask for a rating, and then finish.

The example files demonstrate different configuration options:

| Subject ID | Format | Features                                                          |
| ---------- | ------ | ----------------------------------------------------------------- |
| 1          | JSON   | Basic ACR rating with a timed break                               |
| 2          | JSON   | Continuous (slider) rating method                                 |
| 3          | JSON   | ACR with custom start/finish messages                             |
| 4          | CFG    | Legacy format with training section, custom messages, break       |
| 5          | JSON   | Full-featured: training, pre/post questionnaires, custom messages |

To create your own test files, see the detailed usage instructions below.

## Usage Guide

This section explains how to set up and run a subjective video quality test using the app.

### Prepare Video Files

You obviously need video files to be rated by the participants.

The app uses Android's native `MediaPlayer` for playback, which supports a wide range of video codecs and containers. Here's what's available:

| Codec      | Container Formats | Notes                                                    |
| ---------- | ----------------- | -------------------------------------------------------- |
| H.264/AVC  | MP4, MKV, 3GP     | Best compatibility across all devices                    |
| H.265/HEVC | MP4, MKV          | Better compression; Android 5.0+ (all supported devices) |
| AV1        | MP4, WebM, MKV    | Best compression; Android 10+ (hardware support varies)  |
| VP9        | WebM, MKV, MP4    | Royalty-free alternative; Android 4.4+                   |
| VP8        | WebM, MKV         | Older royalty-free codec; Android 4.0+; not recommended  |

For more information, see the Android documentation on [Supported Media Formats](https://developer.android.com/media/platform/supported-formats).

When preparing video files for subjective testing, consider the following recommendations to ensure compatibility and quality.

- Video codec: H.264 (AVC), High Profile
- Audio codec: AAC, 128-320 kbps, stereo
- Resolution: Match device display (e.g., 1920×1080 for Full HD), otherwise the device will scale the video during playback
- Frame rate: keep the original frame rate (e.g., 24, 25, 30, 60 fps)
- Bitrate: Use CRF (`-crf 18` or lower in FFmpeg) for visually lossless quality
- Container: MP4

Example FFmpeg command:

```bash
ffmpeg -i input.mp4 -c:v libx264 -crf 18 -preset slow -c:a aac -b:a 320k output.mp4
```

Note that hardware decoding support varies by device; software decoding may be used as fallback. The maximum resolution/bitrate depends on device hardware capabilities.

### Prepare Config Files

Each subject who will take part in the test must have their own config file specifying the videos to be shown and rated ("playlist") and other settings.

The app supports two config formats:

- JSON format (recommended): `subject_<ID>.json` – Structured JSON with questionnaire support. See [JSON Format](#json-format) below.
- Legacy text format: `subject_<ID>.cfg` – Simple text with one entry per line. See [Legacy CFG Format](#legacy-cfg-format) below.

For example, for subject ID `1`, the config file should be named `subject_1.json` (preferred) or `subject_1.cfg`. If both exist, the JSON file takes precedence.

To generate multiple configs for multiple subjects, you can look at the `create_config_files.py` script.

#### JSON Format

The JSON format is the preferred format for new configurations. It supports all features of the legacy format plus questionnaires for collecting participant data.

A JSON schema is available at [`json-schema/subject-config.schema.json`](json-schema/subject-config.schema.json).

Example `subject_1.json`:

```json
{
  "method": "ACR",
  "custom_messages": {
    "start_message": "Welcome to the test!\nPlease watch each video and rate its quality.",
    "finish_message": "Thank you for participating!",
    "training_message": "This is a training session.",
    "pre_questionnaire_message": "Please answer a few questions before we begin.",
    "post_questionnaire_message": "Please provide some feedback on your experience."
  },
  "playlist": [
    "TRAINING_START",
    "training_video1.mp4",
    "training_video2.mp4",
    "TRAINING_END",
    "video1.mp4",
    "video2.mp4",
    "BREAK 30",
    "video3.mp4"
  ],
  "pre_questionnaire": [
    {"question": "How old are you?", "type": "number"},
    {"question": "What is your gender?", "type": "radio", "options": ["Male", "Female", "Other", "Prefer not to say"]}
  ],
  "post_questionnaire": [
    {"question": "Any comments?", "type": "text", "required": false}
  ]
}
```

The following main fields are supported:

| Field                | Required | Description                                                          |
| -------------------- | -------- | -------------------------------------------------------------------- |
| `method`             | No       | Rating method (see below). Defaults to `ACR`.                        |
| `custom_messages`    | No       | Custom messages shown during the test (see below).                   |
| `playlist`           | Yes      | Array of video filenames and special commands (see below).           |
| `pre_questionnaire`  | No       | Array of questions to show before the test (UI not yet implemented). |
| `post_questionnaire` | No       | Array of questions to show after the test (UI not yet implemented).  |

The `method` field specifies how subjects will rate videos:

- `ACR` (default) – 5-point quality scale (Excellent/Good/Fair/Poor/Bad) shown after each video, following ITU-T Rec. P.910
- `CONTINUOUS` – Slider-based rating (0-100) shown after each video
- `DSIS` – Impairment scale (not fully implemented)
- `TIME_CONTINUOUS` – Real-time rating during playback using volume buttons (experimental)

With `custom_messages`, you can customize the messages shown at different points in the test. All fields are optional and support `\n` for line breaks:

- `start_message` – Shown before the first video (or before training if training is defined). The subject must click Continue to proceed.
- `finish_message` – Shown after the last rating is submitted. The subject clicks OK to finish.
- `training_message` – Shown on the training introduction screen before the first training video (only if training section is defined).
- `pre_questionnaire_message` – Introductory message shown before the pre-questionnaire, explaining how to answer (only if pre-questionnaire is defined).
- `post_questionnaire_message` – Introductory message shown before the post-questionnaire, explaining how to answer (only if post-questionnaire is defined).

The `playlist` array contains video filenames and special commands:

- **Video filename** (e.g., `"video1.mp4"`) – A video file in the `SubjectiveMovies/` folder. The file must exist!

- **Training Section** – `TRAINING_START` and `TRAINING_END`:
  - Defines a training section to familiarize subjects with the rating procedure
  - `TRAINING_START` marks the beginning; `TRAINING_END` marks the end
  - Videos between these markers are training videos (rated but not included in final results)
  - A training introduction screen (with optional custom `training_message`) is shown before the first training video
  - A training complete screen is shown after the last training video, before the main test begins
  - If you use `TRAINING_START`, you must also include `TRAINING_END`, and vice versa

- **Breaks** – `BREAK` or `BREAK <seconds>`:
  - Inserts a break between videos to prevent viewer fatigue
  - `BREAK` (no number) – Shows a message telling the subject to wait for the test supervisor. The subject must click Continue.
  - `BREAK 30` (with number) – A timed break of the specified duration in seconds, with a countdown timer. The Continue button is enabled when the timer reaches zero.

Finally, you can define questionnaires (`pre_questionnaire` and `post_questionnaire`). These are arrays of question objects shown before and after the test, respectively.

Each question is an object with the following fields:

| Field      | Required                  | Description                                                     |
| ---------- | ------------------------- | --------------------------------------------------------------- |
| `question` | Yes                       | The question text.                                              |
| `type`     | Yes                       | Question type: `number`, `radio`, `multiple-choice`, or `text`. |
| `options`  | For radio/multiple-choice | Array of answer options (strings).                              |
| `required` | No                        | Whether an answer is required. Defaults to `true`.              |

#### Legacy CFG Format

The legacy text format uses video filename or special directive per line. This format is supported for backward compatibility; the JSON format is recommended for new configurations.

**Format:** Each line contains either a video filename (e.g., `video1.mp4`) or a directive:

- `METHOD <type>` – Sets the rating method. See [Rating Methods](#rating-methods-method-field) above for supported types (`ACR`, `CONTINUOUS`, `DSIS`, `TIME_CONTINUOUS`). Must be on the first line if used. Defaults to `ACR`.
- `START_MESSAGE <message>` – Custom start message. See [Custom Messages](#custom-messages-custom_messages-object) above for behavior. Use `\n` for line breaks.
- `FINISH_MESSAGE <message>` – Custom finish message. See [Custom Messages](#custom-messages-custom_messages-object) above.
- `TRAINING_MESSAGE <message>` – Custom training message. See [Custom Messages](#custom-messages-custom_messages-object) above.
- `TRAINING_START` and `TRAINING_END` – Define a training section. See [Playlist Special Commands](#playlist-special-commands) above for details. Both must be present if either is used.
- `BREAK` or `BREAK <seconds>` – Insert a break. See [Playlist Special Commands](#playlist-special-commands) above for timed vs. untimed breaks.

**Example `subject_1.cfg`:**

```
METHOD ACR
START_MESSAGE Welcome to the test!\nPlease watch the following videos and rate their quality.
TRAINING_MESSAGE This is the training section.\nPlease get familiar with the rating procedure.
TRAINING_START
training_video1.mp4
training_video2.mp4
TRAINING_END
video1.mp4
video2.mp4
video3.mp4
BREAK 30
video4.mp4
video5.mp4
video6.mp4
BREAK 30
video7.mp4
video8.mp4
FINISH_MESSAGE Thank you for participating!\nPlease inform the test supervisor.
```

This example shows all major features: a custom start message, training section with custom message, main test videos with timed breaks, and a custom finish message. For detailed explanations of each feature, see the [JSON Format](#json-format) section above.

### Move the Files to the Device

The application stores videos, config files, and logs in app-specific storage. Due to Android's scoped storage restrictions (Android 10+), you'll need to use ADB to transfer files. All files are stored in the app's external storage directory:

```
/storage/emulated/0/Android/data/org.univie.subjectiveplayer/files/
├── SubjectiveCfg/     # Config files
├── SubjectiveMovies/  # Video files
└── SubjectiveLogs/    # Test result logs
```

> [!IMPORTANT]
> The app must be run at least once before transferring files to create the necessary directories with the right permissions.

To move your files (videos and config files) to the device automatically, use the provided `push_to_device.sh` script:

```bash
# Push your own test files from a custom directory
examples/push_to_device.sh -d /path/to/your/test/files

# Or push the included example files
examples/push_to_device.sh
```

Run `examples/push_to_device.sh --help` for all options.

Alternatively, use ADB manually to push files to the device:

```bash
# Push video files
adb push your_video.mp4 /storage/emulated/0/Android/data/org.univie.subjectiveplayer/files/SubjectiveMovies/

# Push config files
adb push subject_1.cfg /storage/emulated/0/Android/data/org.univie.subjectiveplayer/files/SubjectiveCfg/
```

### Validate the Config Files

The app can validate the config files to ensure all referenced video files exist. This is optional, but recommended.

To validate the config files:

- Start the app.
- Open the menu (three dots in the top-right corner).
- Select *Validate Config Files*.
- The app will check all config files in the `SubjectiveCfg` folder and report any missing video files or invalid syntax.
- You can also see which config uses which method, if training is used, and how many videos are in each config.

### Run the Test

When you start the app, you can select the config by having the subject enter their respective ID.

The test is self-guided and will show videos, ask for ratings, and handle breaks as defined in the config  (see above).

After a test, the results are stored in the `SubjectiveLogs` folder. Each file corresponds to one subject's test results.

### Obtain the Results

To get the results back to your computer, use ADB to pull the log files:

```bash
adb pull /storage/emulated/0/Android/data/org.univie.subjectiveplayer/files/SubjectiveLogs/ ./SubjectiveLogs/
```

This will copy all log files to your local `SubjectiveLogs/` directory.

## Output Format

Results are written as CSV files.

### Rating Output

For each subject there will be one rating output file. It is named as follows:

```
<participant_id>_<datetime>_<method>.csv
```

For example: `1_20250115-143055_ACR.csv`

The CSV file contains one line per video rated, plus additional lines for breaks:

| Column            | Description                                                                                           |
| ----------------- | ----------------------------------------------------------------------------------------------------- |
| `video_position`  | Zero-based index of the video in the playlist. `-1` for BREAK entries.                                |
| `video_name`      | Filename of the video (e.g., `video1.mp4`). `BREAK` for break entries.                                |
| `rating`          | Integer rating value. For ACR: 0=Excellent, 1=Good, 2=Fair, 3=Poor, 4=Bad. For continuous: 0-100.     |
| `rated_at`        | ISO8601 timestamp when the rating was submitted (e.g., `2025-01-15T14:32:05+01:00`).                  |
| `rating_duration` | Time in seconds the subject took to submit the rating (e.g., `2.345`). Empty time-continuous ratings. |

The `rating`, `rated_at`, and `rating_duration` columns are empty for BREAK entries.

For time-continuous ratings, there will be one line per second of video playback, with the `rating` column containing the current rating value at that second, and the `rated_at` column indicating when that rating was recorded.

The `rating_duration` column can be used to detect issues with single votes caused by observers struggling to decide on a rating. For more information, see: [Robitza, W., and Hlavacs, H. (2014). Assessing the validity of subjective QoE data through rating times and self-reported confidence. In *2014 Sixth International Workshop on Quality of Multimedia Experience (QoMEX)* (pp. 297–302). Singapore: IEEE.](https://ieeexplore.ieee.org/document/6982335/). Note: the `rating_duration` column is empty for time-continuous ratings since ratings are logged automatically during playback rather than after subject interaction.

### Questionnaire Output

If pre- or post-questionnaires are defined in the JSON config, answers are logged to separate CSV files:

```
<participant_id>_<datetime>_questionnaire_<type>.csv
```

For example: `1_20250115-143050_questionnaire_pre.csv`

The CSV uses a [tidy data](https://vita.had.co.nz/papers/tidy-data.pdf) format with one row per answer:

| Column            | Description                                                                          |
| ----------------- | ------------------------------------------------------------------------------------ |
| `question_number` | 1-based index of the question.                                                       |
| `question_type`   | Type of question: `number`, `radio`, `multiple-choice`, or `text`.                   |
| `question`        | The question text.                                                                   |
| `answer`          | The answer value. For multiple-choice, each selected option produces a separate row. |
| `answered_at`     | ISO8601 timestamp when the question was answered.                                    |
| `answer_duration` | Time in seconds the subject took to answer.                                          |

For multiple-choice questions, each selected option is written as a separate row with the same `question_number`, `answered_at`, and `answer_duration`. This tidy format makes analysis easier.

## In-App Configuration Options

The app allows some level of customization. Access the settings via the main menu (three dots) and clicking *Preferences*. The following options are available:

### Display

| Setting                   | Description                                                                                                                                    | Default |
| ------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- | ------- |
| **Edge-to-edge playback** | When enabled, video fills the entire screen (may be cut off by camera notch). When disabled, video avoids display cutouts and rounded corners. | Enabled |

### Configuration Files

| Setting                 | Description                                                                                                                                        | Default  |
| ----------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------- | -------- |
| **Allow duplicate IDs** | When enabled, allows the same participant ID to be used multiple times. When disabled, warns if an ID has already been used in existing log files. | Disabled |

### Time-Continuous Rating

| Setting      | Description                                                                                                | Default  |
| ------------ | ---------------------------------------------------------------------------------------------------------- | -------- |
| **No ticks** | When enabled, the time-continuous rating slider shows only Min/Max labels without intermediate tick marks. | Disabled |

## Developer Guide

To build the app from source, clone the repository and open it in Android Studio. Make sure you have the required SDK components installed:

- Android SDK with:
  - Build Tools 34.0.0 or higher
  - Platform SDK 35 (Android 15) or higher
- Java Development Kit (JDK) 17 or higher
- Gradle 8.9 or higher (included via wrapper)

### Building

To build it from the command line, use:

```bash
./gradlew assembleDebug
```

Or, for a release build:

```bash
./gradlew assembleRelease
```

If you want to run the app directly on a connected device or emulator, use:

```bash
./gradlew installDebug
```

Or, for release:

```bash
./gradlew installRelease
```

### Testing

To run the unit tests, use:

```bash
./gradlew test
```

Note that full UI tests are not included at this time. The unit tests only cover some utility functions like loading and parsing config files.

## Contributing

Contributions are welcome! Please open issues for bug reports or feature requests.

Note that your contributions will be licensed under the GNU General Public License v3, and that by submitting a pull request, you agree to this.

## License

The GNU General Public License v3

SubjectivePlayer for Android
Copyright (c) 2012-2025 Werner Robitza

SubjectivePlayer for Android is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

SubjectivePlayer for Android is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with SubjectivePlayer for Android.  If not, see <http://www.gnu.org/licenses/>.

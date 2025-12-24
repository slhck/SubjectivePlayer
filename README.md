# Subjective Player

![](app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp)

A video player for Android, enabling subjective quality assessment.

Author: Werner Robitza

**Contents:**

- [Features](#features)
- [Recent Changes](#recent-changes)
- [Requirements](#requirements)
- [Usage Guide](#usage-guide)
  - [Installation of the .apk](#installation-of-the-apk)
  - [Quick Setup Example](#quick-setup-example)
  - [Prepare Video Files](#prepare-video-files)
  - [Prepare Playlists](#prepare-playlists)
  - [Move the Files to the Device](#move-the-files-to-the-device)
  - [Validate the Playlists](#validate-the-playlists)
  - [Run the Test](#run-the-test)
  - [Obtain the Results](#obtain-the-results)
- [Output Format](#output-format)
- [Configuration Options](#configuration-options)
  - [Display](#display)
  - [Configuration Files](#configuration-files)
  - [Continuous Rating](#continuous-rating)
- [Developer Guide](#developer-guide)
- [Contributing](#contributing)
- [License](#license)

## Features

- Playback:
  - Multiple video codecs: H.264, H.265/HEVC, VP9, VP8, AV1
  - Multiple container formats: MP4, WebM, MKV, 3GP
  - Edge-to-edge playback, utilizing the full screen, or avoiding display cutouts and rounded corners
  - Playlists to define which videos to show to which users – different playlists for different users may be used
- Rating:
  - After each video, users are asked for their opinion using different methodologies:
    - 5-point ACR (Absolute Category Rating) categorical scale (Excellent/Good/Fair/Poor/Bad) from ITU-T Rec. P.910
    - Continuous slider-based rating (0-100)
    - Continuous real-time rating using volume buttons
    - (DSIS impairment scale – ⚠️ not fully implemented)
    - (Continuous real-time rating using volume buttons – ⚠️ not tested)
- General:
  - User ratings are based on IDs, so we can identify different participants later
  - Logging of the user ratings to CSV files
  - On-device validation of playlists
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
  - Training sessions to familiarize users with the rating procedure
  - Breaks between videos to prevent viewer fatigue

Here the ACR rating screen is shown after a video playback:

<img src="docs/subjective_player_acr.png" alt="Subjective Player ACR Rating Screen" width="500"/>

## Recent Changes

- v2.0 – Complete rewrite using new Android dependencies and many more features.
- v1.1 – Commit [323123](https://github.com/slhck/SubjectivePlayer/commit/323123c43480ca5d846ac2fe56bd405bf03a3425) – updates for ITU-T Rec. P.1203 model development in 2017
- v1.0 – Commit [742593b69cc4096368d9cbcafce362a3d9d2236c](https://github.com/slhck/SubjectivePlayer/commit/742593b69cc4096368d9cbcafce362a3d9d2236c) – initial commit in 2012

For more details, see the [Changelog](CHANGELOG.md).

## Requirements

For running the app, you need:

- Android device running Android 7.0 (Nougat) or higher (API level 24+)
- Storage access for video files and playlists

Pre-built APKs are available from the [Releases page](https://github.com/slhck/SubjectivePlayer/releases/latest). These are unsigned builds.

If you want to make changes to the functionality, you need to build the app yourself. Check the [Developer Guide](#developer-guide) for more information.

## Usage Guide

### Installation of the .apk

First, downlaod the latest APK from the [Releases page](https://github.com/slhck/SubjectivePlayer/releases/latest).
Then, install it on your Android device.

- Make sure you have [`adb` installed](https://developer.android.com/tools/adb) on your computer.
- [Enable developer mode](https://developer.android.com/studio/debug/dev-options) on the device
- Connect the device to your computer via USB
- When the device is recognized, allow debugging access
- Then run:

    ```bash
    adb install <path-to-apk>
    ```

### Quick Setup Example

Then, to run a test, you need to put a playlist file (with the videos to be rated) and the actual videos onto the device.
The details of this will be explained below, but for a quick start, you can use the provided sample files.

Push sample test files with:

```bash
cd examples
./push_to_device.sh
```

Now, in the app, enter a valid user ID (`1` or `2`), and run a test.
It will show three videos, ask for a rating, and then finish.

To create your own test files, see the detailed usage instructions below.

### Prepare Video Files

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

Note that hardware decoding support varies by device; software decoding may be used as fallback. The maximum resolution/bitrate depends on device hardware capabilities

### Prepare Playlists

Each user who will take part in the test must have their own playlist file specifying the videos to be shown and rated.
Playlists must be named like `subject_<ID>.cfg` suffix. For example, for user ID `1`, the playlist file should be named `subject_1.cfg` and so on.

To generate multiple playlists for multiple users, you can look at the `create_playlists_mobile.py` script.

The file is a simple text file with one entry per line.

Each line can be either:

- A video file name – the name of a video file (e.g., `video1.mp4`). The file must exist in the `SubjectiveMovies/` folder!
- A special command (see below)

The special commands are:

- `METHOD <type>` (optional, first line only)
  - This sets the rating method for this playlist.
  - The possible `<type>` values are:
    - `ACR` – 5-point quality scale (Excellent/Good/Fair/Poor/Bad) – **default if not specified**
    - `CONTINUOUS` – slider-based rating (0-100) shown after each video
    - `DSIS` – impairment scale (⚠️ not fully implemented)
    - `TIME_CONTINUOUS` – real-time rating during playback using volume buttons (⚠️ not tested!)
- `BREAK`
  - Inserts a break between videos to prevent viewer fatigue. The user must click a button to continue.
  - `BREAK` – shows a message telling the user to wait for the test supervisor.
  - `BREAK <seconds>` – timed break of where `<seconds>` is the duration in seconds, with a countdown timer.
- `START_MESSAGE <message>`
  - Shows a custom start message (`<message>`) before the first video. You can use `\n` for line breaks.
- `FINISH_MESSAGE <message>`
  - Shows a custom finish message (`<message>`) after the last video. You can use `\n` for line breaks.
- `TRAINING_START` and `TRAINING_END`
  - This defines a training section to familiarize users with the rating procedure.
  - `TRAINING_START` – marks the beginning of the training section
  - `TRAINING_END` – marks the end of the training section
  - Videos between `TRAINING_START` and `TRAINING_END` are considered training videos
  - A training introduction screen is shown before the first training video
  - A training complete screen is shown after the last training video, before the main test begins
  - **Note:** If you use `TRAINING_START`, you must also include `TRAINING_END`, and vice versa
- `TRAINING_MESSAGE <message>`
  - Defines a custom message (`<message>`) before training begins. You can use `\n` for line breaks.

For example, a very arbitrary `subject_1.cfg` could look like this:

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

In this example:

- A custom start message is shown first
- Then a training introduction screen appears, with a custom training message
- Two training videos are played with ratings
- A training complete screen is shown
- The main test begins with `video1.mp4` through `video8.mp4`, with breaks in between
- A custom finish message is shown at the end

### Move the Files to the Device

The application stores videos, playlists, and logs in app-specific storage. Due to Android's scoped storage restrictions (Android 10+), you'll need to use ADB to transfer files. All files are stored in the app's external storage directory:

```
/storage/emulated/0/Android/data/org.univie.subjectiveplayer/files/
├── SubjectiveCfg/     # Playlist configuration files
├── SubjectiveMovies/  # Video files
└── SubjectiveLogs/    # Test result logs
```

Use ADB to push files to the device:

```bash
# Create directories (app must be run at least once first)
adb shell mkdir -p /storage/emulated/0/Android/data/org.univie.subjectiveplayer/files/SubjectiveMovies
adb shell mkdir -p /storage/emulated/0/Android/data/org.univie.subjectiveplayer/files/SubjectiveCfg

# Push video files
adb push your_video.mp4 /storage/emulated/0/Android/data/org.univie.subjectiveplayer/files/SubjectiveMovies/

# Push playlist files
adb push playlist1.cfg /storage/emulated/0/Android/data/org.univie.subjectiveplayer/files/SubjectiveCfg/
```

To automate this process, you can use the provided `push_to_device.sh` script in the `examples/` directory and adapt it to your needs.

### Validate the Playlists

The app can validate the playlists to ensure all referenced video files exist. This is optional, but recommended.

To validate the playlists:

- Start the app
- Open the menu (three dots in the top-right corner)
- Select *Validate Config Files*.
- The app will check all playlist files in the `SubjectiveCfg` folder and report any missing video files or invalid syntax.
- You can also see which playlist uses which method, if training is used, and how many videos are in each playlist.

### Run the Test

When you start the app, you can select the playlist by having the user enter their respective ID.

The test is self-guided and will show videos, ask for ratings, and handle breaks as defined in the playlist (see above).

After a test, the results are stored in the `SubjectiveLogs` folder. Each file corresponds to one user's test results.

### Obtain the Results

To get the results back to your computer, use ADB to pull the log files:

```bash
adb pull /storage/emulated/0/Android/data/org.univie.subjectiveplayer/files/SubjectiveLogs/ ./SubjectiveLogs/
```

This will copy all log files to your local `SubjectiveLogs/` directory.

## Output Format

Results are written as CSV files. Each file is named with the pattern:

```
<participant_id>_<datetime>_<method>.csv
```

For example: `1_20250115-143055_ACR.csv`

The CSV file contains one line per video rated, plus additional lines for breaks:

| Column           | Description                                                                                                                |
| ---------------- | -------------------------------------------------------------------------------------------------------------------------- |
| `video_position` | Zero-based index of the video in the playlist. `-1` for BREAK entries.                                                     |
| `video_name`     | Filename of the video (e.g., `video1.mp4`). `BREAK` for break entries.                                                     |
| `rating`         | Integer rating value. Empty for BREAK entries. For ACR: 0=Excellent, 1=Good, 2=Fair, 3=Poor, 4=Bad. For continuous: 0-100. |
| `rated_at`       | ISO8601 timestamp when the rating was submitted (e.g., `2025-01-15T14:32:05+01:00`). Empty for BREAK entries.              |

For time-continuous ratings, there will be one line per second of video playback, with the `rating` column containing the current rating value at that second, and the `rated_at` column indicating when that rating was recorded.

## Configuration Options

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

| Setting      | Description                                                                                                    | Default  |
| ------------ | -------------------------------------------------------------------------------------------------------------- | -------- |
| **No ticks** | When enabled, the time-continuous rating slider shows only Min/Max labels without intermediate tick marks. | Disabled |

## Developer Guide

To build the app from source, clone the repository and open it in Android Studio. Make sure you have the required SDK components installed:

- Android SDK with:
  - Build Tools 34.0.0 or higher
  - Platform SDK 35 (Android 15) or higher
- Java Development Kit (JDK) 17 or higher
- Gradle 8.9 or higher (included via wrapper)

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

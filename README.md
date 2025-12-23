# Subjective Player

A video player for Android, enabling subjective quality assessment.

Author: Werner Robitza

**Contents:**

- [Features](#features)
- [Requirements](#requirements)
- [Quick Start](#quick-start)
- [Usage](#usage)
  - [Prepare Video Files](#prepare-video-files)
  - [Prepare Playlists](#prepare-playlists)
  - [Move the Files to the Device](#move-the-files-to-the-device)
  - [Run the Test](#run-the-test)
  - [Obtain the Results](#obtain-the-results)
- [Output Format](#output-format)
- [Configuration Options](#configuration-options)
  - [Display](#display)
  - [Configuration Files](#configuration-files)
  - [Continuous Rating](#continuous-rating)
- [License](#license)

## Features

- Playback of MPEG-4 AVC (H.264) coded videos, either in .mp4 format
- After each video, users are asked for their opinion using different methodologies:
  - 5-point ACR (Absolute Category Rating) categorical scale (Excellent/Good/Fair/Poor/Bad)
  - Continuous slider-based rating (0-100)
  - (DSIS impairment scale – ⚠️ not fully implemented)
  - (Continuous real-time rating using volume buttons – ⚠️ not tested)
- User ratings are based on IDs, so we can identify different participants later
- Playlists to define which videos to show to which users – different playlists for different users may be used
- Logging of the user ratings to CSV files
- Breaks between videos to prevent viewer fatigue
- On-device validation of playlists

## Requirements

For running the app, you need:

- Android device running Android 7.0 (Nougat) or higher (API level 24+)
- Storage access for video files and playlists
- Enable "Install from unknown sources" or use ADB to install the APK

For building it, you need:

- Android SDK with:
 - Build Tools 34.0.0 or higher
 - Platform SDK 35 (Android 15) or higher
- Java Development Kit (JDK) 17 or higher
- Gradle 8.9 or higher (included via wrapper)

To build the project, run:

```bash
./gradlew assembleDebug
```

The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

For a release build:

```bash
./gradlew assembleRelease
```

## Quick Start

To run the app you need to build and install it. Connect an Android phone, make sure you have the build tools installed, and call:

```bash
./gradlew installDebug
```

Then, to run a test, you need to put a playlist file (with the videos to be rated) and the actual videos onto the device.
Push sample test files with:

```bash
cd examples
./push_to_device.sh
```

Now, in the app, enter a valid user ID (`1` or `2`), and run a test.
It will show three videos, ask for a rating, and then finish.

To create your own test files, see the detailed usage instructions below.

## Usage

To create your own test setup, follow these steps.

### Prepare Video Files

Prepare your videos as H.264/AAC in MP4 format for device compatibility. We recommend using the following encoding settings:

- Video codec: H.264 (AVC), High Profile, using a tool like FFmpeg or HandBrake
- Audio codec: AAC, 320 kbps, stereo
- Resolution: Match device display (e.g., 1920x1080 for Full HD)
- Frame rate: 30 fps or 60 fps
- Bitrate: Use CRF (`-crf 18` or lower in FFmpeg) for good quality that should be visually lossless but still reasonable in size
- Container: MP4

### Prepare Playlists

Each user who will take part in the test must have their own playlist file specifying the videos to be shown and rated.
Playlists must be named like `subject_<ID>.cfg` suffix. For example, for user ID `1`, the playlist file should be named `subject_1.cfg` and so on.

To generate multiple playlists for multiple users, you can look at the `create_playlists_mobile.py` script.

The file is a simple text file with one entry per line. Lines can contain:

- `METHOD <type>` (optional, first line only) – sets the rating method for this playlist
  - `ACR` – 5-point quality scale (Excellent/Good/Fair/Poor/Bad) – **default if not specified**
  - `CONTINUOUS` – slider-based rating (0-100) shown after each video
  - `DSIS` – impairment scale (⚠️ not fully implemented)
  - `CONTINUOUS_RATING` – real-time rating during playback using volume buttons (⚠️ not tested!)
- Video file names – the name of a video file (e.g., `video1.mp4`)
  - The file must exist in the `SubjectiveMovies/` folder!
- `BREAK` commands – insert breaks between videos to prevent viewer fatigue
  - `BREAK` – shows a message telling the user to wait for the test supervisor
  - `BREAK 60` – timed break of 60 seconds with countdown timer

For example, `subject_1.cfg` could look like this:

```
METHOD ACR
video1.mp4
video2.mp4
video3.mp4
BREAK 30
video4.mp4
video5.mp4
video6.mp4
BREAK
video7.mp4
video8.mp4
```

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

### Run the Test

Now, when you start the app again, you can select the playlist by having the user enter their respective ID.

After a test, the results are stored in the `SubjectiveLogs` folder. Each file corresponds to one user's test.

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

For example: `1_20250115-1430_ACR.csv`

The CSV file contains one line per video rated, plus additional lines for breaks:

| Column           | Description                                                                                                                |
| ---------------- | -------------------------------------------------------------------------------------------------------------------------- |
| `video_position` | Zero-based index of the video in the playlist. `-1` for BREAK entries.                                                     |
| `video_name`     | Filename of the video (e.g., `video1.mp4`). `BREAK` for break entries.                                                     |
| `rating`         | Integer rating value. Empty for BREAK entries. For ACR: 0=Excellent, 1=Good, 2=Fair, 3=Poor, 4=Bad. For continuous: 0-100. |
| `rated_at`       | ISO8601 timestamp when the rating was submitted (e.g., `2025-01-15T14:32:05+01:00`). Empty for BREAK entries.              |

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

### Continuous Rating

| Setting      | Description                                                                                           | Default  |
| ------------ | ----------------------------------------------------------------------------------------------------- | -------- |
| **No ticks** | When enabled, the continuous rating slider shows only Min/Max labels without intermediate tick marks. | Disabled |

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

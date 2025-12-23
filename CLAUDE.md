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

Enabled via playlist config file (e.g., `subject_1.cfg`):

```
METHOD ACR
...
```

## Storage Directories

All under app-specific storage (`ctx.getExternalFilesDir(null)`):
- `SubjectiveCfg/` - Playlist files (`playlist1.cfg`, `playlist2.cfg`, etc.)
- `SubjectiveMovies/` - Video files (H.264/AAC MP4)
- `SubjectiveLogs/` - Output CSV files with ratings

## Testing with Device

```bash
# Push example test files
cd examples
./push_to_device.sh
```

# Building

To build the app from source, clone the repository and open it in Android Studio.

## Prerequisites

Make sure you have the required SDK components installed:

- Android SDK with:
    - Build Tools 34.0.0 or higher
    - Platform SDK 35 (Android 15) or higher
- Java Development Kit (JDK) 17 or higher
- Gradle 8.9 or higher (included via wrapper)

## Build Commands

### Debug Build

```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

### Install Directly to Device

For debug:

```bash
./gradlew installDebug
```

For release:

```bash
./gradlew installRelease
```

## Testing

To run the unit tests:

```bash
./gradlew test
```

!!! note
    Full UI tests are not included at this time. The unit tests only cover some utility functions like loading and parsing config files.

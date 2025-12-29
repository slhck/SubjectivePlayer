# Installation

## Download the APK

First, download the latest APK from the [Releases page](https://github.com/slhck/SubjectivePlayer/releases/latest).
Then, install it on your Android device.

## Install on Device

- Make sure you have [`adb` installed](https://developer.android.com/tools/adb) on your computer.
- [Enable developer mode](https://developer.android.com/studio/debug/dev-options) on the device.
- Connect the device to your computer via USB.
- When the device is recognized, allow debugging access.
- Then run:

    ```bash
    adb install <path-to-apk>
    ```

## Initial Setup

Open the app once to create the necessary storage directories.

To run a test, you need to put a playlist file (with the videos to be rated) and the actual videos onto the device. See [Config Files](../user-guide/config-files.md) for details.

!!! warning "Running the App Once is Important"

    The app creates necessary storage directories on first run.
    If you copy files to the device before running the app once, the app may not work correctly.

## Quick Test with Sample Files

For a quick start, you can use the provided sample files:

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

To create your own test files, see the [Config Files](../user-guide/config-files.md) guide.

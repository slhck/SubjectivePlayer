#!/bin/bash
#
# Push SubjectivePlayer test files to an Android device via adb

set -e

usage() {
    cat << EOF
Usage: $(basename "$0") [OPTIONS] [device_serial]

Push SubjectivePlayer test files (videos and playlists) to an Android device
or emulator via adb.

Arguments:
  device_serial    Optional device serial (e.g., emulator-5554, RF8M33XXXXX)
                   If not specified and only one device is connected, it will
                   be auto-selected. If multiple devices are connected, you
                   must specify which one to use.

Options:
  -h, --help       Show this help message and exit
  -l, --list       List connected devices and exit

Files pushed:
  *.mp4 files  ->  /sdcard/Android/data/org.univie.subjectiveplayer/files/SubjectiveMovies/
  *.cfg files  ->  /sdcard/Android/data/org.univie.subjectiveplayer/files/SubjectiveCfg/

Examples:
  $(basename "$0")                    # Auto-select single connected device
  $(basename "$0") emulator-5554      # Use specific emulator
  $(basename "$0") RF8M33XXXXX        # Use specific physical device
  $(basename "$0") --list             # Show connected devices
EOF
}

# Parse options
case "${1:-}" in
    -h|--help)
        usage
        exit 0
        ;;
    -l|--list)
        echo "Connected devices:"
        adb devices -l
        exit 0
        ;;
esac

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Device storage paths (matches Configuration.java)
# App uses getExternalFilesDir() which maps to:
# /storage/emulated/0/Android/data/org.univie.subjectiveplayer/files/
APP_STORAGE="/sdcard/Android/data/org.univie.subjectiveplayer/files"
VIDEOS_PATH="$APP_STORAGE/SubjectiveMovies"
CONFIG_PATH="$APP_STORAGE/SubjectiveCfg"
LOGS_PATH="$APP_STORAGE/SubjectiveLogs"

# Handle optional device serial
ADB_CMD="adb"
if [ -n "$1" ]; then
    ADB_CMD="adb -s $1"
    echo "Using device: $1"
fi

echo "=== SubjectivePlayer Test Files Push Script ==="
echo ""

# Check if adb is available
if ! command -v adb &> /dev/null; then
    echo "Error: adb not found in PATH"
    exit 1
fi

# Check if device is connected (handles both physical devices and emulators)
DEVICES=$(adb devices | grep -E '(device|emulator)$' | grep -v "^List")
DEVICE_COUNT=$(echo "$DEVICES" | grep -c . 2>/dev/null || echo 0)

if [ "$DEVICE_COUNT" -eq 0 ]; then
    echo "Error: No device connected or device not authorized"
    echo "Please connect a device/emulator and authorize USB debugging"
    echo ""
    echo "Available devices:"
    adb devices
    exit 1
fi

# If multiple devices and no serial specified, show options
if [ "$DEVICE_COUNT" -gt 1 ] && [ -z "$1" ]; then
    echo "Error: Multiple devices detected. Please specify device serial:"
    echo ""
    adb devices
    echo ""
    echo "Usage: $0 <device_serial>"
    exit 1
fi

# Auto-select device if only one is available and no serial specified
if [ -z "$1" ] && [ "$DEVICE_COUNT" -eq 1 ]; then
    DEVICE_SERIAL=$(echo "$DEVICES" | awk '{print $1}')
    ADB_CMD="adb -s $DEVICE_SERIAL"
    echo "Using device: $DEVICE_SERIAL"
fi

echo "Creating directories on device..."
$ADB_CMD shell "mkdir -p $VIDEOS_PATH"
$ADB_CMD shell "mkdir -p $CONFIG_PATH"
$ADB_CMD shell "mkdir -p $LOGS_PATH"

echo ""
echo "Pushing video files to $VIDEOS_PATH..."
for video in "$SCRIPT_DIR"/*.mp4; do
    if [ -f "$video" ]; then
        filename=$(basename "$video")
        echo "  - $filename"
        $ADB_CMD push "$video" "$VIDEOS_PATH/$filename"
    fi
done

echo ""
echo "Pushing playlist files to $CONFIG_PATH..."
for playlist in "$SCRIPT_DIR"/*.cfg; do
    if [ -f "$playlist" ]; then
        filename=$(basename "$playlist")
        echo "  - $filename"
        $ADB_CMD push "$playlist" "$CONFIG_PATH/$filename"
    fi
done

echo ""
echo "=== Done! ==="
echo ""
echo "Files on device:"
echo "  Videos:    $VIDEOS_PATH"
echo "  Playlists: $CONFIG_PATH"
echo "  Logs:      $LOGS_PATH"
echo ""
echo "To verify, run:"
echo "  adb shell ls -la $VIDEOS_PATH"
echo "  adb shell ls -la $CONFIG_PATH"

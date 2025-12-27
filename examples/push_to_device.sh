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
  -d, --dir DIR    Source directory containing *.mp4 and *.cfg files to push
                   (default: script's directory)

Files pushed:
  *.mp4 files            ->  /sdcard/Android/data/org.univie.subjectiveplayer/files/SubjectiveMovies/
  *.json or *.cfg files  ->  /sdcard/Android/data/org.univie.subjectiveplayer/files/SubjectiveCfg/

Note: On Android 11+ (API 30+), you must open the SubjectivePlayer app at least once
before running this script. The app needs to create the storage directories with
proper ownership, otherwise the app won't be able to read the pushed files.

Examples:
  $(basename "$0")                        # Push examples, auto-select device
  $(basename "$0") -d ~/my_test_files     # Push files from custom directory
  $(basename "$0") -d ./mytest device123  # Custom dir + specific device
  $(basename "$0") emulator-5554          # Use specific emulator
  $(basename "$0") --list                 # Show connected devices
EOF
}

# Default to script's directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INPUT_DIR="$SCRIPT_DIR"
DEVICE_SERIAL=""

# Parse options
while [ $# -gt 0 ]; do
    case "$1" in
        -h|--help)
            usage
            exit 0
            ;;
        -l|--list)
            echo "Connected devices:"
            adb devices -l
            exit 0
            ;;
        -d|--dir)
            if [ -z "${2:-}" ]; then
                echo "Error: -d/--dir requires a directory argument"
                exit 1
            fi
            INPUT_DIR="$2"
            shift 2
            ;;
        -*)
            echo "Error: Unknown option: $1"
            usage
            exit 1
            ;;
        *)
            # Positional argument = device serial
            DEVICE_SERIAL="$1"
            shift
            ;;
    esac
done

# Validate input directory
if [ ! -d "$INPUT_DIR" ]; then
    echo "Error: Input directory does not exist: $INPUT_DIR"
    exit 1
fi

# Convert to absolute path
INPUT_DIR="$(cd "$INPUT_DIR" && pwd)"

# Device storage paths (matches Configuration.java)
# App uses getExternalFilesDir() which maps to:
# /storage/emulated/0/Android/data/org.univie.subjectiveplayer/files/
APP_STORAGE="/sdcard/Android/data/org.univie.subjectiveplayer/files"
VIDEOS_PATH="$APP_STORAGE/SubjectiveMovies"
CONFIG_PATH="$APP_STORAGE/SubjectiveCfg"
LOGS_PATH="$APP_STORAGE/SubjectiveLogs"

# Handle optional device serial
ADB_CMD="adb"
if [ -n "$DEVICE_SERIAL" ]; then
    ADB_CMD="adb -s $DEVICE_SERIAL"
    echo "Using device: $DEVICE_SERIAL"
fi

echo "=== SubjectivePlayer Test Files Push Script ==="
echo "Source directory: $INPUT_DIR"
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
if [ "$DEVICE_COUNT" -gt 1 ] && [ -z "$DEVICE_SERIAL" ]; then
    echo "Error: Multiple devices detected. Please specify device serial:"
    echo ""
    adb devices
    echo ""
    echo "Usage: $0 [-d DIR] <device_serial>"
    exit 1
fi

# Auto-select device if only one is available and no serial specified
if [ -z "$DEVICE_SERIAL" ] && [ "$DEVICE_COUNT" -eq 1 ]; then
    DEVICE_SERIAL=$(echo "$DEVICES" | awk '{print $1}')
    ADB_CMD="adb -s $DEVICE_SERIAL"
    echo "Using device: $DEVICE_SERIAL"
fi

# Check Android version for scoped storage warning
API_LEVEL=$($ADB_CMD shell getprop ro.build.version.sdk | tr -d '\r')
echo "Device API level: $API_LEVEL"

# Check if directories exist with proper ownership (created by the app)
check_directory() {
    local dir_path="$1"
    local dir_name="$2"

    # Check if directory exists
    if ! $ADB_CMD shell "[ -d '$dir_path' ]" 2>/dev/null; then
        return 1
    fi

    # On Android 11+ (API 30+), check ownership
    if [ "$API_LEVEL" -ge 30 ]; then
        local owner
        owner=$($ADB_CMD shell "stat -c '%U' '$dir_path'" 2>/dev/null | tr -d '\r')
        if [ "$owner" = "shell" ]; then
            return 2  # Directory exists but wrong owner
        fi
    fi

    return 0
}

DIRS_OK=true
echo ""
echo "Checking storage directories..."

for dir_info in "SubjectiveCfg:$CONFIG_PATH" "SubjectiveMovies:$VIDEOS_PATH" "SubjectiveLogs:$LOGS_PATH"; do
    dir_name="${dir_info%%:*}"
    dir_path="${dir_info#*:}"

    check_directory "$dir_path" "$dir_name"
    result=$?

    if [ $result -eq 1 ]; then
        echo "  [MISSING] $dir_name"
        DIRS_OK=false
    elif [ $result -eq 2 ]; then
        echo "  [WRONG OWNER] $dir_name (owned by 'shell' instead of app)"
        DIRS_OK=false
    else
        echo "  [OK] $dir_name"
    fi
done

if [ "$DIRS_OK" = false ]; then
    echo ""
    echo "=== ERROR: Storage directories not ready ==="
    echo ""
    if [ "$API_LEVEL" -ge 30 ]; then
        echo "On Android 11+ (API $API_LEVEL), the app must create these directories."
        echo ""
        echo "To fix this:"
        echo "  1. Make sure SubjectivePlayer is installed on the device"
        echo "  2. Open the app at least once (this creates the directories)"
        echo "  3. Run this script again"
        echo ""
        echo "If directories show [WRONG OWNER], delete them and restart the app:"
        echo "  $ADB_CMD shell rm -rf $APP_STORAGE/Subjective*"
        echo "  # Then open the app and run this script again"
    else
        echo "Please install and open SubjectivePlayer at least once."
    fi
    exit 1
fi

# Count files to push
VIDEO_COUNT=$(find "$INPUT_DIR" -maxdepth 1 -name "*.mp4" -o -name "*.webm" -o -name "*.mkv" 2>/dev/null | wc -l | tr -d ' ')
CONFIG_COUNT=$(find "$INPUT_DIR" -maxdepth 1 -name "*.cfg" -o -name "*.json" 2>/dev/null | wc -l | tr -d ' ')

if [ "$VIDEO_COUNT" -eq 0 ] && [ "$CONFIG_COUNT" -eq 0 ]; then
    echo "Warning: No video files (*.mp4, *.webm, *.mkv) or config files (*.json or *.cfg) found in $INPUT_DIR"
    exit 1
fi

echo ""
echo "Pushing video files to $VIDEOS_PATH..."
VIDEO_PUSHED=0
for video in "$INPUT_DIR"/*.mp4 "$INPUT_DIR"/*.webm "$INPUT_DIR"/*.mkv; do
    if [ -f "$video" ]; then
        filename=$(basename "$video")
        echo "  - $filename"
        $ADB_CMD push "$video" "$VIDEOS_PATH/$filename"
        VIDEO_PUSHED=$((VIDEO_PUSHED + 1))
    fi
done
if [ "$VIDEO_PUSHED" -eq 0 ]; then
    echo "  (no video files found)"
fi

echo ""
echo "Pushing playlist files to $CONFIG_PATH..."
CONFIG_PUSHED=0
for playlist in "$INPUT_DIR"/*.cfg "$INPUT_DIR"/*.json; do
    if [ -f "$playlist" ]; then
        filename=$(basename "$playlist")
        echo "  - $filename"
        $ADB_CMD push "$playlist" "$CONFIG_PATH/$filename"
        CONFIG_PUSHED=$((CONFIG_PUSHED + 1))
    fi
done
if [ "$CONFIG_PUSHED" -eq 0 ]; then
    echo "  (no config files found)"
fi

echo ""
echo "=== Done! ==="
echo ""
echo "Files on device:"
echo "  Videos:    $VIDEOS_PATH"
echo "  Playlists: $CONFIG_PATH"
echo "  Logs:      $LOGS_PATH"
echo ""
echo "To verify, run:"
echo "  $ADB_CMD shell ls -la $VIDEOS_PATH"
echo "  $ADB_CMD shell ls -la $CONFIG_PATH"

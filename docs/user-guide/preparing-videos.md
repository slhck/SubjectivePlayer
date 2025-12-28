# Preparing Videos

You obviously need video files to be rated by the participants.

## Supported Formats

The app uses Android's native `MediaPlayer` for playback, which supports a wide range of video codecs and containers:

| Codec      | Container Formats | Notes                                                    |
| ---------- | ----------------- | -------------------------------------------------------- |
| H.264/AVC  | MP4, MKV, 3GP     | Best compatibility across all devices                    |
| H.265/HEVC | MP4, MKV          | Better compression; Android 5.0+ (all supported devices) |
| AV1        | MP4, WebM, MKV    | Best compression; Android 10+ (hardware support varies)  |
| VP9        | WebM, MKV, MP4    | Royalty-free alternative; Android 4.4+                   |
| VP8        | WebM, MKV         | Older royalty-free codec; Android 4.0+; not recommended  |

For more information, see the Android documentation on [Supported Media Formats](https://developer.android.com/media/platform/supported-formats).

## Recommendations

When preparing video files for subjective testing, consider the following recommendations to ensure compatibility and quality:

- **Video codec:** H.264 (AVC), High Profile
- **Audio codec:** AAC, 128-320 kbps, stereo
- **Resolution:** Match device display (e.g., 1920x1080 for Full HD), otherwise the device will scale the video during playback
- **Frame rate:** Keep the original frame rate (e.g., 24, 25, 30, 60 fps)
- **Bitrate:** Use CRF (`-crf 18` or lower in FFmpeg) for visually lossless quality
- **Container:** MP4

## Example FFmpeg Command

```bash
ffmpeg -i input.mp4 -c:v libx264 -crf 18 -preset slow -c:a aac -b:a 320k output.mp4
```

!!! note
    Hardware decoding support varies by device; software decoding may be used as fallback. The maximum resolution/bitrate depends on device hardware capabilities.

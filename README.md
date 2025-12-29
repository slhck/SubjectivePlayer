# SubjectivePlayer

![](app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp)

A video player for Android, enabling subjective quality assessment.

**Author:** Werner Robitza

## Features

- Multiple video codecs (H.264, H.265/HEVC, VP9, VP8, AV1) and containers (MP4, WebM, MKV, 3GP)
- Edge-to-edge playback or safe area mode avoiding display cutouts
- Rating methodologies: ACR (5-point scale), continuous slider, real-time rating
- Training sessions, breaks, and pre/post questionnaires
- CSV logging of ratings with timestamps and duration
- On-device config validation
- Internationalization: English, German, French, Italian, Spanish, Japanese, Korean, Chinese

Here the ACR rating screen is shown after a video playback:

<img src="docs/assets/images/subjective_player_acr.png" alt="Subjective Player ACR Rating Screen" width="500"/>

## Quick Start

1. Download the latest APK from the [Releases page](https://github.com/slhck/SubjectivePlayer/releases/latest)
2. Install on your Android device: `adb install <path-to-apk>`
3. Open the app once to create storage directories
4. Push sample test files:

    ```bash
    cd examples
    ./push_to_device.sh
    ```

5. Enter a subject ID (`1` through `5`) and run a test

Make sure to read the full documentation (see below) for detailed instructions on creating your own test configurations.

## What's New

See the [full changelog](CHANGELOG.md) for all updates.

## Documentation

Check out the [documentation](https://slhck.info/SubjectivePlayer/) for detailed usage instructions, config file formats, and developer guides.

## Contributing

Contributions are welcome! Please open issues for bug reports or feature requests.

## License

GNU General Public License v3. See [LICENSE](LICENSE) for details.

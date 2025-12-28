# SubjectivePlayer

![](assets/images/ic_launcher.webp)

A video player for Android, enabling subjective quality assessment.

**Author:** Werner Robitza

## Features

- Playback:
    - Multiple video codecs: H.264, H.265/HEVC, VP9, VP8, AV1
    - Multiple container formats: MP4, WebM, MKV, 3GP
    - Edge-to-edge playback, utilizing the full screen, or avoiding display cutouts and rounded corners
    - Config files to define which videos to show to which subjects -- different playlists for different subjects may be used
- Rating:
    - After each video, subjects are asked for their opinion using different methodologies:
      - 5-point ACR (Absolute Category Rating) categorical scale (Excellent/Good/Fair/Poor/Bad) from ITU-T Rec. P.910
      - Continuous slider-based rating (0-100)
      - Continuous real-time rating using volume buttons
      - (DSIS impairment scale -- not fully implemented)
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

![Subjective Player ACR Rating Screen](assets/images/subjective_player_acr.png){ width="500" }

## Recent Changes

- v2.x -- Complete rewrite using new Android dependencies and many more features.
- v1.1 -- Commit [323123](https://github.com/slhck/SubjectivePlayer/commit/323123c43480ca5d846ac2fe56bd405bf03a3425) -- updates for ITU-T Rec. P.1203 model development in 2017
- v1.0 -- Commit [742593b69cc4096368d9cbcafce362a3d9d2236c](https://github.com/slhck/SubjectivePlayer/commit/742593b69cc4096368d9cbcafce362a3d9d2236c) -- initial commit in 2012

For more details, see the [Changelog](about/changelog.md).

## Quick Start

For running the app, you need:

- Android device running Android 7.0 (Nougat) or higher (API level 24+)
- Storage access for video files and playlists

Pre-built APKs are available from the [Releases page](https://github.com/slhck/SubjectivePlayer/releases/latest). These are unsigned builds.

See the [Installation](getting-started/installation.md) guide for detailed instructions.

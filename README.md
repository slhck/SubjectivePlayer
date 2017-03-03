![](http://dl.dropbox.com/u/84665/cacmtv/subjectiveplayer-logo.png)

Author: Werner Robitza

## Introduction

Thanks for using SubjectivePlayer for Android. I built this software because there was nothing like it available yet and we definitely needed a video player capable of recording user opinion.

There are quite a few tools around that exist for traditional PCs, mostly Windows-based, which show videos or still images to test persons and ask them to rate the respective video or image quality. Such tools adhere to the ITU recommendations on subjective evaluation of audio and video and have been used for a long time. The hardware that should be used is described precisely, and so are the testing procedures.

Because the context of usage in mobile multimedia is totally different from the good old desktop PC at home, these ITU test recommendations do not really fit perfectly. We tried to come up with a simple and easy solution to present videos on an Android device and record the subjective opinion of viewers. Of course, when possible, ITU recommendations were followed or reinterpreted for mobile usage.

----

## Features and Non-Features

To put it in a brief list, SubjectivePlayer for Android can:

 * play an indefinitely long list of MPEG-4 AVC (H.264) coded videos, either in .mp4 or .3gpp format
 * after each video ask the user for their opinion using a certain methodology
 * record a user ID so as to identify the test person later
 * log the user opinion to text files

----

## Requirements

In order to use SubjectivePlayer for Android, you need to:

 * get a mobile phone running Android (tested with Galaxy S5 – use the "legacy" branch for older phones)
 * equip the phone with a SD card
 * activate the Debugging mode on the phone so you can use unsigned apps
 * preferably: a computer with the appropriate Android SDK installed to make your own changes
 * videos :)

## Installation

Download the `SubjectivePlayer.apk` file in this repository. It's the newest build. You can point your mobile device to this file directly, and it will prompt you to install the software.

Make sure you allow the application to access your SD card.

## Usage

The application loads videos from the SD card of the device. If the device does not have a physical SD card, Android will simulate that there is one.

Here is what you have to do to perform a test:

- Prepare your movies as H.264/AAC video and audio, multiplexed in MP4. That ensures compatibility with the device.
- Prepare a playlist, which consists of a list of video names, and call it `playlist1.cfg`, for example.
- For each participant that you want to test, you need a new playlist file with their ID, so for example, you need to create the files `playlist1.cfg` and `playlist2.cfg` for two users.

You can generate playlists using the `create_playlists_mobile.py` script.

Now let's put everything on the device:

### If you have an SD card

- Start the app
- Go to options (hold the option button) and enable SD card
- Quit and start the app again
- Connect the phone and you will see three folders in `Android/data/org.univie.subjectiveplayer/files`: `SubjectiveMovies`, `SubjectiveCfg` and `SubjectiveLogs`.
- Place your video files in the `SubjectiveMovies` folder.
- Place your playlist in the `SubjectiveCfg` folder.

### If you have no SD card

- Start the app once
- Connect the phone and you will see three folders on the internal memory: `SubjectiveMovies`, `SubjectiveCfg` and `SubjectiveLogs`.
- Place your video files in the `SubjectiveMovies` folder.
- Place your playlist in the `SubjectiveCfg` folder.

If you cannot write to these folders, you need to [use adb](http://lifehacker.com/the-easiest-way-to-install-androids-adb-and-fastboot-to-1586992378) and run something similar to this:

    adb push local-file /mnt/sdcard/SubjectiveMovies/

## Run the Test

Now, when you start the app again, you can select the playlist by having the user enter their respective ID.

After a test, the results are stored in the `SubjectiveLogs` folder. Each file corresponds to one user's test. The files contain the running number of video, the video name, and the corresponding rating as an integer number.

## License

The GNU General Public License v3

SubjectivePlayer for Android
Copyright (c) 2012-2017 Werner Robitza

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

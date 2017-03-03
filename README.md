![](http://dl.dropbox.com/u/84665/cacmtv/subjectiveplayer-logo.png)

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
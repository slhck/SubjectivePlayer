/*	This file is part of SubjectivePlayer for Android.
 *
 *	SubjectivePlayer for Android is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	SubjectivePlayer for Android is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with SubjectivePlayer for Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.univie.subjectiveplayer;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.SurfaceHolder.Callback;
import android.widget.LinearLayout.LayoutParams;
import android.widget.SeekBar;

/**
 * Plays the videos and shows dialogs the order set by the session data. This
 * class heavliy incorporates methods from the Android Developer Guide:
 * http://developer
 * .android.com/resources/samples/ApiDemos/src/com/example/android
 * /apis/media/MediaPlayerDemo_Video.html - Licensed under the Apache License,
 * Version 2.0 - http://www.apache.org/licenses/LICENSE-2.0
 */
public class SubjectivePlayerSession extends Activity implements Callback,
		OnCompletionListener, OnPreparedListener, OnVideoSizeChangedListener,
		OnErrorListener {

	private static final String TAG = SubjectivePlayerSession.class
			.getSimpleName();

	private View mSeekBarDialogView = null;

	private static int sCurrentRating;
	private static final int RATING_MIN = 1;
	private static final int RATING_DEFAULT = 3;
	private static final int RATING_MAX = 5;
	/** The interval in milliseconds that the rating should be captured	 */
	private static final int RATING_INTERVAL = 1000;
	
	private LoggingThread mLoggingThread;
	private Thread mThread;

	private MediaPlayer mPlayer;
	private SurfaceView mPlayView;
	private SurfaceHolder mHolder;
	private int mVideoWidth;
	private int mVideoHeight;
	/** Determines whether a video is currently playing or not */
	private boolean mIsVideoPlaying = false;
	/**
	 * Determines whether the video size is known or not. If not, the player
	 * will not start
	 */
	private boolean mIsVideoSizeKnown = false;
	/**
	 * Determines whether a video is ready to be played back. If not, the player
	 * will not start
	 */
	private boolean mIsVideoReadyToBePlayed = false;

	private static final int DIALOG_ACR_CATEGORICAL = 0;
	private static final int DIALOG_DSIS_CATEGORICAL = 1;
	private static final int DIALOG_CONTINUOUS = 2;

	/**
	 * Called when the activity is being started or restarted
	 */
	@Override
	public void onResume() {
		super.onResume();

		// reload any updated preferences
		Configuration.setPreferences(PreferenceManager
				.getDefaultSharedPreferences(getBaseContext()));
	}

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.video_view);

		try {
			mPlayView = (SurfaceView) findViewById(R.id.video_surface);
			mHolder = mPlayView.getHolder();
			mHolder.addCallback(this);
			mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		} catch (Exception e) {
			Log.e(TAG, "Error while creating Surface:" + e.toString());
		}
	}

	@Override
	/**
	 * Called when the activity pauses
	 */
	protected void onPause() {
		super.onPause();
		releasePlayer();
		if (mLoggingThread != null) {
			mLoggingThread.stop();
		}
		cleanUp();
	}

	@Override
	/**
	 * Called when the activity is destroyed.
	 */
	protected void onDestroy() {
		super.onDestroy();
		releasePlayer();
		cleanUp();
		Session.reset();
	}

	/**
	 * Called when the surface to display the video is created. This is only
	 * called once, so the player is prepared for playing the first file of the
	 * session.
	 */
	public void surfaceCreated(SurfaceHolder holder) {
		preparePlayerForVideo(Session.sCurrentTrack);
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {

	}

	public void surfaceDestroyed(SurfaceHolder holder) {

	}

	/**
	 * Prepares a video player for the given video index of the session. The
	 * player will start automatically when it has completed preparing, so it is
	 * only necessary to call this method which will then trigger all other
	 * calls and listeners.
	 * 
	 * @param videoIndex
	 *            The index of the video in the session.
	 * @throws IllegalArgumentException
	 * @throws IllegalStateException
	 *             When a command was executed on the player that doesn't work
	 *             in the current state.
	 * @throws IOException
	 *             When the input file was not found.
	 * @throws ArrayIndexOutOfBoundsException
	 *             When the video index exceeded the array size.
	 */
	public void preparePlayerForVideo(int videoIndex) {
		try {
			
			if (Session.sCurrentMethod == Methods.TYPE_CONTINUOUS_RATING) {
				String videoName = Session.sTracks.get(videoIndex);
				Log.d(TAG, "Creating new logging thread for video " + videoName);
				mLoggingThread = new LoggingThread(videoName);
				sCurrentRating = RATING_DEFAULT;
			}
			
			mPlayer = new MediaPlayer();

			String videoPath = getPathFromPlaylist(videoIndex);
			File videoFile = new File(videoPath);
			if ((!videoFile.exists()) || (!videoFile.canRead())) {
				throw new IOException("Video file " + videoPath + "not found!");
			}
			mPlayer.setDataSource(videoPath);
			mPlayer.setDisplay(mHolder);
			mPlayer.setScreenOnWhilePlaying(true);
			mPlayer.setOnPreparedListener(this);
			mPlayer.setOnCompletionListener(this);
			mPlayer.setOnVideoSizeChangedListener(this);
			mPlayer.setOnErrorListener(this);
			mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mPlayer.prepare();
		} catch (IllegalArgumentException e) {
			Log.e(TAG, e.toString());
			e.printStackTrace();
		} catch (IllegalStateException e) {
			Log.e(TAG, e.toString());
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(TAG, e.toString());
			e.printStackTrace();
		} catch (ArrayIndexOutOfBoundsException e) {
			Log.e(TAG, e.toString());
			e.printStackTrace();
		}
	}

	/**
	 * Called when the Media Player is finished preparing and ready to play.
	 */
	public void onPrepared(MediaPlayer player) {
		mIsVideoReadyToBePlayed = true;
		if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown) {
			startVideo();
		}
	}

	/**
	 * Called when the video size changes.
	 */
	public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
		Log.v(TAG, "onVideoSizeChanged called");
		if (width == 0 || height == 0) {
			Log.e(TAG, "invalid video width(" + width + ") or height(" + height
					+ ")");
			return;
		}
		mIsVideoSizeKnown = true;

		mVideoWidth = width;
		mVideoHeight = height;

		if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown) {
			startVideo();
		}
	}

	/**
	 * Called when the Media Player finished playing its file.
	 */
	public void onCompletion(MediaPlayer player) {
		// release the player and reset
		releasePlayer();
		cleanUp();

		switch (Session.sCurrentMethod) {
		case Methods.TYPE_ACR_CATEGORIGAL:
			showDialog(DIALOG_ACR_CATEGORICAL);
			break;
		case Methods.TYPE_DSIS_CATEGORICAL:
			break;
		case Methods.TYPE_CONTINUOUS:
			showDialog(DIALOG_CONTINUOUS);
			break;
		case Methods.TYPE_CONTINUOUS_RATING:
			nextVideo();
			break;
		default:
			finish();
		}
	}

	public boolean onError(MediaPlayer mp, int what, int extra) {
		switch (what) {
		case (MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING):
			Log.e(TAG, "The media player can't decode fast enough.");
			finishSession();
			break;
		}
		return false;
	}

	/**
	 * Starts the player.
	 */
	public void startVideo() {
		LayoutParams mParams = new LayoutParams(mVideoWidth, mVideoHeight);
		mPlayView.setLayoutParams(mParams);
		mIsVideoPlaying = true;
		
		if (Session.sCurrentMethod == Methods.TYPE_CONTINUOUS_RATING) {
			Log.d(TAG, "Running thread");
			mThread = new Thread(mLoggingThread);
			mThread.start();
		}
		
		mPlayer.start();
	}

	/**
	 * Resets all variables of the player.
	 */
	private void cleanUp() {
		mVideoWidth = 0;
		mVideoHeight = 0;
		mIsVideoReadyToBePlayed = false;
		mIsVideoSizeKnown = false;
		mIsVideoPlaying = false;
		if (mLoggingThread != null) {
			mLoggingThread.stop();
		}
	}

	/**
	 * Releases the player.
	 */
	private void releasePlayer() {
		if (mPlayer != null) {
			mPlayer.release();
			mPlayer = null;
		}
	}

	/**
	 * Returns the correct path for a video file at the given index. The path is
	 * composed from the Configuration, i.e. the directory where the videos
	 * reside and the file name of the current video. This method does not check
	 * whether a file exists or not.
	 * 
	 * @param index
	 *            The index of the video in the session
	 * @return The composed path of the video file
	 * @throws ArrayIndexOutOfBoundsException
	 *             When an index is requested that is not available
	 * @throws IOException
	 *             When a file is not found
	 */
	private String getPathFromPlaylist(int index)
			throws ArrayIndexOutOfBoundsException {
		File file = new File(Configuration.sFolderVideos,
				Session.sTracks.get(index));
		String path = file.getPath();
		Log.d(TAG, "Set data source to: " + path);
		return path;
	}

	/**
	 * Finishes the current rating session by cleaning up the player and writing
	 * the rating logs.
	 */
	private void finishSession() {
		cleanUp();
		releasePlayer();
		if (Session.sCurrentMethod != Methods.TYPE_CONTINUOUS_RATING) {
			Logger.writeSessionLogCSV();
		}
		Session.reset();
		finish();
	}

	/**
	 * Starts the next video after a dialog has been closed by the user
	 */
	private void nextVideo() {
		// show the next video if possible
		Session.sCurrentTrack++;
		if (Session.sCurrentTrack < Session.sTracks.size()) {
			if (Session.sCurrentMethod == Methods.TYPE_CONTINUOUS) {
				SubjectivePlayerSession.this.removeDialog(DIALOG_CONTINUOUS);				
			}
			if (Session.sCurrentMethod == Methods.TYPE_ACR_CATEGORIGAL) {
				SubjectivePlayerSession.this.removeDialog(DIALOG_ACR_CATEGORICAL);				
			}
			preparePlayerForVideo(Session.sCurrentTrack);
		} else {
			finishSession();
		}
	}

	/**
	 * KeyDown handler for the Volume Up / Volume Down buttons
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
				|| keyCode == KeyEvent.KEYCODE_VOLUME_UP) {

			if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
					&& (sCurrentRating > RATING_MIN)) {
				sCurrentRating--;
			}

			if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP)
					&& (sCurrentRating < RATING_MAX)) {
				sCurrentRating++;
			}

			Log.d(TAG, "Current rating: " + sCurrentRating);

			return true;
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}

	public class LoggingThread implements Runnable {
		String videoName;
		
		public LoggingThread(String videoName) {
			Log.d(TAG, "Created logging thread for " + videoName);
			this.videoName = videoName;
		}

		/**
		 * Runs the logging thread in the background
		 */
		public void run() {
			Log.d(TAG, "Running the thread for " + videoName);
			try {
				Logger.startContinuousLogCSV(videoName);
				while (mIsVideoPlaying) {
					Logger.writeContinuousData(videoName, "" + mPlayer.getCurrentPosition(), "" + sCurrentRating);
					Thread.sleep(RATING_INTERVAL);
				}
				Log.d(TAG, "Video " + videoName + " not playing anymore, stopping.");
				Logger.closeContinuousLogCSV();
			} catch (Exception e) {
				Logger.closeContinuousLogCSV();
			}
		}
		
		public void stop() {
			Logger.closeContinuousLogCSV();
			Log.d(TAG, "Stopped logging thread for " + videoName);
		}
	}

	/**
	 * Handles the dialogs shown in the application
	 */
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		
		switch (id) {
		// display a dialog asking the user to rate ACR quality
		case DIALOG_ACR_CATEGORICAL:
			AlertDialog.Builder builderACR = new AlertDialog.Builder(this);
			builderACR.setTitle(R.string.rate_ACR_caption);
			builderACR.setItems(Methods.LABELS_ACR,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							int rating = item;
							Session.sRatings.add(rating);
							dialog.dismiss();
							nextVideo();
						}
					});
			dialog = (AlertDialog) builderACR.create();
			break;

		case DIALOG_CONTINUOUS:
			AlertDialog.Builder builderContinuous = new AlertDialog.Builder(
					this);
			LayoutInflater inflater = (LayoutInflater) this
					.getSystemService(LAYOUT_INFLATER_SERVICE);

			// if no ticks should be shown in the seek bar
			if (Configuration.sNoTicks) {
				mSeekBarDialogView = inflater.inflate(
						R.layout.dialog_continuous_view_no_ticks, null);
			} else {
				mSeekBarDialogView = inflater.inflate(
						R.layout.dialog_continuous_view, null);
			}

			builderContinuous.setView(mSeekBarDialogView);
			builderContinuous.setTitle(R.string.rate_continuous_caption);
			builderContinuous.setCancelable(false);
			builderContinuous.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							SeekBar seekBar = (SeekBar) mSeekBarDialogView
									.findViewById(R.id.dialog_continuous_seekbar);
							int rating = seekBar.getProgress();
							Session.sRatings.add(rating);
							dialog.dismiss();
							nextVideo();
						}
					});
			dialog = (AlertDialog) builderContinuous.create();
			break;
		default:
			dialog = null;
		}
		return dialog;
	}

}

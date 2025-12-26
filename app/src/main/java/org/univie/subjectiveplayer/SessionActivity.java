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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.DataSetObserver;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.graphics.Rect;
import android.os.Build;
import android.view.WindowMetrics;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.SurfaceHolder.Callback;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
//import android.widget.LinearLayout.LayoutParams;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.view.ContextThemeWrapper;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Plays the videos and shows dialogs the order set by the session data. This
 * class heavliy incorporates methods from the Android Developer Guide:
 * http://developer
 * .android.com/resources/samples/ApiDemos/src/com/example/android
 * /apis/media/MediaPlayerDemo_Video.html - Licensed under the Apache License,
 * Version 2.0 - http://www.apache.org/licenses/LICENSE-2.0
 */
public class SessionActivity extends AppCompatActivity implements Callback,
		OnCompletionListener, OnPreparedListener, OnVideoSizeChangedListener,
		OnErrorListener {

	private static final String TAG = SessionActivity.class
			.getSimpleName();

	private View mSeekBarDialogView = null;

    private Button mOkButton = null;

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

	/** Tracks whether the surface is currently valid */
	private boolean mIsSurfaceValid = false;

	/** Stores pending video index when surface was not ready */
	private int mPendingVideoIndex = -1;

    private Dialog mDialog;
    /** Separate dialog instance for break dialogs to avoid interfering with rating dialogs */
    private Dialog mBreakDialog;
    /** Dialog instance for start screen */
    private Dialog mStartDialog;
    /** Dialog instance for finish screen */
    private Dialog mFinishDialog;
    /** Dialog instance for training intro screen */
    private Dialog mTrainingIntroDialog;
    /** Dialog instance for training complete screen */
    private Dialog mTrainingCompleteDialog;
    /** Tracks whether the start screen has been shown */
    private boolean mStartScreenShown = false;
    /** Tracks whether the training intro screen has been shown */
    private boolean mTrainingIntroShown = false;
	private static final int DIALOG_ACR_CATEGORICAL = 0;
	private static final int DIALOG_DSIS_CATEGORICAL = 1;
	private static final int DIALOG_CONTINUOUS = 2;
    // P.NATS: new dialog
    private static final int DIALOG_ACR_CUSTOM = 3;
    private static final int DIALOG_BREAK = 4;

    /** Placeholder rating value for BREAK entries */
    private static final int BREAK_RATING_PLACEHOLDER = -1;

    /** CountDownTimer for timed breaks */
    private CountDownTimer mBreakTimer = null;

    private long mBackPressedTime = 0;

    private int mCurrentRating = 0;

    /** Timestamp when the rating dialog was shown (for tracking rating duration) */
    private long mRatingDialogShownTime = 0;

    /** Current display insets (for cutout/safe area) */
    private int mInsetLeft = 0;
    private int mInsetRight = 0;

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

		// Enable edge-to-edge display
		WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

		setContentView(R.layout.video_view);

		// Handle window insets based on preference
		LinearLayout videoContainer = findViewById(R.id.video_container);
		ViewCompat.setOnApplyWindowInsetsListener(videoContainer, (v, windowInsets) -> {
			// Log all inset values for debugging
			Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
			Insets displayCutout = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout());
			Insets combined = windowInsets.getInsets(
				WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
			);

			Log.d(TAG, "System bars insets: left=" + systemBars.left + ", top=" + systemBars.top +
				", right=" + systemBars.right + ", bottom=" + systemBars.bottom);
			Log.d(TAG, "Display cutout insets: left=" + displayCutout.left + ", top=" + displayCutout.top +
				", right=" + displayCutout.right + ", bottom=" + displayCutout.bottom);
			Log.d(TAG, "Combined insets: left=" + combined.left + ", top=" + combined.top +
				", right=" + combined.right + ", bottom=" + combined.bottom);

			// Also check the actual DisplayCutout object
			androidx.core.view.DisplayCutoutCompat cutout = windowInsets.getDisplayCutout();
			if (cutout != null) {
				Log.d(TAG, "DisplayCutout safe insets: left=" + cutout.getSafeInsetLeft() +
					", top=" + cutout.getSafeInsetTop() + ", right=" + cutout.getSafeInsetRight() +
					", bottom=" + cutout.getSafeInsetBottom());
			} else {
				Log.d(TAG, "DisplayCutout is null");
			}

			if (!Configuration.sEdgeToEdge) {
				// Use the larger of combined insets or safe insets from cutout
				int left = combined.left;
				int top = combined.top;
				int right = combined.right;
				int bottom = combined.bottom;

				if (cutout != null) {
					left = Math.max(left, cutout.getSafeInsetLeft());
					top = Math.max(top, cutout.getSafeInsetTop());
					right = Math.max(right, cutout.getSafeInsetRight());
					bottom = Math.max(bottom, cutout.getSafeInsetBottom());
				}

				// Store insets for video sizing
				mInsetLeft = left;
				mInsetRight = right;

				Log.d(TAG, "Applying padding: left=" + left + ", top=" + top +
					", right=" + right + ", bottom=" + bottom);
				v.setPadding(left, top, right, bottom);
			} else {
				// Edge-to-edge: no padding
				mInsetLeft = 0;
				mInsetRight = 0;
				v.setPadding(0, 0, 0, 0);
			}
			return WindowInsetsCompat.CONSUMED;
		});

		try {
			mPlayView = (SurfaceView) findViewById(R.id.video_surface);
			mHolder = mPlayView.getHolder();
			mHolder.addCallback(this);
			// Note: setType(SURFACE_TYPE_PUSH_BUFFERS) removed - deprecated since API 15
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
		Log.d(TAG, "onPause called");
		releasePlayer();
		if (mLoggingThread != null) {
			mLoggingThread.stop();
		}
		if (mBreakTimer != null) {
			mBreakTimer.cancel();
			mBreakTimer = null;
			Log.d(TAG, "Break timer cancelled in onPause");
		}
		if (mBreakDialog != null) {
			mBreakDialog.dismiss();
			mBreakDialog = null;
			Log.d(TAG, "Break dialog dismissed in onPause");
		}
		if (mStartDialog != null) {
			mStartDialog.dismiss();
			mStartDialog = null;
			Log.d(TAG, "Start dialog dismissed in onPause");
		}
		if (mFinishDialog != null) {
			mFinishDialog.dismiss();
			mFinishDialog = null;
			Log.d(TAG, "Finish dialog dismissed in onPause");
		}
		if (mTrainingIntroDialog != null) {
			mTrainingIntroDialog.dismiss();
			mTrainingIntroDialog = null;
			Log.d(TAG, "Training intro dialog dismissed in onPause");
		}
		if (mTrainingCompleteDialog != null) {
			mTrainingCompleteDialog.dismiss();
			mTrainingCompleteDialog = null;
			Log.d(TAG, "Training complete dialog dismissed in onPause");
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
		// Close the session log file (ensures data is saved even if test is cancelled)
		CsvLogger.closeSessionLog();
		Session.reset();
	}

	/**
	 * Called when the surface to display the video is created. This is only
	 * called once, so the player is prepared for playing the first file of the
	 * session.
	 */
	public void surfaceCreated(SurfaceHolder holder) {
		mIsSurfaceValid = true;
		// Check if there's a pending video to prepare
		if (mPendingVideoIndex >= 0) {
			int videoIndex = mPendingVideoIndex;
			mPendingVideoIndex = -1;
			preparePlayerForVideo(videoIndex);
		} else if (Session.sCurrentTrack == 0 && mPlayer == null && !mStartScreenShown) {
			// First video - show start screen before preparing
			showStartScreen();
		}
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {

	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		mIsSurfaceValid = false;
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
		Log.d(TAG, "preparePlayerForVideo called for index " + videoIndex);

		// Check if this is the first training video and show training intro if needed
		if (Session.isFirstTrainingTrack(videoIndex) && !mTrainingIntroShown) {
			Log.i(TAG, "First training video at index " + videoIndex + ", showing training intro");
			showTrainingIntroScreen();
			return;
		}

		// Check if this is a BREAK command
		if (videoIndex < Session.sTracks.size()) {
			String track = Session.sTracks.get(videoIndex);
			if (Session.isBreakCommand(track)) {
				Log.i(TAG, "BREAK command detected at index " + videoIndex + ": " + track);
				showBreakDialog(track);
				return;
			}
		}

		// If surface is not valid, store pending video index and wait
		if (!mIsSurfaceValid) {
			Log.d(TAG, "Surface not ready, queuing video " + videoIndex);
			mPendingVideoIndex = videoIndex;
			return;
		}

		try {

			if (Session.sCurrentMethod == Methods.TYPE_TIME_CONTINUOUS) {
				String videoName = Session.sTracks.get(videoIndex);
				Log.d(TAG, "Creating new logging thread for video " + videoName);
				mLoggingThread = new LoggingThread(videoName, videoIndex);
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

		// Hide the video surface while showing the rating dialog
		// Use INVISIBLE instead of GONE to keep the surface alive
		if (mPlayView != null) {
			mPlayView.setVisibility(View.INVISIBLE);
		}

		switch (Session.sCurrentMethod) {
		case Methods.TYPE_ACR_CATEGORICAL:
            // P.NATS
			//showDialog(DIALOG_ACR_CATEGORICAL);
			mRatingDialogShownTime = System.currentTimeMillis();
            showDialog(DIALOG_ACR_CUSTOM);
			break;
		case Methods.TYPE_DSIS_CATEGORICAL:
			break;
		case Methods.TYPE_CONTINUOUS:
			mRatingDialogShownTime = System.currentTimeMillis();
			showDialog(DIALOG_CONTINUOUS);
			break;
		case Methods.TYPE_TIME_CONTINUOUS:
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
		// Show the video surface again
		if (mPlayView != null) {
			mPlayView.setVisibility(View.VISIBLE);
		}

        LayoutParams mParams = mPlayView.getLayoutParams();

        // Get screen width using modern API
        int screenWidth;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics windowMetrics = getWindowManager().getCurrentWindowMetrics();
            Rect bounds = windowMetrics.getBounds();
            screenWidth = bounds.width();
        } else {
            screenWidth = getResources().getDisplayMetrics().widthPixels;
        }

        // Subtract insets for safe area (when not edge-to-edge)
        int availableWidth = screenWidth - mInsetLeft - mInsetRight;
        Log.d(TAG, "Video sizing: screenWidth=" + screenWidth + ", insetLeft=" + mInsetLeft +
            ", insetRight=" + mInsetRight + ", availableWidth=" + availableWidth);

        // Set the width of the SurfaceView to the available width
        mParams.width = availableWidth;
        // Set the height of the SurfaceView to match the aspect ratio of the video
        mParams.height = (int) (((float) mVideoHeight / (float) mVideoWidth) * (float) availableWidth);

        mPlayView.setLayoutParams(mParams);
        mIsVideoPlaying = true;
		
		if (Session.sCurrentMethod == Methods.TYPE_TIME_CONTINUOUS) {
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
	 * Finishes the current rating session by cleaning up the player and closing
	 * the log file. Shows finish screen before actually finishing.
	 */
	private void finishSession() {
		cleanUp();
		releasePlayer();
		if (Session.sCurrentMethod != Methods.TYPE_TIME_CONTINUOUS) {
			CsvLogger.closeSessionLog();
		}
		// Show finish screen before ending
		showFinishScreen();
	}

	/**
	 * Starts the next video after a dialog has been closed by the user
	 */
	private void nextVideo() {
		// Check if we just finished the last training video (before incrementing track)
		int justCompletedTrack = Session.sCurrentTrack;

		// show the next video if possible
		Session.sCurrentTrack++;
		if (Session.sCurrentTrack < Session.sTracks.size()) {
			if (Session.sCurrentMethod == Methods.TYPE_CONTINUOUS) {
				SessionActivity.this.removeDialog(DIALOG_CONTINUOUS);
			}
			if (Session.sCurrentMethod == Methods.TYPE_ACR_CATEGORICAL) {
				SessionActivity.this.removeDialog(DIALOG_ACR_CATEGORICAL);
			}

			// Check if we just completed the last training video and need to show training complete
			if (Session.isLastTrainingTrack(justCompletedTrack)) {
				Log.i(TAG, "Last training video completed at index " + justCompletedTrack + ", showing training complete");
				showTrainingCompleteScreen();
				return;
			}

			// Show the surface first to trigger recreation if needed
			if (mPlayView != null) {
				mPlayView.setVisibility(View.VISIBLE);
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

        if (Session.sCurrentMethod == Methods.TYPE_ACR_CATEGORICAL) {
            return super.onKeyDown(keyCode, event);
        }

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
		int videoPosition;

		public LoggingThread(String videoName, int videoPosition) {
			Log.d(TAG, "Created logging thread for " + videoName);
			this.videoName = videoName;
			this.videoPosition = videoPosition;
		}

		/**
		 * Runs the logging thread in the background.
		 * Logs continuous ratings to the session log file.
		 * Note: rating_duration is null for time-continuous ratings since
		 * ratings are logged automatically during playback, not after user interaction.
		 */
		public void run() {
			Log.d(TAG, "Running the thread for " + videoName);
			try {
				while (mIsVideoPlaying) {
					CsvLogger.logRating(videoPosition, videoName, sCurrentRating, System.currentTimeMillis(), null);
					Thread.sleep(RATING_INTERVAL);
				}
				Log.d(TAG, "Video " + videoName + " not playing anymore, stopping.");
			} catch (Exception e) {
				Log.e(TAG, "Error in logging thread: " + e.getMessage());
			}
		}

		public void stop() {
			Log.d(TAG, "Stopped logging thread for " + videoName);
		}
	}

    /**
     * Closes the currently active dialog
     */
    private void dismissCurrentDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
        }
    }

	/**
	 * Handles the dialogs shown in the application
	 */
	protected Dialog onCreateDialog(int id) {
		//Dialog dialog = null;
		
		switch (id) {
		// display a dialog asking the user to rate ACR quality
		case DIALOG_ACR_CATEGORICAL:
            // P.NATS: reduce font size
            ContextThemeWrapper cw = new ContextThemeWrapper(this, R.style.AlertDialogTheme);
            AlertDialog.Builder builderACR = new AlertDialog.Builder(cw);
			builderACR.setTitle(R.string.rate_ACR_caption);
			builderACR.setItems(R.array.acr_labels,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							int rating = item;
							long ratedAt = System.currentTimeMillis();
							Double ratingDuration = (ratedAt - mRatingDialogShownTime) / 1000.0;
							Session.sRatings.add(rating);
                            Session.sRatingTime.add(ratedAt);
							// Log rating immediately to file
							String videoName = Session.sTracks.get(Session.sCurrentTrack);
							CsvLogger.logRating(Session.sCurrentTrack, videoName, rating, ratedAt, ratingDuration);
							dialog.dismiss();
							nextVideo();
						}
					});
			mDialog = (AlertDialog) builderACR.create();

            // Hack to make the list items smaller
            // http://stackoverflow.com/questions/24367612/how-can-i-set-an-alertdialog-item-height
            mDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogInterface) {
                    ListView listView = ((AlertDialog) dialogInterface).getListView();
                    final ListAdapter originalAdapter = listView.getAdapter();

                    listView.setAdapter(new ListAdapter() {
                        @Override
                        public int getCount() {
                            return originalAdapter.getCount();
                        }

                        @Override
                        public Object getItem(int id) {
                            return originalAdapter.getItem(id);
                        }

                        @Override
                        public long getItemId(int id) {
                            return originalAdapter.getItemId(id);
                        }

                        @Override
                        public int getItemViewType(int id) {
                            return originalAdapter.getItemViewType(id);
                        }

                        @Override
                        public View getView(int position, View convertView, ViewGroup parent) {
                            View view = originalAdapter.getView(position, convertView, parent);
                            TextView textView = (TextView) view;
                            //textView.setTextSize(16); set text size programmatically if needed
                            textView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, 100 /* this is item height */));
                            return view;
                        }

                        @Override
                        public int getViewTypeCount() {
                            return originalAdapter.getViewTypeCount();
                        }

                        @Override
                        public boolean hasStableIds() {
                            return originalAdapter.hasStableIds();
                        }

                        @Override
                        public boolean isEmpty() {
                            return originalAdapter.isEmpty();
                        }

                        @Override
                        public void registerDataSetObserver(DataSetObserver observer) {
                            originalAdapter.registerDataSetObserver(observer);

                        }

                        @Override
                        public void unregisterDataSetObserver(DataSetObserver observer) {
                            originalAdapter.unregisterDataSetObserver(observer);

                        }

                        @Override
                        public boolean areAllItemsEnabled() {
                            return originalAdapter.areAllItemsEnabled();
                        }

                        @Override
                        public boolean isEnabled(int position) {
                            return originalAdapter.isEnabled(position);
                        }

                    });
                }
            });

			break;

        // P.NATS
        case DIALOG_ACR_CUSTOM:
            mDialog = new CustomDialog(this);
            mDialog.setContentView(R.layout.dialog_acr_custom);
            mDialog.setCancelable(false);

            // button listeners
            final List<RadioButton> radioButtonList = new ArrayList<RadioButton>();
            RadioButton acrButtonExcellent   = (RadioButton) mDialog.findViewById(R.id.radioButtonExcellent);
            RadioButton acrButtonGood        = (RadioButton) mDialog.findViewById(R.id.radioButtonGood);
            RadioButton acrButtonFair        = (RadioButton) mDialog.findViewById(R.id.radioButtonFair);
            RadioButton acrButtonPoor        = (RadioButton) mDialog.findViewById(R.id.radioButtonPoor);
            RadioButton acrButtonBad         = (RadioButton) mDialog.findViewById(R.id.radioButtonBad);

            // Add all buttons to "group"
            radioButtonList.add(acrButtonExcellent);
            radioButtonList.add(acrButtonGood);
            radioButtonList.add(acrButtonFair);
            radioButtonList.add(acrButtonPoor);
            radioButtonList.add(acrButtonBad);

            // ACR values
            final HashMap<Button, Integer> valueMap = new HashMap<Button, Integer>();
            valueMap.put(acrButtonExcellent, 5);
            valueMap.put(acrButtonGood, 4);
            valueMap.put(acrButtonFair, 3);
            valueMap.put(acrButtonPoor, 2);
            valueMap.put(acrButtonBad, 1);

            mOkButton = (Button) mDialog.findViewById(R.id.buttonSendRating);
            mOkButton.setEnabled(false);

            for (final Button b : valueMap.keySet()) {
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Button okButton = (Button) mDialog.findViewById(R.id.buttonSendRating);
                        mCurrentRating = valueMap.get(b);
                        Log.d(TAG, "Setting rating to " + mCurrentRating);
                        int id = view.getId();
                        for (RadioButton rb : radioButtonList) {
                            if (rb.getId() == id) {
                                rb.setChecked(true);
                            } else {
                                rb.setChecked(false);
                            }
                        }
                        okButton.setVisibility(View.VISIBLE);
                        okButton.setEnabled(true);
                    }
                });
            }

            mOkButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (!v.isEnabled()) {
                        return;
                    }
                    long ratedAt = System.currentTimeMillis();
                    Double ratingDuration = (ratedAt - mRatingDialogShownTime) / 1000.0;
                    Session.sRatings.add(mCurrentRating);
                    Log.d(TAG, "Rating saved: " + mCurrentRating + " (took " + ratingDuration + "s)");
                    Session.sRatingTime.add(ratedAt);
                    // Log rating immediately to file
                    String videoName = Session.sTracks.get(Session.sCurrentTrack);
                    CsvLogger.logRating(Session.sCurrentTrack, videoName, mCurrentRating, ratedAt, ratingDuration);
                    dismissCurrentDialog();
                    // reset buttons
                    for (RadioButton rb : radioButtonList) {
                        rb.setChecked(false);
                    }
                    v.setVisibility(View.INVISIBLE);
                    v.setEnabled(false);
                    nextVideo();
                }
            });

            mDialog.getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

            break;

		case DIALOG_CONTINUOUS:
			mDialog = new CustomDialog(this);
			mDialog.setContentView(R.layout.dialog_continuous_custom);
			mDialog.setCancelable(false);

			// Show tick labels or min/max labels based on preference
			View tickLabelsContainer = mDialog.findViewById(R.id.tick_labels_container);
			View minMaxLabelsContainer = mDialog.findViewById(R.id.min_max_labels_container);
			if (Configuration.sNoTicks) {
				tickLabelsContainer.setVisibility(View.GONE);
				minMaxLabelsContainer.setVisibility(View.VISIBLE);
			} else {
				tickLabelsContainer.setVisibility(View.VISIBLE);
				minMaxLabelsContainer.setVisibility(View.GONE);
			}

			final SeekBar seekBar = (SeekBar) mDialog.findViewById(R.id.dialog_continuous_seekbar);
			final TextView valueLabel = (TextView) mDialog.findViewById(R.id.continuous_value_label);
			final Button rateButton = (Button) mDialog.findViewById(R.id.buttonSendRating);

			// Update value label when slider changes
			seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					valueLabel.setText(String.valueOf(progress));
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {}

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {}
			});

			// Rate button click handler
			rateButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					int rating = seekBar.getProgress();
					long ratedAt = System.currentTimeMillis();
					Double ratingDuration = (ratedAt - mRatingDialogShownTime) / 1000.0;
					Session.sRatings.add(rating);
					Session.sRatingTime.add(ratedAt);
					Log.d(TAG, "Continuous rating saved: " + rating + " (took " + ratingDuration + "s)");
					// Log rating immediately to file
					String videoName = Session.sTracks.get(Session.sCurrentTrack);
					CsvLogger.logRating(Session.sCurrentTrack, videoName, rating, ratedAt, ratingDuration);
					dismissCurrentDialog();
					// Reset slider to center for next video
					seekBar.setProgress(50);
					valueLabel.setText("50");
					nextVideo();
				}
			});

			mDialog.getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			break;
		default:
			mDialog = null;
		}
		return mDialog;
	}

    // http://stackoverflow.com/questions/6413700/android-proper-way-to-use-onbackpressed
    @Override
    public void onBackPressed() {        // to prevent irritating accidental logouts
        long t = System.currentTimeMillis();
        if (t - mBackPressedTime > 2000) {    // 2 secs
            mBackPressedTime = t;
            Toast.makeText(this, "Press back again to cancel.",
                    Toast.LENGTH_SHORT).show();
        } else {    // this guy is serious
            // clean up
            super.onBackPressed();       // bye
        }
    }

    /**
     * Shows the break dialog for a BREAK command.
     * @param breakCommand The BREAK command string (e.g., "BREAK" or "BREAK 60")
     */
    private void showBreakDialog(String breakCommand) {
        Log.d(TAG, "showBreakDialog called with: " + breakCommand);

        // Hide the video surface while showing the break dialog
        if (mPlayView != null) {
            mPlayView.setVisibility(View.INVISIBLE);
        }

        // Parse the break duration
        final int breakDuration = Session.parseBreakDuration(breakCommand);
        final boolean hasDuration = breakDuration > 0;

        Log.d(TAG, "Break duration: " + breakDuration + " seconds, hasDuration: " + hasDuration);

        // Create and show the break dialog (use separate mBreakDialog to avoid interfering with rating dialogs)
        mBreakDialog = new CustomDialog(this);
        mBreakDialog.setContentView(R.layout.dialog_break);
        mBreakDialog.setCancelable(false);

        final TextView timerMessage = (TextView) mBreakDialog.findViewById(R.id.break_timer_message);
        final Button continueButton = (Button) mBreakDialog.findViewById(R.id.break_continue_button);

        if (hasDuration) {
            // Timed break - show countdown
            timerMessage.setText(getString(R.string.break_timer_countdown, breakDuration));
            continueButton.setEnabled(false);
            continueButton.setAlpha(0.5f);

            // Start countdown timer
            mBreakTimer = new CountDownTimer(breakDuration * 1000L, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    int secondsRemaining = (int) (millisUntilFinished / 1000);
                    Log.d(TAG, "Break countdown: " + secondsRemaining + " seconds remaining");
                    timerMessage.setText(getString(R.string.break_timer_countdown, secondsRemaining));
                }

                @Override
                public void onFinish() {
                    Log.i(TAG, "Break countdown finished");
                    timerMessage.setText(R.string.break_timer_ready);
                    continueButton.setEnabled(true);
                    continueButton.setAlpha(1.0f);
                    vibrateLong();
                }
            };
            mBreakTimer.start();
            Log.d(TAG, "Break countdown timer started");
        } else {
            // Untimed break - supervisor controlled
            timerMessage.setText(R.string.break_supervisor_message);
            continueButton.setEnabled(true);
            continueButton.setAlpha(1.0f);
            Log.d(TAG, "Untimed break - waiting for supervisor");
        }

        // Continue button click handler
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!v.isEnabled()) {
                    return;
                }
                Log.i(TAG, "Break continue button clicked");
                onBreakFinished();
            }
        });

        mBreakDialog.getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mBreakDialog.show();
        Log.d(TAG, "Break dialog shown");
    }

    /**
     * Called when a break is finished (user clicks Continue).
     * Adds placeholder values and proceeds to next video.
     */
    private void onBreakFinished() {
        Log.d(TAG, "onBreakFinished called");

        // Cancel any running timer
        if (mBreakTimer != null) {
            mBreakTimer.cancel();
            mBreakTimer = null;
            Log.d(TAG, "Break timer cancelled");
        }

        // Dismiss the break dialog
        if (mBreakDialog != null) {
            mBreakDialog.dismiss();
            mBreakDialog = null;
            Log.d(TAG, "Break dialog dismissed");
        }

        // Add placeholder values to keep data lists aligned with sTracks
        Session.sRatings.add(BREAK_RATING_PLACEHOLDER);
        Session.sRatingTime.add(System.currentTimeMillis());
        Log.d(TAG, "Added placeholder rating (" + BREAK_RATING_PLACEHOLDER + ") for BREAK at index " + Session.sCurrentTrack);

        // Log break entry to file
        CsvLogger.logBreak();

        // Proceed to next video
        nextVideo();
    }

    /**
     * Shows the start screen before the first video.
     * Uses custom message from config file if available, otherwise uses default.
     */
    private void showStartScreen() {
        Log.d(TAG, "showStartScreen called");
        mStartScreenShown = true;

        // Hide the video surface while showing the start screen
        if (mPlayView != null) {
            mPlayView.setVisibility(View.INVISIBLE);
        }

        // Create and show the start dialog
        mStartDialog = new CustomDialog(this);
        mStartDialog.setContentView(R.layout.dialog_start);
        mStartDialog.setCancelable(false);

        final TextView messageView = (TextView) mStartDialog.findViewById(R.id.start_message);
        final Button continueButton = (Button) mStartDialog.findViewById(R.id.start_continue_button);

        // Use custom message from config if available, otherwise use default
        if (Session.sStartMessage != null && !Session.sStartMessage.isEmpty()) {
            messageView.setText(Session.sStartMessage);
            Log.d(TAG, "Using custom start message from config");
        } else {
            messageView.setText(R.string.start_message_default);
            Log.d(TAG, "Using default start message");
        }

        // Continue button click handler
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Start screen continue button clicked");
                onStartScreenFinished();
            }
        });

        mStartDialog.getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mStartDialog.show();
        Log.d(TAG, "Start screen dialog shown");
    }

    /**
     * Called when the start screen is finished (user clicks Continue).
     * Proceeds to prepare and play the first video.
     */
    private void onStartScreenFinished() {
        Log.d(TAG, "onStartScreenFinished called");

        // Start the session log file (for non-continuous rating methods)
        if (Session.sCurrentMethod != Methods.TYPE_TIME_CONTINUOUS) {
            CsvLogger.startSessionLog();
        }

        // Dismiss the start dialog
        if (mStartDialog != null) {
            mStartDialog.dismiss();
            mStartDialog = null;
            Log.d(TAG, "Start dialog dismissed");
        }

        // Show the surface and prepare first video
        if (mPlayView != null) {
            mPlayView.setVisibility(View.VISIBLE);
        }
        preparePlayerForVideo(Session.sCurrentTrack);
    }

    /**
     * Shows the finish screen after all videos have been rated.
     * Uses custom message from config file if available, otherwise uses default.
     */
    private void showFinishScreen() {
        Log.d(TAG, "showFinishScreen called");

        // Hide the video surface while showing the finish screen
        if (mPlayView != null) {
            mPlayView.setVisibility(View.INVISIBLE);
        }

        // Create and show the finish dialog
        mFinishDialog = new CustomDialog(this);
        mFinishDialog.setContentView(R.layout.dialog_finish);
        mFinishDialog.setCancelable(false);

        final TextView messageView = (TextView) mFinishDialog.findViewById(R.id.finish_message);
        final Button okButton = (Button) mFinishDialog.findViewById(R.id.finish_ok_button);

        // Use custom message from config if available, otherwise use default
        if (Session.sFinishMessage != null && !Session.sFinishMessage.isEmpty()) {
            messageView.setText(Session.sFinishMessage);
            Log.d(TAG, "Using custom finish message from config");
        } else {
            messageView.setText(R.string.finish_message_default);
            Log.d(TAG, "Using default finish message");
        }

        // OK button click handler
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Finish screen OK button clicked");
                onFinishScreenClosed();
            }
        });

        mFinishDialog.getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mFinishDialog.show();
        Log.d(TAG, "Finish screen dialog shown");
    }

    /**
     * Called when the finish screen is closed (user clicks OK).
     * Resets session and finishes the activity.
     */
    private void onFinishScreenClosed() {
        Log.d(TAG, "onFinishScreenClosed called");

        // Dismiss the finish dialog
        if (mFinishDialog != null) {
            mFinishDialog.dismiss();
            mFinishDialog = null;
            Log.d(TAG, "Finish dialog dismissed");
        }

        // Reset session and finish activity
        Session.reset();
        finish();
    }

    /**
     * Shows the training intro screen before the first training video.
     * Uses custom message from config file if available, otherwise uses default.
     */
    private void showTrainingIntroScreen() {
        Log.d(TAG, "showTrainingIntroScreen called");
        mTrainingIntroShown = true;

        // Hide the video surface while showing the training intro screen
        if (mPlayView != null) {
            mPlayView.setVisibility(View.INVISIBLE);
        }

        // Create and show the training intro dialog
        mTrainingIntroDialog = new CustomDialog(this);
        mTrainingIntroDialog.setContentView(R.layout.dialog_training_intro);
        mTrainingIntroDialog.setCancelable(false);

        final TextView messageView = (TextView) mTrainingIntroDialog.findViewById(R.id.training_intro_message);
        final Button continueButton = (Button) mTrainingIntroDialog.findViewById(R.id.training_intro_continue_button);

        // Use custom message from config if available, otherwise use default
        if (Session.sTrainingMessage != null && !Session.sTrainingMessage.isEmpty()) {
            messageView.setText(Session.sTrainingMessage);
            Log.d(TAG, "Using custom training message from config");
        } else {
            messageView.setText(R.string.training_intro_message_default);
            Log.d(TAG, "Using default training intro message");
        }

        // Continue button click handler
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Training intro continue button clicked");
                onTrainingIntroFinished();
            }
        });

        mTrainingIntroDialog.getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mTrainingIntroDialog.show();
        Log.d(TAG, "Training intro dialog shown");
    }

    /**
     * Called when the training intro screen is finished (user clicks Continue).
     * Proceeds to prepare and play the first training video.
     */
    private void onTrainingIntroFinished() {
        Log.d(TAG, "onTrainingIntroFinished called");

        // Dismiss the training intro dialog
        if (mTrainingIntroDialog != null) {
            mTrainingIntroDialog.dismiss();
            mTrainingIntroDialog = null;
            Log.d(TAG, "Training intro dialog dismissed");
        }

        // Show the surface and prepare first training video
        if (mPlayView != null) {
            mPlayView.setVisibility(View.VISIBLE);
        }
        preparePlayerForVideo(Session.sCurrentTrack);
    }

    /**
     * Shows the training complete screen after the last training video.
     */
    private void showTrainingCompleteScreen() {
        Log.d(TAG, "showTrainingCompleteScreen called");

        // Hide the video surface while showing the training complete screen
        if (mPlayView != null) {
            mPlayView.setVisibility(View.INVISIBLE);
        }

        // Create and show the training complete dialog
        mTrainingCompleteDialog = new CustomDialog(this);
        mTrainingCompleteDialog.setContentView(R.layout.dialog_training_complete);
        mTrainingCompleteDialog.setCancelable(false);

        final TextView messageView = (TextView) mTrainingCompleteDialog.findViewById(R.id.training_complete_message);
        final Button continueButton = (Button) mTrainingCompleteDialog.findViewById(R.id.training_complete_continue_button);

        // Use default message (training complete message is not customizable)
        messageView.setText(R.string.training_complete_message_default);
        Log.d(TAG, "Using default training complete message");

        // Continue button click handler
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Training complete continue button clicked");
                onTrainingCompleteFinished();
            }
        });

        mTrainingCompleteDialog.getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mTrainingCompleteDialog.show();
        Log.d(TAG, "Training complete dialog shown");
    }

    /**
     * Called when the training complete screen is finished (user clicks Continue).
     * Proceeds to prepare and play the next video (first main test video).
     */
    private void onTrainingCompleteFinished() {
        Log.d(TAG, "onTrainingCompleteFinished called");

        // Dismiss the training complete dialog
        if (mTrainingCompleteDialog != null) {
            mTrainingCompleteDialog.dismiss();
            mTrainingCompleteDialog = null;
            Log.d(TAG, "Training complete dialog dismissed");
        }

        // Show the surface and prepare next video
        if (mPlayView != null) {
            mPlayView.setVisibility(View.VISIBLE);
        }
        preparePlayerForVideo(Session.sCurrentTrack);
    }

    /**
     * Vibrates the device with a long vibration to signal break end.
     */
    private void vibrateLong() {
        Log.d(TAG, "vibrateLong called");
        try {
            Vibrator vibrator;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vibratorManager = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                vibrator = vibratorManager.getDefaultVibrator();
            } else {
                vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(500);
                }
                Log.d(TAG, "Device vibrated for 500ms");
            } else {
                Log.w(TAG, "No vibrator available on this device");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during vibration: " + e.getMessage());
        }
    }

}

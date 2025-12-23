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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

public abstract class Configuration {

	private static final String TAG = Configuration.class.getSimpleName();

    private static Context sCtx = null;

	/** File handle for the root of the SD card */
	public static File sStorage = null;

	/** File handle for the root of the app data folder */
	public static File sFolderApproot = null;

	/** File handle for the folder in which videos are stored */
	public static File sFolderVideos = null;

	/** File handle for the folder in which log files are stored */
	public static File sFolderLogs = null;

	/** File handle for a configuration file */
	public static File sFileConfig = null;

	/** File name of a configuration file */
	public static String sFileConfigName = null;

	/**
	 * Path of the folder in which videos are stored, relative to the SD card
	 * root with a trailing slash. If it does not exist, it will be created
	 * automatically on the SD card.
	 */
	public static final String PATH_VIDEOS = new String("SubjectiveMovies/");

	/**
	 * Path of the folder in which log files are stored, relative to the SD card
	 * root with a trailing slash. If it does not exist, it will be created
	 * automatically on the SD card.
	 */
	public static final String PATH_LOGS = new String("SubjectiveLogs/");
	
	/**
	 * Path of the folder in which config files are stored, relative to the SD card
	 * root with a trailing slash. If it does not exist, it will be created
	 * automatically on the SD card.
	 */
	public static final String PATH_CONFIG = new String("SubjectiveCfg/");

	/**
	 * Controls whether the continuous dialog shows no ticks and just a maximum
	 * and minimum label
	 */
	public static boolean sNoTicks = false;


    /**
     * Allow using same ID twice or more
     */
    public static boolean sAllowDuplicateIds = false;

    /**
     * Controls whether video playback uses edge-to-edge display
     * (true = fullscreen, false = respect display cutouts)
     */
    public static boolean sEdgeToEdge = true;

    /** The container for all application preferences */
	private static SharedPreferences sPreferences = null;


	/**
	 * Internally updates the preference values, called by setPreferences on
	 * each activity resume. This method only overwrites the internal members of
	 * this class with the contents of the SharedPreferences.
	 */
	private static void updatePreferences() {
		Log.d(TAG, "updatePreferences called");
		if (sPreferences != null) {
			sNoTicks      = sPreferences.getBoolean("noticks", false);
            sAllowDuplicateIds = sPreferences.getBoolean("allowduplicateids", false);
            sEdgeToEdge   = sPreferences.getBoolean("edgetoedge", true);
			Log.d(TAG, "Preferences updated: noTicks=" + sNoTicks +
					", allowDuplicateIds=" + sAllowDuplicateIds +
					", edgeToEdge=" + sEdgeToEdge);
		}
	}

	/**
	 * Sets the preferences and updates them internally. This method should be
	 * called by an activity in the onResume method.
	 */
	public static void setPreferences(SharedPreferences preferences) {
		Configuration.sPreferences = preferences;
		updatePreferences();
	}

    /**
     * Creates a directory
     * @param dir directory
     */
    private static void createOrCheckDir(File dir) throws Exception {
        if (dir.exists()) {
            Log.d(TAG, "Directory exists: " + dir.getAbsolutePath());
            return;
        }
        Log.d(TAG, "Creating directory: " + dir.getAbsolutePath());
        boolean created = dir.mkdirs();
        if (!created) {
            Log.e(TAG, "Failed to create directory: " + dir.getAbsolutePath());
            throw new Exception("Couldn't create " + dir);
        }
        Log.i(TAG, "Created directory: " + dir.getAbsolutePath());
    }

	/**
	 * Tries to initialize storage, obtain the file handles and then create
	 * folders if they don't exist already.
	 *
	 * Uses app-specific external storage directory which doesn't require
	 * runtime permissions on Android 10+ (scoped storage).
	 * Location: /storage/emulated/0/Android/data/org.univie.subjectiveplayer/files/
	 */
	public static void initialize(Context ctx) throws Exception {
		Log.d(TAG, "initialize called");
        sCtx = ctx;

        // Use app-specific external storage (works without permissions on Android 10+)
        sStorage = ctx.getExternalFilesDir(null);
		Log.d(TAG, "External storage state: " + Environment.getExternalStorageState());

        if (sStorage == null || !Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Log.e(TAG, "Could not initialize external storage");
            throw new Exception("Could not open external storage!");
        }

        Log.i(TAG, "Using app storage at " + sStorage.toString());

        // Create subdirectories within app storage
        sFolderApproot = new File(sStorage, PATH_CONFIG);
        sFolderVideos = new File(sStorage, PATH_VIDEOS);
        sFolderLogs = new File(sStorage, PATH_LOGS);

		Log.d(TAG, "Storage paths configured:");
		Log.d(TAG, "  Config folder: " + sFolderApproot.getAbsolutePath());
		Log.d(TAG, "  Videos folder: " + sFolderVideos.getAbsolutePath());
		Log.d(TAG, "  Logs folder: " + sFolderLogs.getAbsolutePath());

        // Create directories if they don't exist
        Configuration.createOrCheckDir(sFolderApproot);
        Configuration.createOrCheckDir(sFolderVideos);
        Configuration.createOrCheckDir(sFolderLogs);

		Log.i(TAG, "Configuration initialized successfully");
	}
}

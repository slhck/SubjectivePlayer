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
	 * The suffix of configuration files. It can be set in the application
	 * preferences
	 */
	public static String sConfigSuffix = null;

	/**
	 * Controls whether the logger writes the numbers instead of "Excellent, .."
	 * labels for ACR
	 */
	public static boolean sAcrNumbers = true;

	/**
	 * Controls whether the continuous dialog shows no ticks and just a maximum
	 * and minimum label
	 */
	public static boolean sNoTicks = false;


    /**
     * Controls whether the SD card is used
     */
    public static boolean sUseSdCard = false;


    /**
     * Allow using same ID twice or more
     */
    public static boolean sAllowDuplicateIds = false;

    /** The container for all application preferences */
	private static SharedPreferences sPreferences = null;


	/**
	 * Internally updates the preference values, called by setPreferences on
	 * each activity resume. This method only overwrites the internal members of
	 * this class with the contents of the SharedPreferences.
	 */
	private static void updatePreferences() {
		if (sPreferences != null) {
			sConfigSuffix = sPreferences.getString("configsuffix", "cfg");
			sAcrNumbers   = sPreferences.getBoolean("acrnumbers", true);
			sNoTicks      = sPreferences.getBoolean("noticks", false);
            sUseSdCard    = sPreferences.getBoolean("usesdcard", false);
            sAllowDuplicateIds = sPreferences.getBoolean("allowduplicateids", false);
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
        if (dir.exists()) return;
        boolean created = dir.mkdirs();
        if (!created) throw new Exception("Couldn't create " + dir);

    }

	/**
	 * Tries to initialize the SD card, obtain the file handles and then create
	 * folders if they don't exist already.
	 */
	public static void initialize(Context ctx) throws Exception {
        sCtx = ctx;
		if (Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
            // refers to the internal storage
            sStorage = Environment.getExternalStorageDirectory();
            Log.d(TAG, "Using internal storage at " + sStorage.toString());
        } else {
            Log.e(TAG, "Could not initialize internal storage");
            throw new Exception("Could not open internal storage!");
        }

        // check if the user requested SD card instead
        if (sUseSdCard) {
            File[] extDirs = ctx.getExternalFilesDirs(null);
            File extSdPath = new File("");
            for (File f : extDirs) {
                if (f.toString().contains("extSdCard")) {
                    extSdPath = f;
                }
            }
            if (extSdPath.exists() && extSdPath.canWrite()) {
                sStorage = extSdPath;
                Log.d(TAG, "Using external SD card at " + sStorage.toString());
            } else {
                // do nothing; use internal storage instead
            }
        }

        sFolderApproot = new File(sStorage, PATH_CONFIG);
        sFolderVideos = new File(sStorage, PATH_VIDEOS);
        sFolderLogs = new File(sStorage, PATH_LOGS);

        // create the data and video directory if they don't exist already
        Configuration.createOrCheckDir(sFolderApproot);
        Configuration.createOrCheckDir(sFolderVideos);
        Configuration.createOrCheckDir(sFolderLogs);
	}
}

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

import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

public abstract class Configuration {

	private static final String TAG = Configuration.class.getSimpleName();

	/** File handle for the root of the SD card */
	public static File sSDcard = null;

	/** File handle for the root of the app data folder */
	public static File sFolderApproot = null;

	/** File handle for the folder in which videos are stored */
	public static File sFolderVideos = null;

	/** File handle for the folder in which log files are stored */
	public static File sFolderLogs = null;

	/** File handle for a configuration file */
	public static File sFileConfig = null;

	/** File name of a configuration file */
	public static String sfileConfigName = null;

	/**
	 * Path of the root of the app data folder, relative to the SD card root,
	 * with a trailing slash. If it does not exist, it will be created
	 * automatically.
	 */
	public static final String PATH_APPROOT = new String(
			"Android/data/org.univie.subjectiveplayer/files/");

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
			sAcrNumbers = sPreferences.getBoolean("acrnumbers", true);
			sNoTicks = sPreferences.getBoolean("noticks", false);
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
	 * Tries to initialize the SD card, obtain the file handles and then create
	 * folders if they don't exist already.
	 */
	public static void initialize() throws Exception {
		if (Environment.getExternalStorageState().equalsIgnoreCase(
				Environment.MEDIA_MOUNTED)) {
			sSDcard = Environment.getExternalStorageDirectory();
			sFolderApproot = new File(sSDcard, PATH_CONFIG);
			sFolderVideos = new File(sSDcard, PATH_VIDEOS);
			sFolderLogs = new File(sSDcard, PATH_LOGS);

			// create the data and video directory if they don't exist already
			sFolderApproot.mkdirs();
			sFolderVideos.mkdirs();
			sFolderLogs.mkdirs();
		} else {
			Log.e(TAG, "Could not initialize SD card");
			throw new Exception("Could not open SD card!");
		}
	}
}

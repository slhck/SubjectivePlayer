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

import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Logging Class that writes log files with the user's ratings.
 * Writes ratings continuously as they are collected to prevent data loss if the test is cancelled.
 */
public abstract class CsvLogger {

	private static final String TAG = CsvLogger.class.getSimpleName();

	/** Sentinel value for BREAK video_position */
	private static final int BREAK_VIDEO_POSITION = -1;

	/** File handle for the session log file */
	private static File sSessionLogFile = null;
	/** FileWriter for session log */
	private static FileWriter sSessionFileWriter = null;
	/** BufferedWriter for session log */
	private static BufferedWriter sSessionBufferedWriter = null;
	/** Whether session logging has started */
	private static boolean sSessionLogStarted = false;


	/**
	 * The date format as specified in SimpleDateFormat for writing the filename
	 */
	private static final String DATE_FORMAT = "yyyyMMdd-HHmmss";
	/**
	 * ISO8601 date format for the rated_at column
	 */
	private static final String ISO8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";
	/** The CSV separator character */
	private static final char SEP_CSV = ',';
	/** The File separator character, e.g. a space */
	private static final char SEP_FILE = '_';
	/** Whether a CSV header should be written or not */
	private static final boolean HEADER = true;
	/** The file suffix */
	private static final String SUFFIX = "csv";

	/**
	 * Converts a Unix epoch timestamp in milliseconds to ISO8601 format
	 * @param millis Unix epoch timestamp in milliseconds
	 * @return ISO8601 formatted date string
	 */
	private static String formatAsIso8601(long millis) {
		SimpleDateFormat iso8601Format = new SimpleDateFormat(ISO8601_FORMAT, Locale.US);
		return iso8601Format.format(new Date(millis));
	}

    /**
     * Check whether an ID already exists in the log files
     */
    public static boolean idExists(int id) {
        File[] files = Configuration.sFolderLogs.listFiles();
        if (files == null) {
            return false;
        }
        for (File f : files) {
            String idPart = f.getName().split("_", 2)[0];
            try {
                int currentId = Integer.parseInt(idPart);
                if (currentId == id) {
                    return true;
                }
            } catch (Exception e) {
                // do nothing
            }
        }
        return false;
    }

	/**
	 * Starts a session log file. This should be called when the test session begins.
	 * The file is created with a header and will be written to incrementally as ratings come in.
	 * File name format: ID_StartTime_Method.csv
	 */
	public static void startSessionLog() {
		if (sSessionLogStarted) {
			Log.w(TAG, "Session log already started, closing previous one");
			closeSessionLog();
		}

		try {
			SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT, Locale.US);
			String methodName = Methods.METHOD_NAMES[Session.sCurrentMethod];
			methodName = methodName.replace(' ', SEP_FILE);

			// ID_StartTime_Method.csv
			String fileName = "" + Session.sParticipantId + SEP_FILE
					+ format.format(new Date()) + SEP_FILE + methodName
					+ "." + SUFFIX;
			sSessionLogFile = new File(Configuration.sFolderLogs, fileName);

			Log.d(TAG, "Starting session log: " + sSessionLogFile.getAbsolutePath());

			sSessionFileWriter = new FileWriter(sSessionLogFile);
			sSessionBufferedWriter = new BufferedWriter(sSessionFileWriter);

			if (HEADER) {
				sSessionBufferedWriter.write("video_position" + SEP_CSV + "video_name" + SEP_CSV
						+ "rating" + SEP_CSV + "rated_at");
				sSessionBufferedWriter.newLine();
				sSessionBufferedWriter.flush();
			}

			sSessionLogStarted = true;
			Log.i(TAG, "Session log started: " + fileName);
		} catch (IOException e) {
			Log.e(TAG, "Error starting session log: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Logs a single rating entry to the session log file.
	 * This should be called immediately after each rating is collected.
	 *
	 * @param videoPosition The position/index of the video in the playlist
	 * @param videoName The name of the video file
	 * @param rating The rating value
	 * @param ratedAtMillis The timestamp when the rating was made (Unix epoch ms)
	 */
	public static void logRating(int videoPosition, String videoName, int rating, long ratedAtMillis) {
		if (!sSessionLogStarted) {
			Log.w(TAG, "Session log not started, starting now");
			startSessionLog();
		}

		try {
			sSessionBufferedWriter.write("" + videoPosition + SEP_CSV + videoName + SEP_CSV
					+ rating + SEP_CSV + formatAsIso8601(ratedAtMillis));
			sSessionBufferedWriter.newLine();
			sSessionBufferedWriter.flush();
			Log.d(TAG, "Logged rating: video=" + videoName + ", rating=" + rating);
		} catch (IOException e) {
			Log.e(TAG, "Error logging rating: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Logs a BREAK entry to the session log file.
	 * BREAK entries have video_position=-1, video_name=BREAK, and empty rating/rated_at.
	 */
	public static void logBreak() {
		if (!sSessionLogStarted) {
			Log.w(TAG, "Session log not started, starting now");
			startSessionLog();
		}

		try {
			sSessionBufferedWriter.write("" + BREAK_VIDEO_POSITION + SEP_CSV + "BREAK" + SEP_CSV + SEP_CSV);
			sSessionBufferedWriter.newLine();
			sSessionBufferedWriter.flush();
			Log.d(TAG, "Logged BREAK entry");
		} catch (IOException e) {
			Log.e(TAG, "Error logging break: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Closes the session log file. Should be called when the session ends.
	 */
	public static void closeSessionLog() {
		if (!sSessionLogStarted) {
			Log.d(TAG, "Session log not started, nothing to close");
			return;
		}

		try {
			if (sSessionBufferedWriter != null) {
				sSessionBufferedWriter.close();
			}
			if (sSessionFileWriter != null) {
				sSessionFileWriter.close();
			}
			Log.i(TAG, "Session log closed: " + (sSessionLogFile != null ? sSessionLogFile.getName() : "unknown"));
		} catch (IOException e) {
			Log.e(TAG, "Error closing session log: " + e.getMessage());
			e.printStackTrace();
		} finally {
			sSessionLogStarted = false;
			sSessionBufferedWriter = null;
			sSessionFileWriter = null;
			sSessionLogFile = null;
		}
	}

	/**
	 * @deprecated Use logRating() instead - continuous ratings now go to the session log
	 */
	@Deprecated
	public static void startContinuousLogCSV(String videoName) {
		// No-op: continuous ratings now use the session log
		Log.d(TAG, "startContinuousLogCSV is deprecated, using session log instead");
	}

	/**
	 * @deprecated Use logRating() instead - continuous ratings now go to the session log
	 */
	@Deprecated
	public static void writeContinuousData(String videoName, String timeStamp, String data) {
		// No-op: continuous ratings now use the session log
		Log.w(TAG, "writeContinuousData is deprecated, use logRating() instead");
	}

	/**
	 * @deprecated Use closeSessionLog() instead - continuous ratings now go to the session log
	 */
	@Deprecated
	public static void closeContinuousLogCSV() {
		// No-op: continuous ratings now use the session log
		Log.d(TAG, "closeContinuousLogCSV is deprecated, using session log instead");
	}
}

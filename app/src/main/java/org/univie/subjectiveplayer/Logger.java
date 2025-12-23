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
import java.util.TimeZone;

/**
 * Logging Class that writes log files with the user's ratings
 */
public abstract class Logger {

	private static final String TAG = Logger.class.getSimpleName();

	/** Sentinel value for BREAK video_position */
	private static final int BREAK_VIDEO_POSITION = -1;

	/** File handle for the log file */
	private static File sLogFile = null;
	/** The constructed current file name */
	private static String sFileName = null;
	/**
	 * The date format as specified in SimpleDateFormat for writing the filename
	 */
	private static final String DATE_FORMAT = "yyyyMMdd-HHmm";
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

	/** Whether a continuous log is running or not */
	private static boolean sContinuousLogStarted = false;

	private static FileWriter sFileWriter;
	private static BufferedWriter sBufferedWriter;

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

        for (File f : Configuration.sFolderLogs.listFiles()) {
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
	 * Writes a log of the session data to the specified file
	 */
	public static void writeSessionLogCSV() {
		Log.d(TAG, "writeSessionLogCSV called");
		try {
			SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
			String methodName = Methods.METHOD_NAMES[Session.sCurrentMethod];
			methodName = methodName.replace(' ', SEP_FILE);

            // ID_Date_Method.csv
			sFileName = "" + Session.sParticipantId + SEP_FILE
					+ format.format(new Date()) + SEP_FILE + methodName
					+ "." + SUFFIX;
			sLogFile = new File(Configuration.sFolderLogs, sFileName);

			Log.d(TAG, "Writing session log to: " + sLogFile.getAbsolutePath());

			sFileWriter = new FileWriter(sLogFile);
			sBufferedWriter = new BufferedWriter(sFileWriter);

			if (HEADER) {
				sBufferedWriter.write("video_position" + SEP_CSV + "video_name" + SEP_CSV
						+ "rating" + SEP_CSV + "rated_at");
				sBufferedWriter.newLine();
			}

			// iterate through all ratings
			for (int i = 0; i < Session.sTracks.size(); ++i) {
				String trackName = Session.sTracks.get(i);

				// Check if this is a BREAK entry
				if (Session.isBreakCommand(trackName)) {
					// Write BREAK entry: video_position = -1, video_name = BREAK, rating = empty, rated_at = empty
					sBufferedWriter.write("" + BREAK_VIDEO_POSITION + SEP_CSV + "BREAK" + SEP_CSV + SEP_CSV);
					Log.d(TAG, "Wrote BREAK entry for track " + i + ": " + trackName);
				} else {
					// Write video_position and video_name
					sBufferedWriter.write("" + i + SEP_CSV + trackName + SEP_CSV);

					// write the rating (always as number)
					Integer rating = Session.sRatings.get(i);
					sBufferedWriter.write(rating.toString());
					Log.d(TAG, "Wrote rating for track " + i + ": " + trackName + " = " + rating);

					// Write rated_at as ISO8601
					sBufferedWriter.write("" + SEP_CSV);
					sBufferedWriter.write(formatAsIso8601(Session.sRatingTime.get(i)));
				}
				sBufferedWriter.newLine();
			}

			sBufferedWriter.close();
			sFileWriter.close();
			Log.i(TAG, "Session log written successfully: " + sFileName);
		} catch (IOException e) {
			Log.e(TAG, "Error writing session log: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Starts a continuous results file, including header information if needed
	 *
	 * @param videoName
	 *            The name of the video, to be used for the file name
	 */
	public static void startContinuousLogCSV(String videoName) {
		try {
			SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
			String methodName = Methods.METHOD_NAMES[Session.sCurrentMethod];
			methodName = methodName.replace(' ', SEP_FILE);

			sFileName = "" + Session.sParticipantId + SEP_FILE
					+ format.format(new Date()) + SEP_FILE + methodName
					+ SEP_FILE + videoName + "." + SUFFIX;
			sLogFile = new File(Configuration.sFolderLogs, sFileName);

			sFileWriter = new FileWriter(sLogFile);
			sBufferedWriter = new BufferedWriter(sFileWriter);

			if (HEADER) {
				sBufferedWriter.write("USERID" + SEP_CSV + "VIDEONAME"
						+ SEP_CSV + "TIMESTAMP" + SEP_CSV + "RATING");
				sBufferedWriter.newLine();
			}

			sContinuousLogStarted = true;

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Writes one continuous data line to the file. IO issues here?
	 *
	 * @param videoName
	 *            The name of the video file itself
	 * @param timeStamp
	 *            The date timestamp that should appear in the result file
	 * @param data
	 *            The rating itself, whatever it is
	 */
	public static void writeContinuousData(String videoName, String timeStamp,
			String data) {
		try {
			if (!sContinuousLogStarted) {
				throw new Exception("Can't log if file wasn't started yet.");
			}

			sBufferedWriter.write("" + Session.sParticipantId + SEP_CSV
					+ videoName + SEP_CSV + timeStamp + SEP_CSV + data);
			sBufferedWriter.newLine();
			sBufferedWriter.flush();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void closeContinuousLogCSV() {
		sContinuousLogStarted = false;

		try {
			sBufferedWriter.close();
			sFileWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

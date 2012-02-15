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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.util.Log;

/**
 * Logging Class that writes log files with the user's ratings
 */
public abstract class Logger {

	/** File handle for the log file */
	private static File sLogFile = null;
	/** The constructed current file name */
	private static String sFileName = null;
	/**
	 * The date format as specified in SimpleDateFormat for writing the filename
	 */
	private static final String DATE_FORMAT = "yyyyMMdd-HHmm";
	/** The CSV separator character */
	private static final char SEP_CSV = ';';
	/** The File separator character, e.g. a space */
	private static final char SEP_FILE = '_';
	/** Whether a CSV header should be written or not */
	private static final boolean HEADER = true;
	/** The file suffix */
	private static final String SUFFIX = "txt";

	/** Whether a continuous log is running or not */
	private static boolean sContinuousLogStarted = false;

	private static FileWriter sFileWriter;
	private static BufferedWriter sBufferedWriter;

	/**
	 * Writes a log of the session data to the specified file
	 */
	public static void writeSessionLogCSV() {
		try {
			SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
			String methodName = Methods.METHOD_NAMES[Session.sCurrentMethod];
			methodName = methodName.replace(' ', SEP_FILE);

			sFileName = "" + Session.sParticipantId + SEP_FILE
					+ format.format(new Date()) + SEP_FILE + methodName
					+ SEP_FILE + "." + SUFFIX;
			sLogFile = new File(Configuration.sFolderLogs, sFileName);

			sFileWriter = new FileWriter(sLogFile);
			sBufferedWriter = new BufferedWriter(sFileWriter);

			if (HEADER) {
				sBufferedWriter.write("VIDEO" + SEP_CSV + "VIDEONAME" + SEP_CSV
						+ "RATING");
				sBufferedWriter.newLine();
			}

			// iterate through all ratings
			for (int i = 0; i < Session.sTracks.size(); ++i) {

				// write the first line
				sBufferedWriter.write("" + i + SEP_CSV + Session.sTracks.get(i)
						+ SEP_CSV);

				// write the rating depending on the method
				switch (Session.sCurrentMethod) {
				case (Methods.TYPE_ACR_CATEGORIGAL):
					if (Configuration.sAcrNumbers)
						sBufferedWriter.write(Session.sRatings.get(i)
								.toString());
					else
						sBufferedWriter
								.write(Methods.LABELS_ACR[Session.sRatings
										.get(i)]);
					break;
				case (Methods.TYPE_CONTINUOUS):
					sBufferedWriter.write(Session.sRatings.get(i).toString());
					break;
				default:
					sBufferedWriter.write(i);
				}
				sBufferedWriter.newLine();
			}

			sBufferedWriter.close();
			sFileWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
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

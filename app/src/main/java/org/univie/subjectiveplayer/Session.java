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
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

/**
 * Class that stores session data for each participant
 */
public abstract class Session {

	private static final String TAG = Session.class.getSimpleName();

	/** the participant's ID */
	public static int sParticipantId = 0;

	/** the ID of the method the participant is using */
	public static int sCurrentMethod = Methods.UNDEFINED;

	/** the index of the currently playing track */
	public static int sCurrentTrack = 0;

	/** the tracks to be shown */
	public static List<String> sTracks = new ArrayList<>();

	/** the ratings for each corresponding track */
	public static List<Integer> sRatings = new ArrayList<>();

    /** the rating time for each corresponding track */
    public static List<Long> sRatingTime = new ArrayList<>();

    /** Custom start message from config file */
    public static String sStartMessage = null;

    /** Custom finish message from config file */
    public static String sFinishMessage = null;

    /** Custom training message from config file */
    public static String sTrainingMessage = null;

    /** Custom pre-questionnaire message from config file */
    public static String sPreQuestionnaireMessage = null;

    /** Custom post-questionnaire message from config file */
    public static String sPostQuestionnaireMessage = null;

    /** Pre-questionnaire (questions before the test) */
    public static Questionnaire sPreQuestionnaire = null;

    /** Post-questionnaire (questions after the test) */
    public static Questionnaire sPostQuestionnaire = null;

    /** Pre-questionnaire answers (parallel to sPreQuestionnaire.getQuestions()) */
    public static List<QuestionnaireAnswer> sPreQuestionnaireAnswers = new ArrayList<>();

    /** Post-questionnaire answers (parallel to sPostQuestionnaire.getQuestions()) */
    public static List<QuestionnaireAnswer> sPostQuestionnaireAnswers = new ArrayList<>();

    /** Index in sTracks where training starts (-1 if no training section) */
    public static int sTrainingStartIndex = -1;

    /** Index in sTracks where training ends (-1 if no training section) */
    public static int sTrainingEndIndex = -1;

    /** Prefix for BREAK commands in playlist files */
	public static final String BREAK_PREFIX = "BREAK";

	/** Prefix for METHOD directive in playlist files */
	public static final String METHOD_PREFIX = "METHOD";

	/** Prefix for START_MESSAGE directive in playlist files */
	public static final String START_MESSAGE_PREFIX = "START_MESSAGE";

	/** Prefix for FINISH_MESSAGE directive in playlist files */
	public static final String FINISH_MESSAGE_PREFIX = "FINISH_MESSAGE";

	/** Prefix for TRAINING_MESSAGE directive in playlist files */
	public static final String TRAINING_MESSAGE_PREFIX = "TRAINING_MESSAGE";

	/** Marker for beginning of training section in playlist files */
	public static final String TRAINING_START_MARKER = "TRAINING_START";

	/** Marker for end of training section in playlist files */
	public static final String TRAINING_END_MARKER = "TRAINING_END";

	/**
	 * Checks if a track entry is a BREAK command
	 * @param track The track entry to check
	 * @return true if the track is a BREAK command
	 */
	public static boolean isBreakCommand(String track) {
		return track != null && track.startsWith(BREAK_PREFIX);
	}

	/**
	 * Parses the duration from a BREAK command.
	 * @param track The BREAK command string (e.g., "BREAK 60")
	 * @return The duration in seconds, or -1 if no duration specified
	 */
	public static int parseBreakDuration(String track) {
		if (!isBreakCommand(track)) {
			return -1;
		}
		String[] parts = track.trim().split("\\s+");
		if (parts.length >= 2) {
			try {
				int duration = Integer.parseInt(parts[1]);
				Log.d(TAG, "Parsed BREAK duration: " + duration + " seconds");
				return duration;
			} catch (NumberFormatException e) {
				Log.w(TAG, "Could not parse BREAK duration from: " + track);
				return -1;
			}
		}
		Log.d(TAG, "BREAK command without duration: " + track);
		return -1;
	}

	/**
	 * Checks if a line is a METHOD directive
	 * @param line The line to check
	 * @return true if the line is a METHOD directive
	 */
	public static boolean isMethodDirective(String line) {
		return line != null && line.toUpperCase().startsWith(METHOD_PREFIX);
	}

	/**
	 * Checks if a line is a START_MESSAGE directive
	 * @param line The line to check
	 * @return true if the line is a START_MESSAGE directive
	 */
	public static boolean isStartMessageDirective(String line) {
		return line != null && line.toUpperCase().startsWith(START_MESSAGE_PREFIX);
	}

	/**
	 * Checks if a line is a FINISH_MESSAGE directive
	 * @param line The line to check
	 * @return true if the line is a FINISH_MESSAGE directive
	 */
	public static boolean isFinishMessageDirective(String line) {
		return line != null && line.toUpperCase().startsWith(FINISH_MESSAGE_PREFIX);
	}

	/**
	 * Checks if a line is a TRAINING_MESSAGE directive
	 * @param line The line to check
	 * @return true if the line is a TRAINING_MESSAGE directive
	 */
	public static boolean isTrainingMessageDirective(String line) {
		return line != null && line.toUpperCase().startsWith(TRAINING_MESSAGE_PREFIX);
	}

	/**
	 * Checks if a line is a TRAINING_START marker
	 * @param line The line to check
	 * @return true if the line is a TRAINING_START marker
	 */
	public static boolean isTrainingStartMarker(String line) {
		return line != null && line.trim().toUpperCase().equals(TRAINING_START_MARKER);
	}

	/**
	 * Checks if a line is a TRAINING_END marker
	 * @param line The line to check
	 * @return true if the line is a TRAINING_END marker
	 */
	public static boolean isTrainingEndMarker(String line) {
		return line != null && line.trim().toUpperCase().equals(TRAINING_END_MARKER);
	}

	/**
	 * Checks if a training section is defined in the current session
	 * @return true if both TRAINING_START and TRAINING_END are defined
	 */
	public static boolean hasTrainingSection() {
		return sTrainingStartIndex >= 0 && sTrainingEndIndex >= 0;
	}

	/**
	 * Checks if a given track index is within the training section
	 * @param trackIndex The track index to check
	 * @return true if the track is within the training section
	 */
	public static boolean isTrainingTrack(int trackIndex) {
		if (!hasTrainingSection()) {
			return false;
		}
		return trackIndex >= sTrainingStartIndex && trackIndex <= sTrainingEndIndex;
	}

	/**
	 * Checks if a given track index is the first training track
	 * @param trackIndex The track index to check
	 * @return true if this is the first track in the training section
	 */
	public static boolean isFirstTrainingTrack(int trackIndex) {
		return hasTrainingSection() && trackIndex == sTrainingStartIndex;
	}

	/**
	 * Checks if a given track index is the last training track
	 * @param trackIndex The track index to check
	 * @return true if this is the last track in the training section
	 */
	public static boolean isLastTrainingTrack(int trackIndex) {
		return hasTrainingSection() && trackIndex == sTrainingEndIndex;
	}

	/**
	 * Parses the message from a directive line (START_MESSAGE or FINISH_MESSAGE).
	 * Supports escaped newlines: \\n in the config file becomes actual newlines.
	 * @param line The directive line (e.g., "START_MESSAGE Hello\\nWorld")
	 * @param prefix The directive prefix to strip (e.g., "START_MESSAGE")
	 * @return The parsed message with escaped newlines converted, or null if invalid
	 */
	public static String parseMessageDirective(String line, String prefix) {
		if (line == null || !line.toUpperCase().startsWith(prefix)) {
			return null;
		}
		// Get everything after the prefix
		String message = line.substring(prefix.length()).trim();
		if (message.isEmpty()) {
			Log.w(TAG, prefix + " directive without message");
			return null;
		}
		// Convert escaped newlines to actual newlines
		message = message.replace("\\n", "\n");
		Log.d(TAG, "Parsed " + prefix + ": " + message.replace("\n", "\\n"));
		return message;
	}

	/**
	 * Parses the method type from a METHOD directive.
	 * Supported values: ACR, CONTINUOUS, DSIS, TIME_CONTINUOUS
	 * @param line The METHOD directive string (e.g., "METHOD ACR")
	 * @return The method type constant, or Methods.UNDEFINED if invalid
	 */
	public static int parseMethodType(String line) {
		if (!isMethodDirective(line)) {
			return Methods.UNDEFINED;
		}
		String[] parts = line.trim().split("\\s+");
		if (parts.length >= 2) {
			String methodName = parts[1].toUpperCase();
			switch (methodName) {
				case "ACR":
					Log.d(TAG, "Parsed METHOD: ACR (TYPE_ACR_CATEGORICAL)");
					return Methods.TYPE_ACR_CATEGORICAL;
				case "CONTINUOUS":
					Log.d(TAG, "Parsed METHOD: CONTINUOUS (TYPE_CONTINUOUS)");
					return Methods.TYPE_CONTINUOUS;
				case "DSIS":
					Log.d(TAG, "Parsed METHOD: DSIS (TYPE_DSIS_CATEGORICAL)");
					return Methods.TYPE_DSIS_CATEGORICAL;
				case "TIME_CONTINUOUS":
					Log.d(TAG, "Parsed METHOD: TIME_CONTINUOUS (TYPE_TIME_CONTINUOUS)");
					return Methods.TYPE_TIME_CONTINUOUS;
				default:
					Log.w(TAG, "Unknown method type: " + methodName);
					return Methods.UNDEFINED;
			}
		}
		Log.w(TAG, "METHOD directive without type: " + line);
		return Methods.UNDEFINED;
	}

	/**
	 * Tries to extract all video file names from the configuration file passed
	 * and saves them to a vector of Strings. These are just the names of the
	 * videos, not their complete paths. The path can be generated by combining
	 * it from the settings in the Configuration. Missing files will not be
	 * included in the playlist. BREAK commands are preserved as-is.
	 *
	 * Supports both JSON (.json) and legacy text (.cfg) config file formats.
	 * The format is determined by file extension.
	 */
	public static void readVideosFromFile(File configFile) {
		Log.d(TAG, "Reading playlist from file: " + configFile.getAbsolutePath());

		if (!configFile.exists() || !configFile.canRead()) {
			Log.e(TAG, "Config file does not exist or cannot be read: " + configFile.getAbsolutePath());
			return;
		}

		// Parse config file using appropriate parser based on extension
		BaseConfigFile config = ConfigFileFactory.create(configFile);

		// Log any parse errors (but continue - validation is done separately)
		for (BaseConfigFile.ParseError error : config.getParseErrors()) {
			Log.w(TAG, "Config parse error: " + error.message);
		}

		// Copy parsed data to session state
		if (config.getMethod() != Methods.UNDEFINED) {
			sCurrentMethod = config.getMethod();
			Log.i(TAG, "Method set from config file: " + Methods.METHOD_NAMES[sCurrentMethod]);
		}

		if (config.getStartMessage() != null) {
			sStartMessage = config.getStartMessage();
			Log.i(TAG, "Start message set from config file");
		}

		if (config.getFinishMessage() != null) {
			sFinishMessage = config.getFinishMessage();
			Log.i(TAG, "Finish message set from config file");
		}

		if (config.getTrainingMessage() != null) {
			sTrainingMessage = config.getTrainingMessage();
			Log.i(TAG, "Training message set from config file");
		}

		if (config.getPreQuestionnaireMessage() != null) {
			sPreQuestionnaireMessage = config.getPreQuestionnaireMessage();
			Log.i(TAG, "Pre-questionnaire message set from config file");
		}

		if (config.getPostQuestionnaireMessage() != null) {
			sPostQuestionnaireMessage = config.getPostQuestionnaireMessage();
			Log.i(TAG, "Post-questionnaire message set from config file");
		}

		// Copy questionnaires
		if (config.hasPreQuestionnaire()) {
			sPreQuestionnaire = config.getPreQuestionnaire();
			Log.i(TAG, "Pre-questionnaire loaded with " + sPreQuestionnaire.size() + " questions");
		}

		if (config.hasPostQuestionnaire()) {
			sPostQuestionnaire = config.getPostQuestionnaire();
			Log.i(TAG, "Post-questionnaire loaded with " + sPostQuestionnaire.size() + " questions");
		}

		// Copy entries to tracks
		sTracks.addAll(config.getEntries());

		// Copy training indices
		sTrainingStartIndex = config.getTrainingStartIndex();
		sTrainingEndIndex = config.getTrainingEndIndex();
		if (hasTrainingSection()) {
			Log.i(TAG, "Training section: indices " + sTrainingStartIndex + " to " + sTrainingEndIndex);
		}

		// Filter out missing video files (skip BREAK commands)
		for (int i = sTracks.size() - 1; i >= 0; i--) {
			String track = sTracks.get(i);
			if (isBreakCommand(track)) {
				Log.d(TAG, "Keeping BREAK command at index " + i + ": " + track);
				continue;
			}
			File f = new File(Configuration.sFolderVideos, track);
			if (!f.exists()) {
				Log.w(TAG, "Video file not found, removing from playlist: " + f.getAbsolutePath());
				sTracks.remove(i);
				// Adjust training indices if needed
				if (sTrainingStartIndex > i) {
					sTrainingStartIndex--;
				}
				if (sTrainingEndIndex >= i) {
					sTrainingEndIndex--;
				}
			}
		}

		Log.d(TAG, "Playlist loaded with " + sTracks.size() + " entries");
	}

	/**
	 * Checks if a pre-questionnaire is defined
	 * @return true if a pre-questionnaire with at least one question exists
	 */
	public static boolean hasPreQuestionnaire() {
		return sPreQuestionnaire != null && !sPreQuestionnaire.isEmpty();
	}

	/**
	 * Checks if a post-questionnaire is defined
	 * @return true if a post-questionnaire with at least one question exists
	 */
	public static boolean hasPostQuestionnaire() {
		return sPostQuestionnaire != null && !sPostQuestionnaire.isEmpty();
	}

	/**
	 * Resets the session before another round.
	 */
	public static void reset() {
		sParticipantId = 0;
		sCurrentTrack = 0;
		sCurrentMethod = Methods.UNDEFINED;
		sTracks = new ArrayList<>();
		sRatings = new ArrayList<>();
        sRatingTime = new ArrayList<>();
		sStartMessage = null;
		sFinishMessage = null;
		sTrainingMessage = null;
		sPreQuestionnaireMessage = null;
		sPostQuestionnaireMessage = null;
		sPreQuestionnaire = null;
		sPostQuestionnaire = null;
		sPreQuestionnaireAnswers = new ArrayList<QuestionnaireAnswer>();
		sPostQuestionnaireAnswers = new ArrayList<QuestionnaireAnswer>();
		sTrainingStartIndex = -1;
		sTrainingEndIndex = -1;
	}
}

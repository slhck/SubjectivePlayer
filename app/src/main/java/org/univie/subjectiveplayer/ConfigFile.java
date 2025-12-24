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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a parsed config file with all its metadata.
 * Parses once and provides accessors for all config information.
 */
public class ConfigFile {

    private static final String TAG = ConfigFile.class.getSimpleName();

    private final File file;
    private final String filename;
    private final String id;

    // Parsed content
    private int method = Methods.UNDEFINED;
    private String startMessage = null;
    private String finishMessage = null;
    private String trainingMessage = null;

    // Video entries (including BREAK commands)
    private final List<String> entries = new ArrayList<>();

    // Training section indices (-1 if not defined)
    private int trainingStartIndex = -1;
    private int trainingEndIndex = -1;

    // Parsing errors found
    private final List<ParseError> parseErrors = new ArrayList<>();

    // Statistics
    private int videoCount = 0;
    private int trainingVideoCount = 0;
    private int breakCount = 0;

    /**
     * Represents a parsing error with location information
     */
    public static class ParseError {
        public final int lineNumber;
        public final String message;

        public ParseError(int lineNumber, String message) {
            this.lineNumber = lineNumber;
            this.message = message;
        }
    }

    /**
     * Creates and parses a config file.
     * @param configFile The config file to parse
     */
    public ConfigFile(File configFile) {
        this.file = configFile;
        this.filename = configFile.getName();
        this.id = extractIdFromFilename(filename);
        parse();
    }

    /**
     * Extracts the participant ID from a config filename.
     * Matches patterns like "subject_1.cfg", "playlist1.cfg", "1.cfg"
     */
    private static String extractIdFromFilename(String filename) {
        Pattern pattern = Pattern.compile("(\\d+)");
        Matcher matcher = pattern.matcher(filename);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "-";
    }

    /**
     * Parses the config file and populates all fields.
     */
    private void parse() {
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {

            String line;
            int lineNumber = 0;
            boolean firstNonEmptyLine = true;
            boolean inTrainingSection = false;
            int trainingStartLine = -1;
            int trainingEndLine = -1;

            while ((line = br.readLine()) != null) {
                lineNumber++;
                String trimmedLine = line.trim();

                if (trimmedLine.isEmpty()) {
                    continue;
                }

                // Check for METHOD directive
                if (Session.isMethodDirective(trimmedLine)) {
                    int parsedMethod = Session.parseMethodType(trimmedLine);
                    if (parsedMethod == Methods.UNDEFINED) {
                        String[] parts = trimmedLine.split("\\s+");
                        String methodName = parts.length >= 2 ? parts[1] : "(empty)";
                        parseErrors.add(new ParseError(lineNumber,
                                "Unknown METHOD \"" + methodName +
                                "\" (valid: ACR, CONTINUOUS, DSIS, TIME_CONTINUOUS)"));
                    } else {
                        method = parsedMethod;
                    }
                    firstNonEmptyLine = false;
                    continue;
                }

                firstNonEmptyLine = false;

                // Check for START_MESSAGE directive
                if (Session.isStartMessageDirective(trimmedLine)) {
                    String msg = Session.parseMessageDirective(trimmedLine, Session.START_MESSAGE_PREFIX);
                    if (msg != null) {
                        startMessage = msg;
                    }
                    continue;
                }

                // Check for FINISH_MESSAGE directive
                if (Session.isFinishMessageDirective(trimmedLine)) {
                    String msg = Session.parseMessageDirective(trimmedLine, Session.FINISH_MESSAGE_PREFIX);
                    if (msg != null) {
                        finishMessage = msg;
                    }
                    continue;
                }

                // Check for TRAINING_MESSAGE directive
                if (Session.isTrainingMessageDirective(trimmedLine)) {
                    String msg = Session.parseMessageDirective(trimmedLine, Session.TRAINING_MESSAGE_PREFIX);
                    if (msg != null) {
                        trainingMessage = msg;
                    }
                    continue;
                }

                // Check for TRAINING_START marker
                if (Session.isTrainingStartMarker(trimmedLine)) {
                    trainingStartIndex = entries.size();
                    trainingStartLine = lineNumber;
                    inTrainingSection = true;
                    continue;
                }

                // Check for TRAINING_END marker
                if (Session.isTrainingEndMarker(trimmedLine)) {
                    trainingEndIndex = entries.size() - 1;
                    trainingEndLine = lineNumber;
                    inTrainingSection = false;
                    continue;
                }

                // Check for BREAK command
                if (Session.isBreakCommand(trimmedLine)) {
                    String[] parts = trimmedLine.split("\\s+");
                    if (parts.length >= 2) {
                        try {
                            int duration = Integer.parseInt(parts[1]);
                            if (duration < 0) {
                                parseErrors.add(new ParseError(lineNumber,
                                        "BREAK duration must be non-negative"));
                            }
                        } catch (NumberFormatException e) {
                            parseErrors.add(new ParseError(lineNumber,
                                    "BREAK duration \"" + parts[1] + "\" is not a valid number"));
                        }
                    }
                    entries.add(trimmedLine);
                    breakCount++;
                    continue;
                }

                // Otherwise it's a video file entry
                entries.add(trimmedLine);
                if (inTrainingSection) {
                    trainingVideoCount++;
                } else {
                    videoCount++;
                }
            }

            // Validate TRAINING_START and TRAINING_END pairing
            if (trainingStartLine > 0 && trainingEndLine < 0) {
                parseErrors.add(new ParseError(trainingStartLine,
                        "TRAINING_START without matching TRAINING_END"));
            } else if (trainingStartLine < 0 && trainingEndLine > 0) {
                parseErrors.add(new ParseError(trainingEndLine,
                        "TRAINING_END without matching TRAINING_START"));
            } else if (trainingStartLine > 0 && trainingEndLine > 0 && trainingEndLine <= trainingStartLine) {
                parseErrors.add(new ParseError(trainingEndLine,
                        "TRAINING_END must come after TRAINING_START (line " + trainingStartLine + ")"));
            }

        } catch (Exception e) {
            Log.e(TAG, "Error parsing config file: " + filename, e);
            parseErrors.add(new ParseError(0, "Could not read file: " + e.getMessage()));
        }
    }

    // Getters

    public File getFile() {
        return file;
    }

    public String getFilename() {
        return filename;
    }

    public String getId() {
        return id;
    }

    public int getMethod() {
        return method;
    }

    public String getMethodName() {
        if (method >= 0 && method < Methods.METHOD_NAMES.length) {
            return Methods.METHOD_NAMES[method];
        }
        return null;
    }

    public String getStartMessage() {
        return startMessage;
    }

    public String getFinishMessage() {
        return finishMessage;
    }

    public String getTrainingMessage() {
        return trainingMessage;
    }

    public List<String> getEntries() {
        return entries;
    }

    public int getTrainingStartIndex() {
        return trainingStartIndex;
    }

    public int getTrainingEndIndex() {
        return trainingEndIndex;
    }

    public boolean hasTrainingSection() {
        return trainingStartIndex >= 0 && trainingEndIndex >= 0;
    }

    public int getVideoCount() {
        return videoCount;
    }

    public int getTrainingVideoCount() {
        return trainingVideoCount;
    }

    public int getTotalVideoCount() {
        return videoCount + trainingVideoCount;
    }

    public int getBreakCount() {
        return breakCount;
    }

    public List<ParseError> getParseErrors() {
        return parseErrors;
    }

    public boolean hasErrors() {
        return !parseErrors.isEmpty();
    }

    /**
     * Gets the list of video filenames (excluding BREAK commands).
     */
    public List<String> getVideoFilenames() {
        List<String> videos = new ArrayList<>();
        for (String entry : entries) {
            if (!Session.isBreakCommand(entry)) {
                videos.add(entry);
            }
        }
        return videos;
    }
}

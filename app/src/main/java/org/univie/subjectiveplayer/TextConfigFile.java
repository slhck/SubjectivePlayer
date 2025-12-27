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

/**
 * Parses text-based config files (.cfg format).
 * This is the legacy format with one directive or video filename per line.
 */
public class TextConfigFile extends BaseConfigFile {

    private static final String TAG = TextConfigFile.class.getSimpleName();

    /**
     * Creates and parses a text config file.
     * @param configFile The config file to parse
     */
    public TextConfigFile(File configFile) {
        super(configFile);
        parse();
    }

    /**
     * Parses the config file and populates all fields.
     */
    @Override
    protected void parse() {
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {

            String line;
            int lineNumber = 0;
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
                    continue;
                }

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
}

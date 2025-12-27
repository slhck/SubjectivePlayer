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

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * Parses JSON-based config files (.json format).
 * This is the preferred format with structured data and questionnaire support.
 */
public class JsonConfigFile extends BaseConfigFile {

    private static final String TAG = JsonConfigFile.class.getSimpleName();

    /**
     * Creates and parses a JSON config file.
     * @param configFile The config file to parse
     */
    public JsonConfigFile(File configFile) {
        super(configFile);
        parse();
    }

    /**
     * Internal class representing the JSON structure.
     */
    private static class JsonConfig {
        @SerializedName("method")
        String method;

        @SerializedName("custom_messages")
        CustomMessages customMessages;

        @SerializedName("playlist")
        List<String> playlist;

        @SerializedName("pre_questionnaire")
        List<Question> preQuestionnaire;

        @SerializedName("post_questionnaire")
        List<Question> postQuestionnaire;
    }

    /**
     * Internal class for custom messages section.
     */
    private static class CustomMessages {
        @SerializedName("start_message")
        String startMessage;

        @SerializedName("finish_message")
        String finishMessage;

        @SerializedName("training_message")
        String trainingMessage;

        @SerializedName("pre_questionnaire_message")
        String preQuestionnaireMessage;

        @SerializedName("post_questionnaire_message")
        String postQuestionnaireMessage;
    }

    /**
     * Parses the JSON config file and populates all fields.
     */
    @Override
    protected void parse() {
        Gson gson = new Gson();
        JsonConfig config;

        try (FileReader reader = new FileReader(file)) {
            config = gson.fromJson(reader, JsonConfig.class);
        } catch (IOException e) {
            Log.e(TAG, "Error reading config file: " + filename, e);
            parseErrors.add(new ParseError(0, "Could not read file: " + e.getMessage()));
            return;
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "Error parsing JSON in config file: " + filename, e);
            parseErrors.add(new ParseError(0, "Invalid JSON syntax: " + e.getMessage()));
            return;
        }

        if (config == null) {
            parseErrors.add(new ParseError(0, "Empty or invalid JSON config file"));
            return;
        }

        // Parse method
        if (config.method != null) {
            int parsedMethod = parseMethodFromString(config.method);
            if (parsedMethod == Methods.UNDEFINED) {
                parseErrors.add(new ParseError(0,
                        "Unknown method \"" + config.method +
                        "\" (valid: ACR, CONTINUOUS, DSIS, TIME_CONTINUOUS)"));
            } else {
                method = parsedMethod;
            }
        }

        // Parse custom messages
        if (config.customMessages != null) {
            if (config.customMessages.startMessage != null) {
                startMessage = config.customMessages.startMessage;
            }
            if (config.customMessages.finishMessage != null) {
                finishMessage = config.customMessages.finishMessage;
            }
            if (config.customMessages.trainingMessage != null) {
                trainingMessage = config.customMessages.trainingMessage;
            }
            if (config.customMessages.preQuestionnaireMessage != null) {
                preQuestionnaireMessage = config.customMessages.preQuestionnaireMessage;
            }
            if (config.customMessages.postQuestionnaireMessage != null) {
                postQuestionnaireMessage = config.customMessages.postQuestionnaireMessage;
            }
        }

        // Parse playlist
        if (config.playlist == null || config.playlist.isEmpty()) {
            parseErrors.add(new ParseError(0, "Playlist is required and cannot be empty"));
        } else {
            parsePlaylist(config.playlist);
        }

        // Parse questionnaires
        if (config.preQuestionnaire != null && !config.preQuestionnaire.isEmpty()) {
            preQuestionnaire = new Questionnaire(config.preQuestionnaire);
            List<String> errors = preQuestionnaire.validate();
            for (String error : errors) {
                parseErrors.add(new ParseError(0, "pre_questionnaire: " + error));
            }
        }

        if (config.postQuestionnaire != null && !config.postQuestionnaire.isEmpty()) {
            postQuestionnaire = new Questionnaire(config.postQuestionnaire);
            List<String> errors = postQuestionnaire.validate();
            for (String error : errors) {
                parseErrors.add(new ParseError(0, "post_questionnaire: " + error));
            }
        }
    }

    /**
     * Parses a method string to method type constant.
     */
    private int parseMethodFromString(String methodStr) {
        if (methodStr == null) {
            return Methods.UNDEFINED;
        }
        switch (methodStr.toUpperCase()) {
            case "ACR":
                return Methods.TYPE_ACR_CATEGORICAL;
            case "CONTINUOUS":
                return Methods.TYPE_CONTINUOUS;
            case "DSIS":
                return Methods.TYPE_DSIS_CATEGORICAL;
            case "TIME_CONTINUOUS":
                return Methods.TYPE_TIME_CONTINUOUS;
            default:
                return Methods.UNDEFINED;
        }
    }

    /**
     * Parses the playlist entries, processing special markers and commands.
     */
    private void parsePlaylist(List<String> playlist) {
        boolean inTrainingSection = false;
        boolean hasTrainingStart = false;
        boolean hasTrainingEnd = false;

        for (int i = 0; i < playlist.size(); i++) {
            String entry = playlist.get(i).trim();

            if (entry.isEmpty()) {
                continue;
            }

            // Check for TRAINING_START marker
            if (entry.equalsIgnoreCase("TRAINING_START")) {
                if (hasTrainingStart) {
                    parseErrors.add(new ParseError(0,
                            "Duplicate TRAINING_START in playlist (item " + (i + 1) + ")"));
                }
                trainingStartIndex = entries.size();
                inTrainingSection = true;
                hasTrainingStart = true;
                continue;
            }

            // Check for TRAINING_END marker
            if (entry.equalsIgnoreCase("TRAINING_END")) {
                if (!hasTrainingStart) {
                    parseErrors.add(new ParseError(0,
                            "TRAINING_END without TRAINING_START (item " + (i + 1) + ")"));
                } else if (hasTrainingEnd) {
                    parseErrors.add(new ParseError(0,
                            "Duplicate TRAINING_END in playlist (item " + (i + 1) + ")"));
                }
                trainingEndIndex = entries.size() - 1;
                inTrainingSection = false;
                hasTrainingEnd = true;
                continue;
            }

            // Check for BREAK command
            if (Session.isBreakCommand(entry)) {
                String[] parts = entry.split("\\s+");
                if (parts.length >= 2) {
                    try {
                        int duration = Integer.parseInt(parts[1]);
                        if (duration < 0) {
                            parseErrors.add(new ParseError(0,
                                    "BREAK duration must be non-negative (item " + (i + 1) + ")"));
                        }
                    } catch (NumberFormatException e) {
                        parseErrors.add(new ParseError(0,
                                "BREAK duration \"" + parts[1] + "\" is not a valid number (item " + (i + 1) + ")"));
                    }
                }
                entries.add(entry);
                breakCount++;
                continue;
            }

            // Otherwise it's a video file entry
            entries.add(entry);
            if (inTrainingSection) {
                trainingVideoCount++;
            } else {
                videoCount++;
            }
        }

        // Validate TRAINING_START and TRAINING_END pairing
        if (hasTrainingStart && !hasTrainingEnd) {
            parseErrors.add(new ParseError(0, "TRAINING_START without matching TRAINING_END"));
        }
    }
}

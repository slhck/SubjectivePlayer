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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract base class for parsed config files.
 * Provides common fields and accessors for all config file formats.
 * Subclasses implement the actual parsing logic.
 */
public abstract class BaseConfigFile {

    protected final File file;
    protected final String filename;
    protected final String id;

    // Parsed content
    protected int method = Methods.UNDEFINED;
    protected String startMessage = null;
    protected String finishMessage = null;
    protected String trainingMessage = null;
    protected String preQuestionnaireMessage = null;
    protected String postQuestionnaireMessage = null;

    // Video entries (including BREAK commands)
    protected final List<String> entries = new ArrayList<>();

    // Training section indices (-1 if not defined)
    protected int trainingStartIndex = -1;
    protected int trainingEndIndex = -1;

    // Questionnaires (JSON format only, null for text format)
    protected Questionnaire preQuestionnaire = null;
    protected Questionnaire postQuestionnaire = null;

    // Parsing errors found
    protected final List<ParseError> parseErrors = new ArrayList<>();

    // Statistics
    protected int videoCount = 0;
    protected int trainingVideoCount = 0;
    protected int breakCount = 0;

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
     * Creates a config file wrapper.
     * Subclasses should call parse() in their constructor.
     * @param configFile The config file to parse
     */
    protected BaseConfigFile(File configFile) {
        this.file = configFile;
        this.filename = configFile.getName();
        this.id = extractIdFromFilename(filename);
    }

    /**
     * Parses the config file and populates all fields.
     * Must be implemented by subclasses.
     */
    protected abstract void parse();

    /**
     * Extracts the participant ID from a config filename.
     * Matches patterns like "subject_1.cfg", "playlist1.cfg", "1.cfg", "subject_1.json"
     */
    protected static String extractIdFromFilename(String filename) {
        Pattern pattern = Pattern.compile("(\\d+)");
        Matcher matcher = pattern.matcher(filename);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "-";
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

    public String getPreQuestionnaireMessage() {
        return preQuestionnaireMessage;
    }

    public String getPostQuestionnaireMessage() {
        return postQuestionnaireMessage;
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

    /**
     * Returns the pre-questionnaire, or null if not defined.
     * Only available for JSON format config files.
     */
    public Questionnaire getPreQuestionnaire() {
        return preQuestionnaire;
    }

    /**
     * Returns the post-questionnaire, or null if not defined.
     * Only available for JSON format config files.
     */
    public Questionnaire getPostQuestionnaire() {
        return postQuestionnaire;
    }

    /**
     * Returns true if a pre-questionnaire is defined.
     */
    public boolean hasPreQuestionnaire() {
        return preQuestionnaire != null && !preQuestionnaire.isEmpty();
    }

    /**
     * Returns true if a post-questionnaire is defined.
     */
    public boolean hasPostQuestionnaire() {
        return postQuestionnaire != null && !postQuestionnaire.isEmpty();
    }
}

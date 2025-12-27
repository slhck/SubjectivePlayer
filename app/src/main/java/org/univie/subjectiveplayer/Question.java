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

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Represents a single question in a questionnaire.
 * Used in pre_questionnaire and post_questionnaire sections of JSON config files.
 */
public class Question {

    /** Valid question types */
    public static final String TYPE_NUMBER = "number";
    public static final String TYPE_RADIO = "radio";
    public static final String TYPE_MULTIPLE_CHOICE = "multiple-choice";
    public static final String TYPE_TEXT = "text";

    /** The question text to display */
    @SerializedName("question")
    private String question;

    /** The type of question: "number", "radio", "multiple-choice", or "text" */
    @SerializedName("type")
    private String type;

    /** Options for radio or multiple-choice questions */
    @SerializedName("options")
    private List<String> options;

    /** Whether the question requires an answer (defaults to true) */
    @SerializedName("required")
    private Boolean required;

    /**
     * Default constructor for Gson
     */
    public Question() {
    }

    /**
     * Creates a question with all fields.
     */
    public Question(String question, String type, List<String> options, Boolean required) {
        this.question = question;
        this.type = type;
        this.options = options;
        this.required = required;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    /**
     * Returns whether this question is required.
     * Defaults to true if not specified.
     */
    public boolean isRequired() {
        return required == null || required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    /**
     * Validates this question and returns an error message if invalid.
     * @return null if valid, error message if invalid
     */
    public String validate() {
        if (question == null || question.trim().isEmpty()) {
            return "Question text is required";
        }
        if (type == null || type.trim().isEmpty()) {
            return "Question type is required";
        }
        if (!isValidType(type)) {
            return "Invalid question type \"" + type + "\" (valid: number, radio, multiple-choice, text)";
        }
        if ((TYPE_RADIO.equals(type) || TYPE_MULTIPLE_CHOICE.equals(type))
                && (options == null || options.isEmpty())) {
            return "Options are required for " + type + " questions";
        }
        return null;
    }

    /**
     * Checks if the given type is valid.
     */
    public static boolean isValidType(String type) {
        return TYPE_NUMBER.equals(type) ||
               TYPE_RADIO.equals(type) ||
               TYPE_MULTIPLE_CHOICE.equals(type) ||
               TYPE_TEXT.equals(type);
    }
}

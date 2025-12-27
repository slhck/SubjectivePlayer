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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents an answer to a questionnaire question, including timing information.
 * For multiple-choice questions, contains multiple selected options.
 */
public class QuestionnaireAnswer {

    /** The answer(s): single item for most types, multiple for multiple-choice */
    private final List<String> answers;

    /** Timestamp when this question was answered (Unix epoch milliseconds) */
    private final long answeredAtMillis;

    /** Time in seconds the user took to answer this question */
    private final double answerDurationSeconds;

    /**
     * Creates an answer with a single value.
     * Use this for number, radio, and text questions.
     */
    public QuestionnaireAnswer(String answer, long answeredAtMillis, double answerDurationSeconds) {
        this.answers = Collections.singletonList(answer != null ? answer : "");
        this.answeredAtMillis = answeredAtMillis;
        this.answerDurationSeconds = answerDurationSeconds;
    }

    /**
     * Creates an answer with multiple values.
     * Use this for multiple-choice questions.
     */
    public QuestionnaireAnswer(List<String> answers, long answeredAtMillis, double answerDurationSeconds) {
        this.answers = answers != null ? new ArrayList<>(answers) : new ArrayList<>();
        this.answeredAtMillis = answeredAtMillis;
        this.answerDurationSeconds = answerDurationSeconds;
    }

    /**
     * Returns the list of answers.
     * For single-value questions, this list has one element.
     * For multiple-choice questions, this list has one element per selected option.
     */
    public List<String> getAnswers() {
        return Collections.unmodifiableList(answers);
    }

    /**
     * Returns the first (or only) answer.
     * Convenience method for single-value questions.
     */
    public String getAnswer() {
        return answers.isEmpty() ? "" : answers.get(0);
    }

    /**
     * Returns the timestamp when this question was answered.
     */
    public long getAnsweredAtMillis() {
        return answeredAtMillis;
    }

    /**
     * Returns the time in seconds the user took to answer.
     */
    public double getAnswerDurationSeconds() {
        return answerDurationSeconds;
    }

    /**
     * Returns true if this answer has multiple selections (multiple-choice).
     */
    public boolean hasMultipleSelections() {
        return answers.size() > 1;
    }

    /**
     * Returns true if this answer is empty (no selections or empty string).
     */
    public boolean isEmpty() {
        return answers.isEmpty() || (answers.size() == 1 && answers.get(0).isEmpty());
    }
}

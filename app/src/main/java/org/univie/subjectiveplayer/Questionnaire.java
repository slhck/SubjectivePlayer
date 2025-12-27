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
 * Represents a questionnaire containing a list of questions.
 * Used for pre_questionnaire and post_questionnaire in JSON config files.
 */
public class Questionnaire {

    private final List<Question> questions;

    /**
     * Creates an empty questionnaire.
     */
    public Questionnaire() {
        this.questions = new ArrayList<>();
    }

    /**
     * Creates a questionnaire with the given questions.
     */
    public Questionnaire(List<Question> questions) {
        this.questions = questions != null ? new ArrayList<>(questions) : new ArrayList<>();
    }

    /**
     * Returns an unmodifiable view of the questions.
     */
    public List<Question> getQuestions() {
        return Collections.unmodifiableList(questions);
    }

    /**
     * Returns the number of questions.
     */
    public int size() {
        return questions.size();
    }

    /**
     * Returns true if the questionnaire has no questions.
     */
    public boolean isEmpty() {
        return questions.isEmpty();
    }

    /**
     * Validates all questions and returns a list of errors.
     * @return list of error messages (empty if all valid)
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            String error = q.validate();
            if (error != null) {
                errors.add("Question " + (i + 1) + ": " + error);
            }
        }
        return errors;
    }

    /**
     * Returns true if all questions are valid.
     */
    public boolean isValid() {
        return validate().isEmpty();
    }
}

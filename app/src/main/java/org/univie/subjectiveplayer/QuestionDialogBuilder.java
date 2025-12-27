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

import android.app.Dialog;
import android.content.Context;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder class for creating question dialogs.
 * Supports number, radio, multiple-choice, and text question types.
 * Renders each question in a landscape-friendly centered dialog.
 */
public class QuestionDialogBuilder {

    private static final String TAG = QuestionDialogBuilder.class.getSimpleName();

    public interface OnAnswerListener {
        /**
         * Called when the user submits an answer.
         * @param answer The answer string (comma-separated for multiple-choice)
         */
        void onAnswer(String answer);
    }

    private final Context context;
    private final Question question;
    private final int questionNumber;
    private final int totalQuestions;
    private OnAnswerListener listener;

    // UI elements for collecting answers
    private EditText numberInput;
    private EditText textInput;
    private RadioGroup radioGroup;
    private List<CheckBox> checkboxes;

    public QuestionDialogBuilder(Context context, Question question, int questionNumber, int totalQuestions) {
        this.context = context;
        this.question = question;
        this.questionNumber = questionNumber;
        this.totalQuestions = totalQuestions;
    }

    public QuestionDialogBuilder setOnAnswerListener(OnAnswerListener listener) {
        this.listener = listener;
        return this;
    }

    /**
     * Creates and returns the dialog.
     */
    public Dialog build() {
        Dialog dialog = new CustomDialog(context);
        dialog.setCancelable(false);

        // Create the main layout
        FrameLayout rootLayout = new FrameLayout(context);
        rootLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        rootLayout.setBackgroundColor(context.getResources().getColor(R.color.background_dark, null));

        // Create a ScrollView to allow scrolling when content is too tall
        ScrollView scrollView = new ScrollView(context);
        FrameLayout.LayoutParams scrollParams = new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        scrollView.setLayoutParams(scrollParams);
        scrollView.setFillViewport(true);

        // Create a wrapper to center the content vertically
        FrameLayout centerWrapper = new FrameLayout(context);
        centerWrapper.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        // Create centered content container
        LinearLayout contentLayout = new LinearLayout(context);
        FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        contentParams.gravity = Gravity.CENTER;
        contentLayout.setLayoutParams(contentParams);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setGravity(Gravity.CENTER);
        int padding = dpToPx(32);
        contentLayout.setPadding(padding, padding, padding, padding);
        contentLayout.setMinimumWidth(dpToPx(400));

        // Question counter (e.g., "Question 1 of 3")
        TextView counterView = new TextView(context);
        counterView.setText(context.getString(R.string.questionnaire_counter, questionNumber, totalQuestions));
        counterView.setTextColor(context.getResources().getColor(R.color.text_hint, null));
        counterView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        counterView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams counterParams = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        counterParams.bottomMargin = dpToPx(8);
        counterView.setLayoutParams(counterParams);
        contentLayout.addView(counterView);

        // Question text
        TextView questionView = new TextView(context);
        questionView.setText(question.getQuestion());
        questionView.setTextColor(context.getResources().getColor(R.color.white, null));
        questionView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        questionView.setGravity(Gravity.CENTER);
        questionView.setLineSpacing(0, 1.3f);
        LinearLayout.LayoutParams questionParams = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        questionParams.bottomMargin = dpToPx(24);
        questionView.setLayoutParams(questionParams);
        contentLayout.addView(questionView);

        // Required indicator
        if (!question.isRequired()) {
            TextView optionalView = new TextView(context);
            optionalView.setText(context.getString(R.string.questionnaire_optional));
            optionalView.setTextColor(context.getResources().getColor(R.color.text_hint, null));
            optionalView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            optionalView.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams optionalParams = new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            optionalParams.topMargin = dpToPx(-16);
            optionalParams.bottomMargin = dpToPx(24);
            optionalView.setLayoutParams(optionalParams);
            contentLayout.addView(optionalView);
        }

        // Input area based on question type (no nested ScrollView needed now)
        View inputView = createInputViewWithoutScroll();
        if (inputView != null) {
            contentLayout.addView(inputView);
        }

        // Submit button - white text on gray background (matches all other dialogs)
        Button submitButton = new Button(context);
        submitButton.setText(R.string.questionnaire_next);
        submitButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        submitButton.setTextColor(context.getResources().getColor(R.color.white, null));
        submitButton.setTypeface(submitButton.getTypeface(), android.graphics.Typeface.BOLD);
        submitButton.setBackgroundResource(R.drawable.submit_button_background);
        submitButton.setAllCaps(false);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                dpToPx(120), dpToPx(48));
        buttonParams.topMargin = dpToPx(24);
        buttonParams.gravity = Gravity.CENTER;
        submitButton.setLayoutParams(buttonParams);

        submitButton.setOnClickListener(v -> {
            String answer = collectAnswer();
            if (answer == null && question.isRequired()) {
                // Show validation error - answer required
                Log.w(TAG, "Required question not answered");
                return;
            }
            if (listener != null) {
                listener.onAnswer(answer != null ? answer : "");
            }
            dialog.dismiss();
        });

        contentLayout.addView(submitButton);
        centerWrapper.addView(contentLayout);
        scrollView.addView(centerWrapper);
        rootLayout.addView(scrollView);
        dialog.setContentView(rootLayout);
        dialog.getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        return dialog;
    }

    /**
     * Creates the appropriate input view based on question type (without nested ScrollView).
     */
    private View createInputViewWithoutScroll() {
        switch (question.getType()) {
            case Question.TYPE_NUMBER:
                return createNumberInput();
            case Question.TYPE_TEXT:
                return createTextInput();
            case Question.TYPE_RADIO:
                return createRadioInputWithoutScroll();
            case Question.TYPE_MULTIPLE_CHOICE:
                return createMultipleChoiceInputWithoutScroll();
            default:
                Log.e(TAG, "Unknown question type: " + question.getType());
                return null;
        }
    }

    /**
     * Creates radio buttons without a nested ScrollView.
     */
    private View createRadioInputWithoutScroll() {
        List<String> options = question.getOptions();
        if (options == null || options.isEmpty()) {
            return null;
        }

        radioGroup = new RadioGroup(context);
        radioGroup.setOrientation(RadioGroup.VERTICAL);
        radioGroup.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rgParams = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        rgParams.gravity = Gravity.CENTER;
        radioGroup.setLayoutParams(rgParams);

        for (int i = 0; i < options.size(); i++) {
            RadioButton radioButton = new RadioButton(context);
            radioButton.setText(options.get(i));
            radioButton.setTextColor(context.getResources().getColor(R.color.white, null));
            radioButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            radioButton.setButtonTintList(android.content.res.ColorStateList.valueOf(
                    context.getResources().getColor(R.color.white, null)));
            radioButton.setId(View.generateViewId());
            RadioGroup.LayoutParams rbParams = new RadioGroup.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            rbParams.bottomMargin = dpToPx(8);
            radioButton.setLayoutParams(rbParams);
            radioButton.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
            radioGroup.addView(radioButton);
        }

        return radioGroup;
    }

    /**
     * Creates checkboxes without a nested ScrollView.
     */
    private View createMultipleChoiceInputWithoutScroll() {
        List<String> options = question.getOptions();
        if (options == null || options.isEmpty()) {
            return null;
        }

        checkboxes = new ArrayList<>();

        LinearLayout checkboxLayout = new LinearLayout(context);
        checkboxLayout.setOrientation(LinearLayout.VERTICAL);
        checkboxLayout.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams clParams = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        clParams.gravity = Gravity.CENTER;
        checkboxLayout.setLayoutParams(clParams);

        for (int i = 0; i < options.size(); i++) {
            CheckBox checkBox = new CheckBox(context);
            checkBox.setText(options.get(i));
            checkBox.setTextColor(context.getResources().getColor(R.color.white, null));
            checkBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            checkBox.setButtonTintList(android.content.res.ColorStateList.valueOf(
                    context.getResources().getColor(R.color.white, null)));
            LinearLayout.LayoutParams cbParams = new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            cbParams.bottomMargin = dpToPx(8);
            checkBox.setLayoutParams(cbParams);
            checkBox.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
            checkboxes.add(checkBox);
            checkboxLayout.addView(checkBox);
        }

        return checkboxLayout;
    }

    /**
     * Creates a number input field.
     */
    private View createNumberInput() {
        numberInput = new EditText(context);
        numberInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        numberInput.setTextColor(context.getResources().getColor(R.color.white, null));
        numberInput.setHintTextColor(context.getResources().getColor(R.color.text_hint, null));
        numberInput.setHint(R.string.questionnaire_number_hint);
        numberInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        numberInput.setGravity(Gravity.CENTER);
        numberInput.setBackgroundResource(R.drawable.edit_text_background);
        numberInput.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(200), LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        numberInput.setLayoutParams(params);
        return numberInput;
    }

    /**
     * Creates a text input field.
     */
    private View createTextInput() {
        textInput = new EditText(context);
        textInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        textInput.setTextColor(context.getResources().getColor(R.color.white, null));
        textInput.setHintTextColor(context.getResources().getColor(R.color.text_hint, null));
        textInput.setHint(R.string.questionnaire_text_hint);
        textInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        textInput.setMinLines(3);
        textInput.setMaxLines(5);
        textInput.setGravity(Gravity.START | Gravity.TOP);
        textInput.setBackgroundResource(R.drawable.edit_text_background);
        textInput.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(400), LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        textInput.setLayoutParams(params);
        return textInput;
    }

    /**
     * Collects the answer from the input view.
     * @return The answer string, or null if no answer provided
     */
    private String collectAnswer() {
        switch (question.getType()) {
            case Question.TYPE_NUMBER:
                if (numberInput != null) {
                    String text = numberInput.getText().toString().trim();
                    return text.isEmpty() ? null : text;
                }
                break;
            case Question.TYPE_TEXT:
                if (textInput != null) {
                    String text = textInput.getText().toString().trim();
                    return text.isEmpty() ? null : text;
                }
                break;
            case Question.TYPE_RADIO:
                if (radioGroup != null) {
                    int checkedId = radioGroup.getCheckedRadioButtonId();
                    if (checkedId != -1) {
                        RadioButton selected = radioGroup.findViewById(checkedId);
                        if (selected != null) {
                            return selected.getText().toString();
                        }
                    }
                }
                return null;
            case Question.TYPE_MULTIPLE_CHOICE:
                if (checkboxes != null) {
                    List<String> selected = new ArrayList<>();
                    for (CheckBox cb : checkboxes) {
                        if (cb.isChecked()) {
                            selected.add(cb.getText().toString());
                        }
                    }
                    if (!selected.isEmpty()) {
                        return String.join("; ", selected);
                    }
                }
                return null;
        }
        return null;
    }

    /**
     * Converts dp to pixels.
     */
    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}

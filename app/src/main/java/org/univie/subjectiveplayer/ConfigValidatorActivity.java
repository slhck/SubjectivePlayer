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

import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigValidatorActivity extends AppCompatActivity {

    private static final String TAG = ConfigValidatorActivity.class.getSimpleName();

    private ProgressBar mProgressBar;
    private TextView mStatusText;
    private TextView mResultText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_config_validator);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        View mainLayout = findViewById(R.id.main_layout);
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        mProgressBar = findViewById(R.id.progress_bar);
        mStatusText = findViewById(R.id.status_text);
        mResultText = findViewById(R.id.result_text);

        runValidation();
    }

    private void runValidation() {
        mProgressBar.setVisibility(View.VISIBLE);
        mStatusText.setVisibility(View.VISIBLE);
        mStatusText.setText(R.string.validate_running);

        new Thread(() -> {
            List<String> errors = validateConfigFiles();
            runOnUiThread(() -> displayResults(errors));
        }).start();
    }

    private List<String> validateConfigFiles() {
        List<String> errors = new ArrayList<>();

        File configFolder = Configuration.sFolderApproot;
        if (configFolder == null || !configFolder.exists()) {
            errors.add("Config folder does not exist");
            return errors;
        }

        File[] configFiles = configFolder.listFiles((dir, name) -> name.endsWith(".cfg"));
        if (configFiles == null || configFiles.length == 0) {
            return errors;
        }

        Map<String, List<String>> videoToConfigFiles = new HashMap<>();

        for (File configFile : configFiles) {
            Log.d(TAG, "Validating config file: " + configFile.getName());
            validateSingleConfigFile(configFile, errors, videoToConfigFiles);
        }

        checkMissingVideos(videoToConfigFiles, errors);

        return errors;
    }

    private void validateSingleConfigFile(File configFile, List<String> errors,
                                          Map<String, List<String>> videoToConfigFiles) {
        try (FileInputStream fis = new FileInputStream(configFile);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {

            String line;
            int lineNumber = 0;
            boolean firstNonEmptyLine = true;
            boolean hasTrainingStart = false;
            boolean hasTrainingEnd = false;
            int trainingStartLine = -1;
            int trainingEndLine = -1;

            while ((line = br.readLine()) != null) {
                lineNumber++;
                String trimmedLine = line.trim();

                if (trimmedLine.isEmpty()) {
                    continue;
                }

                if (firstNonEmptyLine && Session.isMethodDirective(trimmedLine)) {
                    int method = Session.parseMethodType(trimmedLine);
                    if (method == Methods.UNDEFINED) {
                        String[] parts = trimmedLine.split("\\s+");
                        String methodName = parts.length >= 2 ? parts[1] : "(empty)";
                        errors.add("Config file \"" + configFile.getName() +
                                "\" has invalid syntax at line " + lineNumber +
                                ": Unknown METHOD \"" + methodName +
                                "\" (valid: ACR, CONTINUOUS, DSIS, CONTINUOUS_RATING)");
                    }
                    firstNonEmptyLine = false;
                    continue;
                }

                firstNonEmptyLine = false;

                // Check for TRAINING_START marker
                if (Session.isTrainingStartMarker(trimmedLine)) {
                    hasTrainingStart = true;
                    trainingStartLine = lineNumber;
                    continue;
                }

                // Check for TRAINING_END marker
                if (Session.isTrainingEndMarker(trimmedLine)) {
                    hasTrainingEnd = true;
                    trainingEndLine = lineNumber;
                    continue;
                }

                // Skip other directive lines (START_MESSAGE, FINISH_MESSAGE, TRAINING_MESSAGE)
                if (Session.isStartMessageDirective(trimmedLine) ||
                    Session.isFinishMessageDirective(trimmedLine) ||
                    Session.isTrainingMessageDirective(trimmedLine)) {
                    continue;
                }

                if (Session.isBreakCommand(trimmedLine)) {
                    String[] parts = trimmedLine.split("\\s+");
                    if (parts.length >= 2) {
                        try {
                            int duration = Integer.parseInt(parts[1]);
                            if (duration < 0) {
                                errors.add("Config file \"" + configFile.getName() +
                                        "\" has invalid syntax at line " + lineNumber +
                                        ": BREAK duration must be non-negative");
                            }
                        } catch (NumberFormatException e) {
                            errors.add("Config file \"" + configFile.getName() +
                                    "\" has invalid syntax at line " + lineNumber +
                                    ": BREAK duration \"" + parts[1] + "\" is not a valid number");
                        }
                    }
                    continue;
                }

                videoToConfigFiles.computeIfAbsent(trimmedLine, k -> new ArrayList<>())
                        .add(configFile.getName());
            }

            // Validate TRAINING_START and TRAINING_END pairing
            if (hasTrainingStart && !hasTrainingEnd) {
                errors.add("Config file \"" + configFile.getName() +
                        "\" has TRAINING_START at line " + trainingStartLine +
                        " but is missing TRAINING_END");
            } else if (!hasTrainingStart && hasTrainingEnd) {
                errors.add("Config file \"" + configFile.getName() +
                        "\" has TRAINING_END at line " + trainingEndLine +
                        " but is missing TRAINING_START");
            } else if (hasTrainingStart && hasTrainingEnd && trainingEndLine <= trainingStartLine) {
                errors.add("Config file \"" + configFile.getName() +
                        "\": TRAINING_END (line " + trainingEndLine +
                        ") must come after TRAINING_START (line " + trainingStartLine + ")");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error reading config file: " + configFile.getName(), e);
            errors.add("Config file \"" + configFile.getName() +
                    "\" could not be read: " + e.getMessage());
        }
    }

    private void checkMissingVideos(Map<String, List<String>> videoToConfigFiles,
                                    List<String> errors) {
        File videosFolder = Configuration.sFolderVideos;
        if (videosFolder == null || !videosFolder.exists()) {
            if (!videoToConfigFiles.isEmpty()) {
                errors.add("Videos folder does not exist but config files reference videos");
            }
            return;
        }

        for (Map.Entry<String, List<String>> entry : videoToConfigFiles.entrySet()) {
            String videoName = entry.getKey();
            List<String> configFileNames = entry.getValue();

            File videoFile = new File(videosFolder, videoName);
            if (!videoFile.exists()) {
                String configFilesStr;
                if (configFileNames.size() == 1) {
                    configFilesStr = "config file \"" + configFileNames.get(0) + "\"";
                } else {
                    StringBuilder sb = new StringBuilder("config files ");
                    for (int i = 0; i < configFileNames.size(); i++) {
                        if (i > 0) {
                            sb.append(", ");
                        }
                        sb.append("\"").append(configFileNames.get(i)).append("\"");
                    }
                    configFilesStr = sb.toString();
                }
                errors.add("Video \"" + videoName + "\" not found, but specified in " + configFilesStr);
            }
        }
    }

    private void displayResults(List<String> errors) {
        mProgressBar.setVisibility(View.GONE);
        mStatusText.setVisibility(View.GONE);

        File configFolder = Configuration.sFolderApproot;
        File[] configFiles = configFolder != null ?
                configFolder.listFiles((dir, name) -> name.endsWith(".cfg")) : null;

        if (configFiles == null || configFiles.length == 0) {
            mResultText.setText(R.string.validate_no_config_files);
            mResultText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            return;
        }

        if (errors.isEmpty()) {
            mResultText.setText(R.string.validate_success);
            mResultText.setTextColor(ContextCompat.getColor(this, R.color.success));
        } else {
            SpannableStringBuilder builder = new SpannableStringBuilder();

            String header = getString(R.string.validate_errors_found, errors.size());
            builder.append(header);
            builder.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.error)),
                    0, header.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            builder.append("\n\n");

            for (int i = 0; i < errors.size(); i++) {
                String error = errors.get(i);
                int start = builder.length();
                builder.append("\u2022 ").append(error);
                builder.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.white)),
                        start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                if (i < errors.size() - 1) {
                    builder.append("\n\n");
                }
            }

            mResultText.setText(builder);
        }
    }
}

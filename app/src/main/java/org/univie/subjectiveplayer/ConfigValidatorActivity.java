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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigValidatorActivity extends AppCompatActivity {

    private static final String TAG = ConfigValidatorActivity.class.getSimpleName();

    private ProgressBar mProgressBar;
    private TextView mStatusText;
    private TextView mResultText;
    private List<ConfigFile> mConfigFiles = new ArrayList<>();

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
        mConfigFiles.clear();

        File configFolder = Configuration.sFolderApproot;
        if (configFolder == null || !configFolder.exists()) {
            errors.add("Config folder does not exist");
            return errors;
        }

        File[] files = configFolder.listFiles((dir, name) -> name.endsWith(".cfg"));
        if (files == null || files.length == 0) {
            return errors;
        }

        // Parse all config files
        for (File file : files) {
            Log.d(TAG, "Parsing config file: " + file.getName());
            mConfigFiles.add(new ConfigFile(file));
        }

        // Sort by ID (numerically if possible)
        mConfigFiles.sort((a, b) -> {
            try {
                int idA = Integer.parseInt(a.getId());
                int idB = Integer.parseInt(b.getId());
                return Integer.compare(idA, idB);
            } catch (NumberFormatException e) {
                return a.getId().compareTo(b.getId());
            }
        });

        // Collect parse errors from each config file
        for (ConfigFile config : mConfigFiles) {
            for (ConfigFile.ParseError parseError : config.getParseErrors()) {
                String errorMsg = "Config file \"" + config.getFilename() + "\"";
                if (parseError.lineNumber > 0) {
                    errorMsg += " at line " + parseError.lineNumber;
                }
                errorMsg += ": " + parseError.message;
                errors.add(errorMsg);
            }
        }

        // Check for missing videos
        checkMissingVideos(errors);

        return errors;
    }

    private void checkMissingVideos(List<String> errors) {
        File videosFolder = Configuration.sFolderVideos;

        // Build map of video -> config files that reference it
        Map<String, List<String>> videoToConfigFiles = new HashMap<>();
        for (ConfigFile config : mConfigFiles) {
            for (String videoName : config.getVideoFilenames()) {
                videoToConfigFiles.computeIfAbsent(videoName, k -> new ArrayList<>())
                        .add(config.getFilename());
            }
        }

        if (videoToConfigFiles.isEmpty()) {
            return;
        }

        if (videosFolder == null || !videosFolder.exists()) {
            errors.add("Videos folder does not exist but config files reference videos");
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

        if (mConfigFiles.isEmpty()) {
            mResultText.setText(R.string.validate_no_config_files);
            mResultText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            return;
        }

        SpannableStringBuilder builder = new SpannableStringBuilder();

        // Add validation result section first
        if (errors.isEmpty()) {
            int successStart = builder.length();
            builder.append(getString(R.string.validate_success));
            builder.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.success)),
                    successStart, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            String errorHeader = getString(R.string.validate_errors_found, errors.size());
            int errorStart = builder.length();
            builder.append(errorHeader);
            builder.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.error)),
                    errorStart, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

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
        }

        builder.append("\n\n");

        // Add config files summary section
        String summaryHeader = getString(R.string.validate_config_files_header, mConfigFiles.size());
        int headerStart = builder.length();
        builder.append(summaryHeader);
        builder.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.white)),
                headerStart, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.append("\n\n");

        for (ConfigFile config : mConfigFiles) {
            int lineStart = builder.length();
            // Line 1: ID - filename - video count
            builder.append(config.getId())
                    .append(" - ")
                    .append(config.getFilename())
                    .append(" - ")
                    .append(getString(R.string.validate_video_count, config.getTotalVideoCount()));
            builder.append("\n");

            // Line 2: Method - breaks - training
            String methodName = config.getMethodName();
            if (methodName == null) {
                methodName = getString(R.string.validate_method_undefined);
            }

            builder.append("    ")
                    .append(methodName)
                    .append(" - ");

            if (config.getBreakCount() > 0) {
                builder.append(getString(R.string.validate_break_count, config.getBreakCount()));
            } else {
                builder.append(getString(R.string.validate_no_breaks));
            }

            if (config.hasTrainingSection()) {
                builder.append(" - ")
                        .append(getString(R.string.validate_training_count, config.getTrainingVideoCount()));
            }

            builder.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.text_secondary)),
                    lineStart, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.append("\n\n");
        }

        mResultText.setText(builder);
    }
}

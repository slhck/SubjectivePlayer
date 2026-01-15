# Config Files

Each subject who will take part in the test must have their own config file specifying the videos to be shown and rated ("playlist") and other settings.

The app supports two config formats:

- **JSON format (recommended):** `subject_<ID>.json` -- Structured JSON with questionnaire support
- **Legacy text format:** `subject_<ID>.cfg` -- Simple text with one entry per line

For example, for subject ID `1`, the config file should be named `subject_1.json` (preferred) or `subject_1.cfg`. If both exist, the JSON file takes precedence.

To generate multiple configs for multiple subjects, you can look at the `create_config_files.py` script.

## JSON Format

The JSON format is the preferred format for new configurations. It supports all features of the legacy format plus questionnaires for collecting participant data.

A JSON schema is available at [`json-schema/subject-config.schema.json`](https://github.com/slhck/SubjectivePlayer/blob/master/json-schema/subject-config.schema.json).

### Example

```json
{
  "method": "ACR",
  "custom_messages": {
    "start_message": "Welcome to the test!\nPlease watch each video and rate its quality.",
    "finish_message": "Thank you for participating!",
    "training_message": "This is a training session.",
    "pre_questionnaire_message": "Please answer a few questions before we begin.",
    "post_questionnaire_message": "Please provide some feedback on your experience."
  },
  "playlist": [
    "TRAINING_START",
    "training_video1.mp4",
    "training_video2.mp4",
    "TRAINING_END",
    "video1.mp4",
    "video2.mp4",
    "BREAK 30",
    "video3.mp4"
  ],
  "pre_questionnaire": [
    {"question": "How old are you?", "type": "number"},
    {"question": "What is your gender?", "type": "radio", "options": ["Male", "Female", "Other", "Prefer not to say"]}
  ],
  "post_questionnaire": [
    {"question": "Any comments?", "type": "text", "required": false}
  ]
}
```

### Main Fields

| Field                | Required | Description                                                |
| -------------------- | -------- | ---------------------------------------------------------- |
| `method`             | No       | Rating method (see below). Defaults to `ACR`.              |
| `custom_messages`    | No       | Custom messages shown during the test (see below).         |
| `playlist`           | Yes      | Array of video filenames and special commands (see below). |
| `pre_questionnaire`  | No       | Array of questions to show before the test                 |
| `post_questionnaire` | No       | Array of questions to show after the test                  |

### Rating Methods

The `method` field specifies how subjects will rate videos:

- `ACR` (default) -- 5-point quality scale (Excellent/Good/Fair/Poor/Bad) shown after each video, following ITU-T Rec. P.910
- `CONTINUOUS` -- Slider-based rating (0-100) shown after each video
- `DSIS` -- Impairment scale (not fully implemented)
- `TIME_CONTINUOUS` -- Real-time rating during playback using volume buttons (experimental)

### Custom Messages

With `custom_messages`, you can customize the messages shown at different points in the test. All fields are optional and support `\n` for line breaks:

- `start_message` -- Shown before the first video (or before training if training is defined). The subject must click Continue to proceed.
- `finish_message` -- Shown after the last rating is submitted. The subject clicks OK to finish.
- `training_message` -- Shown on the training introduction screen before the first training video (only if training section is defined).
- `pre_questionnaire_message` -- Introductory message shown before the pre-questionnaire (only if pre-questionnaire is defined).
- `post_questionnaire_message` -- Introductory message shown before the post-questionnaire (only if post-questionnaire is defined).

### Playlist Commands

The `playlist` array contains video filenames and special commands:

**Video filename** (e.g., `"video1.mp4"`)

A video file in the `SubjectiveMovies/` folder. The file must exist!

**Training Section** -- `TRAINING_START` and `TRAINING_END`

- Defines a training section to familiarize subjects with the rating procedure
- `TRAINING_START` marks the beginning; `TRAINING_END` marks the end
- Videos between these markers are training videos (rated but not included in final results)
- A training introduction screen (with optional custom `training_message`) is shown before the first training video
- A training complete screen is shown after the last training video, before the main test begins
- If you use `TRAINING_START`, you must also include `TRAINING_END`, and vice versa

**Breaks** -- `BREAK` or `BREAK <seconds>`

- Inserts a break between videos to prevent viewer fatigue
- `BREAK` (no number) -- Shows a message telling the subject to wait for the test supervisor. The subject must click Continue.
- `BREAK 30` (with number) -- A timed break of the specified duration in seconds, with a countdown timer. The Continue button is enabled when the timer reaches zero.

### Questionnaires

You can define questionnaires (`pre_questionnaire` and `post_questionnaire`). These are arrays of question objects shown before and after the test, respectively.

Each question is an object with the following fields:

| Field      | Required                  | Description                                                     |
| ---------- | ------------------------- | --------------------------------------------------------------- |
| `question` | Yes                       | The question text.                                              |
| `type`     | Yes                       | Question type: `number`, `radio`, `multiple-choice`, or `text`. |
| `options`  | For radio/multiple-choice | Array of answer options (strings).                              |
| `required` | No                        | Whether an answer is required. Defaults to `true`.              |

## Legacy CFG Format

The legacy text format uses video filename or special directive per line. This format is supported for backward compatibility; the JSON format is recommended for new configurations.

### Format

Each line contains either a video filename (e.g., `video1.mp4`) or a directive:

- `METHOD <type>` -- Sets the rating method. See [Rating Methods](#rating-methods) above. Must be on the first line if used. Defaults to `ACR`.
- `START_MESSAGE <message>` -- Custom start message. Use `\n` for line breaks.
- `FINISH_MESSAGE <message>` -- Custom finish message.
- `TRAINING_MESSAGE <message>` -- Custom training message.
- `TRAINING_START` and `TRAINING_END` -- Define a training section.
- `BREAK` or `BREAK <seconds>` -- Insert a break.

### Example

```
METHOD ACR
START_MESSAGE Welcome to the test!\nPlease watch the following videos and rate their quality.
TRAINING_MESSAGE This is the training section.\nPlease get familiar with the rating procedure.
TRAINING_START
training_video1.mp4
training_video2.mp4
TRAINING_END
video1.mp4
video2.mp4
video3.mp4
BREAK 30
video4.mp4
video5.mp4
video6.mp4
BREAK 30
video7.mp4
video8.mp4
FINISH_MESSAGE Thank you for participating!\nPlease inform the test supervisor.
```

This example shows all major features: a custom start message, training section with custom message, main test videos with timed breaks, and a custom finish message. For detailed explanations of each feature, see the [JSON Format](#json-format) section above.

## Generating Config Files with Python

For studies with multiple participants, manually creating config files is tedious and error-prone. The `create_config_files.py` script automates this process by generating randomized playlists for each subject.

The script is meant to be adapted for your study. Copy it, then edit the relevant sections before running.

### Step 1: Configure Training Videos

Open the script and edit the `TRAINING_SET` list at the top. These videos will appear in a training section at the start of each test session (same order for all subjects):

```python
TRAINING_SET = [
    "training_video1.mp4",
    "training_video2.mp4",
]
```

Set `TRAINING_SET = []` if you don't need a training section.

### Step 2: Add Custom Messages and Questionnaires (Optional)

The default script generates minimal configs with only `method` and `playlist`. To include [custom messages](#custom-messages) or [questionnaires](#questionnaires), find the `config` dictionary near the end of the script and extend it:

```python
config = {
    "method": args.method,
    "playlist": playlist,
    "custom_messages": {
        "start_message": "Welcome to the study!",
        "finish_message": "Thank you for participating!"
    },
    "pre_questionnaire": [
        {"question": "What is your age?", "type": "number"},
        {"question": "How often do you watch streaming video?", "type": "radio",
         "options": ["Daily", "Weekly", "Monthly", "Rarely"]}
    ]
}
```

### Step 3: Run the Script

```bash
./create_config_files.py -i /path/to/videos -o /path/to/output -n 30 -m ACR
```

| Option           | Required | Description                                                                       |
| ---------------- | -------- | --------------------------------------------------------------------------------- |
| `-i`, `--input`  | Yes      | Path to directory containing video files (`.mp4`)                                 |
| `-o`, `--output` | Yes      | Path to output directory for config files                                         |
| `-n`, `--number` | No       | Number of subjects (default: 30)                                                  |
| `-m`, `--method` | No       | Rating method: `ACR`, `CONTINUOUS`, `DSIS`, or `TIME_CONTINUOUS` (default: `ACR`) |
| `-p`, `--primes` | No       | Use prime numbers as subject IDs instead of sequential                            |
| `--prime-min`    | No       | Minimum value for prime IDs (default: 1000)                                       |
| `--prime-max`    | No       | Maximum value for prime IDs (default: 9999)                                       |
| `-s`, `--seed`   | No       | Random seed for reproducible output                                               |

This generates config files named `subject_<id>.json` for each subject. The randomization ensures that video order differs between subjects, except for the training section, which is the same for all.

!!! tip "Tip: Use Prime-Based Subject IDs"

    By default, subjects get sequential IDs (1, 2, 3, ...). With `-p/--primes`, the script uses randomly selected prime numbers as IDs (e.g., 2411, 3863, 7919):

    ```bash
    ./create_config_files.py -i /path/to/videos -o ./configs -n 30 -p
    ```

    This prevents participants from accidentally entering another participant's ID. Prime numbers guarantee uniqueness, and shuffling makes IDs unpredictable.

    You can adjust the ID range with `--prime-min` and `--prime-max`:

    ```bash
    # Use 3-digit primes (101-997)
    ./create_config_files.py -i ./videos -o ./configs -n 20 -p --prime-min 100 --prime-max 999
    ```

    Use `-s/--seed` for reproducible results (same IDs on repeated runs):

    ```bash
    ./create_config_files.py -i ./videos -o ./configs -n 30 -p -s 42
    ```

### Step 4: Deploy to Device

!!! warning "Start the app once!"

    Remember that if the app has not run before, you need to start the app once to create the config folder!

You then need to copy the generated config files to the device's `SubjectiveCfg/` folder:

```bash
adb push /path/to/output/*.json /storage/emulated/0/Android/data/org.univie.subjectiveplayer/files/SubjectiveCfg/
```

### Step 5: Validate the Config Files

Open the app, and then [validate the config files](running-tests.md#validate-config-files).

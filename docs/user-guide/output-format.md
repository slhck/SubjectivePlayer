# Output Format

Results are written as CSV files.

## Rating Output

For each subject there will be one rating output file. It is named as follows:

```
<participant_id>_<datetime>_<method>.csv
```

For example: `1_20250115-143055_ACR.csv`

### Columns

The CSV file contains one line per video rated, plus additional lines for breaks:

| Column            | Description                                                                                           |
| ----------------- | ----------------------------------------------------------------------------------------------------- |
| `video_position`  | Zero-based index of the video in the playlist. `-1` for BREAK entries.                                |
| `video_name`      | Filename of the video (e.g., `video1.mp4`). `BREAK` for break entries.                                |
| `rating`          | Integer rating value. For ACR: 0=Excellent, 1=Good, 2=Fair, 3=Poor, 4=Bad. For continuous: 0-100.     |
| `rated_at`        | ISO8601 timestamp when the rating was submitted (e.g., `2025-01-15T14:32:05+01:00`).                  |
| `rating_duration` | Time in seconds the subject took to submit the rating (e.g., `2.345`). Empty for time-continuous ratings. |

### Notes

- The `rating`, `rated_at`, and `rating_duration` columns are empty for BREAK entries.
- For time-continuous ratings, there will be one line per second of video playback, with the `rating` column containing the current rating value at that second, and the `rated_at` column indicating when that rating was recorded.
- The `rating_duration` column can be used to detect issues with single votes caused by observers struggling to decide on a rating. Note: the `rating_duration` column is empty for time-continuous ratings since ratings are logged automatically during playback rather than after subject interaction.

!!! info "Reference"
    For more information on using rating duration for quality assessment, see: [Robitza, W., and Hlavacs, H. (2014). Assessing the validity of subjective QoE data through rating times and self-reported confidence. In *2014 Sixth International Workshop on Quality of Multimedia Experience (QoMEX)* (pp. 297-302). Singapore: IEEE.](https://ieeexplore.ieee.org/document/6982335/)

## Questionnaire Output

If pre- or post-questionnaires are defined in the JSON config, answers are logged to separate CSV files:

```
<participant_id>_<datetime>_questionnaire_<type>.csv
```

For example: `1_20250115-143050_questionnaire_pre.csv`

### Columns

The CSV uses a [tidy data](https://vita.had.co.nz/papers/tidy-data.pdf) format with one row per answer:

| Column            | Description                                                                          |
| ----------------- | ------------------------------------------------------------------------------------ |
| `question_number` | 1-based index of the question.                                                       |
| `question_type`   | Type of question: `number`, `radio`, `multiple-choice`, or `text`.                   |
| `question`        | The question text.                                                                   |
| `answer`          | The answer value. For multiple-choice, each selected option produces a separate row. |
| `answered_at`     | ISO8601 timestamp when the question was answered.                                    |
| `answer_duration` | Time in seconds the subject took to answer.                                          |

### Notes

For multiple-choice questions, each selected option is written as a separate row with the same `question_number`, `answered_at`, and `answer_duration`. This tidy format makes analysis easier.

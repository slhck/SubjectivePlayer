#!/usr/bin/env python
# Generate config files for SubjectivePlayer
# Author: Werner Robitza
#
# This script generates subject_<id>.json config files for SubjectivePlayer.
# Each subject gets a randomized playlist of test videos, with an optional
# training section at the beginning.
#
# Usage:
#   ./create_config_files.py -i /path/to/videos -o /path/to/output -n 30 -m ACR
#   ./create_config_files.py -i /path/to/videos -o /path/to/output -n 30 -p  # use prime IDs
#
# The output files can be pushed to the device's SubjectiveCfg/ folder.

import argparse
import glob
import json
import os
import random


def generate_primes(start: int, end: int) -> list[int]:
    """Generate all prime numbers in a range using Sieve of Eratosthenes."""
    if end < 2:
        return []
    sieve = [True] * (end + 1)
    sieve[0] = sieve[1] = False
    for i in range(2, int(end**0.5) + 1):
        if sieve[i]:
            for j in range(i * i, end + 1, i):
                sieve[j] = False
    return [i for i in range(max(2, start), end + 1) if sieve[i]]

# Define training video filenames here. These videos will be shown at the
# beginning of each test session in a dedicated training section.
# Training videos are used to familiarize subjects with the rating procedure
# before the actual test begins. Their ratings are not included in final results.
# Set to an empty list [] if no training is needed.
TRAINING_SET = [
    "01_tr03_SRC200_HRC03.mp4",
    "02_tr03_SRC414_HRC61.mp4",
    "03_tr03_SRC118_HRC68.mp4",
    "04_tr03_SRC115_HRC76.mp4",
    "05_tr03_SRC305_HRC69.mp4",
    "06_tr03_SRC218_HRC02.mp4",
    "07_TR00_SRC00_HRC01.mp4",
]

parser = argparse.ArgumentParser(
    description="""Generate config files for SubjectivePlayer in JSON format"""
)
parser.add_argument(
    "-i", "--input", type=str, required=True, help="""Path to video files (PVS)"""
)
parser.add_argument(
    "-o",
    "--output",
    type=str,
    required=True,
    help="""Path to output directory""",
)
parser.add_argument(
    "-n", "--number", type=int, default=30, help="""Number of subjects"""
)
parser.add_argument(
    "-m",
    "--method",
    type=str,
    default="ACR",
    choices=["ACR", "CONTINUOUS", "DSIS", "TIME_CONTINUOUS"],
    help="Rating method (default: ACR)",
)
parser.add_argument(
    "-p",
    "--primes",
    action="store_true",
    help="Use prime numbers as subject IDs (e.g., 2411, 3863) instead of sequential (1, 2, 3)",
)
parser.add_argument(
    "--prime-min",
    type=int,
    default=1000,
    help="Minimum value for prime subject IDs (default: 1000)",
)
parser.add_argument(
    "--prime-max",
    type=int,
    default=9999,
    help="Maximum value for prime subject IDs (default: 9999)",
)
parser.add_argument(
    "-s",
    "--seed",
    type=int,
    default=None,
    help="Random seed for reproducible prime selection and playlist shuffling",
)

args = parser.parse_args()

# Set random seed if provided (for reproducible results)
if args.seed is not None:
    random.seed(args.seed)

# ---------------------------------------------------------------------------------------------------------

# Scan the input directory for all .mp4 video files.
# Only the filename (not the full path) is stored in the playlist.
pvs = [os.path.basename(f) for f in glob.glob(os.path.join(args.input, "*.mp4"))]

# Separate training videos from test videos.
# Training videos (defined in TRAINING_SET above) will be shown first.
# Test videos are all other videos found in the input directory.
test_pvs = [p for p in pvs if p not in TRAINING_SET]
training_pvs = sorted([p for p in pvs if p in TRAINING_SET])

# Create output directory if it doesn't exist
os.makedirs(args.output, exist_ok=True)

# Generate subject IDs: either sequential (1, 2, 3, ...) or prime-based
if args.primes:
    primes = generate_primes(args.prime_min, args.prime_max)
    if len(primes) < args.number:
        raise ValueError(
            f"Not enough primes in range [{args.prime_min}, {args.prime_max}] "
            f"for {args.number} subjects (found {len(primes)} primes). "
            "Try increasing --prime-max or decreasing --prime-min."
        )
    random.shuffle(primes)
    subject_ids = primes[: args.number]
else:
    subject_ids = list(range(1, args.number + 1))

# Generate one config file per subject.
# Each subject gets the same training videos (in order) but a unique
# randomized order of test videos to counterbalance presentation order effects.
for subject_id in subject_ids:
    playlist = []

    # Add training section if any training videos were found.
    # TRAINING_START and TRAINING_END markers tell the app to show
    # a training introduction screen and mark these as practice trials.
    if training_pvs:
        playlist.append("TRAINING_START")
        playlist.extend(training_pvs)
        playlist.append("TRAINING_END")

    # Randomize the order of test videos for this subject.
    # Each subject gets a different random order.
    shuffled_test_pvs = test_pvs.copy()
    random.shuffle(shuffled_test_pvs)
    playlist.extend(shuffled_test_pvs)

    # Build the config object. You can extend this with additional fields:
    # - "custom_messages": {"start_message": "...", "finish_message": "...", ...}
    # - "pre_questionnaire": [{"question": "...", "type": "..."}, ...]
    # - "post_questionnaire": [{"question": "...", "type": "..."}, ...]
    # See the JSON schema at json-schema/subject-config.schema.json for details.
    config = {"method": args.method, "playlist": playlist}

    # Write the config file as subject_<id>.json
    output_file = os.path.join(args.output, f"subject_{subject_id}.json")
    with open(output_file, "w") as f:
        json.dump(config, f, indent=2)

    print(f"[info] wrote config for subject {subject_id}: {output_file}")

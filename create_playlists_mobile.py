#!/usr/bin/env python
# Generate mobile playlists for SubjectivePlayer
# Author: Werner Robitza

import argparse
import glob
import math
import os
import random

TRAINING_SET = [
    "01_tr03_SRC200_HRC03.mp4",
    "02_tr03_SRC414_HRC61.mp4",
    "03_tr03_SRC118_HRC68.mp4",
    "04_tr03_SRC115_HRC76.mp4",
    "05_tr03_SRC305_HRC69.mp4",
    "06_tr03_SRC218_HRC02.mp4",
    "07_TR00_SRC00_HRC01.mp4",
]

SUFFIX = ".cfg"

parser = argparse.ArgumentParser(
    description="""This program generates mobile playlists for a number of users in a random fashion"""
)
parser.add_argument(
    "-i", "--input", type=str, default="./avbuffpvs/", help="""Path to PVS"""
)
parser.add_argument(
    "-o",
    "--output",
    type=str,
    default="./playlists_mobile/",
    help="""Path to output dir""",
)
parser.add_argument(
    "-n", "--number", type=int, help="""Number of playlists to generate""", default=30
)
parser.add_argument("-s", "--sessions", type=int, help="Number of sessions", default=3)
parser.add_argument(
    "-p",
    "--prime",
    default=False,
    action="store_true",
    help="Use prime numbers for ID generation",
)

args = parser.parse_args()

# ---------------------------------------------------------------------------------------------------------


# check if number is prime
def is_prime(n):
    for i in range(2, int(math.sqrt(n)) + 1):
        if n % i == 0:
            return False
    return True


# get a number of primes starting with a number
def get_primes(n, init=0):
    primes = list()
    i = init
    while len(primes) < n:
        if is_prime(i):
            primes.append(i)
        i = i + 1
    return primes


# split a list into equal parts
def get_randomized_and_split(input_list, n_per_split):
    shuffled = sorted(input_list, key=lambda k: random.random())
    l = [
        shuffled[i : i + n_per_split]
        for i in range(0, (args.sessions - 1) * n_per_split, n_per_split)
    ]
    l.append(shuffled[(args.sessions - 1) * n_per_split :])
    return l


# create test list
pvs = [os.path.basename(f) for f in glob.glob(args.input + "*.mp4")]

# remove training videos
pvs = [p for p in pvs if p not in TRAINING_SET]

# split into sessions
pvs_per_session = int(len(pvs) / args.sessions)

# get prime numbers for ID generation
primes = get_primes(args.number, init=3)

# create output dir if necessary
if not os.path.exists(args.output):
    os.makedirs(args.output)

# generate playlists and write to files
for user_id, prime in enumerate(primes):
    # start with ID 1 for users
    user_id = user_id + 1
    playlists = get_randomized_and_split(pvs, pvs_per_session)

    # IDs for files
    training_id = str(user_id) + "0"
    session_ids = list()

    # session 1, 2, 3
    for i in range(1, args.sessions + 1):
        if args.prime:
            session_ids.append(str(prime**i))
        else:
            session_ids.append(str(user_id) + str(i))

    training_file = open(args.output + "playlist" + training_id + SUFFIX, "w")
    print(
        "[info] writing training file for user "
        + str(user_id)
        + ": "
        + training_file.name
    )
    for f in TRAINING_SET:
        training_file.write(f + "\n")
    training_file.close()

    for session_id, playlist in zip(session_ids, playlists):
        session_file = open(args.output + "playlist" + session_id + SUFFIX, "w")
        print(
            "[info] writing session file for user "
            + str(user_id)
            + ": "
            + session_file.name
        )
        for f in playlist:
            session_file.write(f + "\n")
        session_file.close()

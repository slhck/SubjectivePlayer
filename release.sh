#!/usr/bin/env bash
#
# Release the project and bump version number in the process.
# Creates a git tag and generates/updates CHANGELOG.md using git-cliff.

set -e

cd "$(dirname "$0")"

FORCE=false

usage() {
    echo "Usage: $0 [options] VERSION"
    echo
    echo "VERSION:"
    echo "  major: bump major version number"
    echo "  minor: bump minor version number"
    echo "  patch: bump patch version number"
    echo
    echo "Options:"
    echo "  -f, --force:  force release (skip branch/clean checks)"
    echo "  -h, --help:   show this help message"
    exit 1
}

# parse args
while [ "$#" -gt 0 ]; do
    case "$1" in
    -f | --force)
        FORCE=true
        shift
        ;;
    -h | --help)
        usage
        ;;
    *)
        break
        ;;
    esac
done

# check if version is specified
if [ "$#" -ne 1 ]; then
    usage
fi

if [ "$1" != "major" ] && [ "$1" != "minor" ] && [ "$1" != "patch" ]; then
    usage
fi

# ensure we are on master branch unless forced
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "")
if [ -z "$CURRENT_BRANCH" ]; then
    echo "Error: unable to determine the current Git branch. Are you in a Git repository?"
    exit 1
fi
if [ "$CURRENT_BRANCH" != "master" ] && [ "$FORCE" = false ]; then
    echo "Error: Releases are only allowed from the 'master' branch. Current branch is '$CURRENT_BRANCH'. Use --force to override."
    exit 1
fi

# check if git is clean and force is not enabled
if ! git diff-index --quiet HEAD -- && [ "$FORCE" = false ]; then
    echo "Error: git is not clean. Please commit all changes first."
    exit 1
fi

# check for required tools
if ! command -v git-cliff > /dev/null; then
    echo "git-cliff is not installed. Please install it first:"
    echo "  brew install git-cliff"
    echo "  or: cargo install git-cliff"
    exit 1
fi

# use GNU grep if available (macOS)
if command -v ggrep > /dev/null; then
    GREP="ggrep"
else
    GREP="grep"
fi

# the build.gradle.kts contains the following version info:
# versionCode = 2
# versionName = "2.0.0"

VERSION_FILE="app/build.gradle.kts"

# extract the current version (handles both "X.Y" and "X.Y.Z" formats)
CURRENT_VERSION=$($GREP -oP '(?<=versionName = ")[^"]+' "$VERSION_FILE")

# extract the versionCode
VERSION_CODE=$($GREP -oP '(?<=versionCode = )\d+' "$VERSION_FILE")

echo "Bumping $VERSION_FILE"

# extract version components (handle missing patch version)
MAJOR=$(cut -d "." -f 1 <<<"$CURRENT_VERSION")
MINOR=$(cut -d "." -f 2 <<<"$CURRENT_VERSION")
PATCH=$(cut -d "." -f 3 <<<"$CURRENT_VERSION")

# default patch to 0 if not present
if [ -z "$PATCH" ]; then
    PATCH=0
fi

echo "Current version: $MAJOR.$MINOR.$PATCH (versionCode: $VERSION_CODE)"

if [ "$1" == "major" ]; then
    MAJOR=$((MAJOR + 1))
    MINOR=0
    PATCH=0
elif [ "$1" == "minor" ]; then
    MINOR=$((MINOR + 1))
    PATCH=0
elif [ "$1" == "patch" ]; then
    PATCH=$((PATCH + 1))
fi

# increment VERSION_CODE
VERSION_CODE=$((VERSION_CODE + 1))

NEW_VERSION="$MAJOR.$MINOR.$PATCH"
echo "New version: $NEW_VERSION (versionCode: $VERSION_CODE)"

# prompt for confirmation
if [ "$FORCE" = false ]; then
    read -p "Do you want to release v$NEW_VERSION? [y/N] " -n 1 -r
    echo
else
    REPLY="y"
fi

if [[ $REPLY =~ ^[Yy]$ ]]; then
    # run lint and tests before releasing
    echo "Running lint and tests..."
    if ! ./gradlew lint test; then
        echo "Error: lint or tests failed. Please fix issues before releasing."
        exit 1
    fi
    echo "Lint and tests passed."
    echo

    # replace version number in build.gradle.kts (Kotlin DSL syntax)
    perl -pi -e "s/versionCode = \d+/versionCode = $VERSION_CODE/g" "$VERSION_FILE"
    perl -pi -e "s/versionName = \"[^\"]+\"/versionName = \"$NEW_VERSION\"/g" "$VERSION_FILE"

    # generate/update changelog
    echo "Generating CHANGELOG.md..."
    git-cliff --tag "v$NEW_VERSION" -o CHANGELOG.md

    # commit changes
    git add "$VERSION_FILE" CHANGELOG.md
    git commit -m "chore: release v$NEW_VERSION"

    # create annotated tag
    git tag -a "v$NEW_VERSION" -m "v$NEW_VERSION"

    echo
    echo "Release v$NEW_VERSION created successfully!"
    echo
    git push origin "$CURRENT_BRANCH"
    git push origin "$CURRENT_BRANCH" --tags
    echo
    echo "Pushed changes and tags to remote repository."
else
    echo "Aborted."
    exit 1
fi

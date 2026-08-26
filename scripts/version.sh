#!/usr/bin/env bash

set -e

ROOT=$(git rev-parse --show-toplevel)
cd "$ROOT"

if [ $# -ne 1 ]; then
    echo "Usage: ./v <tag>"
    echo "  - <tag>: tag name"
    echo "    - must start with v"
    echo "    - format: vM.mTn"
    echo "      - M: Major version code"
    echo "        value range expression: {x | x ∈ ℕ}"
    echo "      - m: minor version code"
    echo "        value range expression: {x | x ∈ [0, 10), x ∈ ℕ}"
    echo "      - T: Type name (r|a|b|p)"
    echo "        - r: release"
    echo "          *when selecting this type, there is no need to fill in the value of n"
    echo "        - a: alpha version (early beta version)"
    echo "        - b: beta version"
    echo "        - p: patch or fix"
    echo "      - n: Number of sub-version"
    echo "        *when selecting the type of release, there is no need to fill in the value of this"
    echo "        value range expression: {x | x ∈ [0, 100), x ∈ ℕ}"
    exit 1
fi

TAG="$1"

if [[ ! "$TAG" =~ ^v ]]; then
    echo "Error: tag must start with v"
    exit 1
fi

TAG="$1"

if [[ ! "$TAG" =~ ^v ]]; then
    echo "Error: tag must start with v"
    exit 1
fi

VERSION="${TAG#v}"

DATE=$(date +"%m%d")

if [[ "$VERSION" =~ ^([0-9]+)\.([0-9]+)r$ ]]; then
    RAW_MAJOR="${BASH_REMATCH[1]}"
    RAW_MINOR="${BASH_REMATCH[2]}"
    TYPE="release"
    TYPE_CODE=0
    NUMBER=0
    VERSION_NAME="${RAW_MAJOR}.${RAW_MINOR}-release"
    CODE_MAJOR="$RAW_MAJOR"
    CODE_MINOR="$RAW_MINOR"
elif [[ "$VERSION" =~ ^([0-9]+)\.([0-9]+)a([0-9]+)$ ]]; then
    RAW_MAJOR="${BASH_REMATCH[1]}"
    RAW_MINOR="${BASH_REMATCH[2]}"
    NUMBER="${BASH_REMATCH[3]}"
    TYPE="alpha"
    TYPE_CODE=1
    VERSION_NAME="${RAW_MAJOR}.${RAW_MINOR}-alpha${NUMBER}"
    CODE_MAJOR="$RAW_MAJOR"
    CODE_MINOR="$RAW_MINOR"
    if [ "$CODE_MINOR" -eq 0 ] && [ "$CODE_MAJOR" -eq 0 ]; then
        echo "Error: cannot decrement version (v0.0-alpha)"
        exit 1
    fi
    if [ "$CODE_MINOR" -eq 0 ]; then
        CODE_MAJOR=$((CODE_MAJOR - 1))
        CODE_MINOR=9
    else
        CODE_MINOR=$((CODE_MINOR - 1))
    fi
elif [[ "$VERSION" =~ ^([0-9]+)\.([0-9]+)b([0-9]+)$ ]]; then
    RAW_MAJOR="${BASH_REMATCH[1]}"
    RAW_MINOR="${BASH_REMATCH[2]}"
    NUMBER="${BASH_REMATCH[3]}"
    TYPE="beta"
    TYPE_CODE=2
    VERSION_NAME="${RAW_MAJOR}.${RAW_MINOR}-beta${NUMBER}"
    CODE_MAJOR="$RAW_MAJOR"
    CODE_MINOR="$RAW_MINOR"
    if [ "$CODE_MINOR" -eq 0 ] && [ "$CODE_MAJOR" -eq 0 ]; then
        echo "Error: cannot decrement version (v0.0-beta)"
        exit 1
    fi
    if [ "$CODE_MINOR" -eq 0 ]; then
        CODE_MAJOR=$((CODE_MAJOR - 1))
        CODE_MINOR=9
    else
        CODE_MINOR=$((CODE_MINOR - 1))
    fi
elif [[ "$VERSION" =~ ^([0-9]+)\.([0-9]+)p([0-9]+)$ ]]; then
    RAW_MAJOR="${BASH_REMATCH[1]}"
    RAW_MINOR="${BASH_REMATCH[2]}"
    NUMBER="${BASH_REMATCH[3]}"
    if [ "$NUMBER" -eq 0 ]; then
        echo "Error: patch number cannot be 0"
        exit 1
    fi
    TYPE="patch"
    TYPE_CODE=0
    VERSION_NAME="${RAW_MAJOR}.${RAW_MINOR}-patch${NUMBER}"
    CODE_MAJOR="$RAW_MAJOR"
    CODE_MINOR="$RAW_MINOR"
else
    echo "Error: unrecognized version format: $TAG"
    exit 1
fi

case "$TYPE" in
    release) NUMBER_PAD="00" ;;
    *)       NUMBER_PAD=$(printf "%02d" "$NUMBER") ;;
esac

VERSION_CODE_RAW="${CODE_MAJOR}${CODE_MINOR}${TYPE_CODE}${NUMBER_PAD}"
VERSION_CODE=$((10#$VERSION_CODE_RAW))

mkdir -p .release

cat > .release/release.env <<EOF
TAG=$TAG
TYPE=$TYPE
RAW_MAJOR=$RAW_MAJOR
RAW_MINOR=$RAW_MINOR
NUMBER=$NUMBER
VERSION_NAME=$VERSION_NAME
VERSION_CODE=$VERSION_CODE
DATE=$DATE
EOF

echo
echo "========== Version Info Generated =========="
echo
echo "  TAG          : $TAG"
echo "  TYPE         : $TYPE"
echo "  VERSION_NAME : $VERSION_NAME"
echo "  VERSION_CODE : $VERSION_CODE"
echo
echo "============================================"
echo
echo "Config saved to .release/release.env"

#!/usr/bin/env bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$ROOT_DIR"

FORCE=false

if [[ "$1" == "-f" ]]; then
    FORCE=true
    shift
fi

if [ ! -f ".release/release.env" ]; then
    echo "Error: .release/release.env not found"
    echo "Please run first:"
    echo "  ./version.sh vx.xr | vx.xax | vx.xbx | vx.xpx"
    exit 1
fi

source .release/release.env

if [ -z "$TAG" ]; then
    echo "Error: TAG not set in .release/release.env"
    exit 1
fi

CURRENT_BRANCH=$(git branch --show-current)

if [ -z "$CURRENT_BRANCH" ]; then
    echo "Error: not on any branch (detached HEAD)"
    exit 1
fi

echo
echo "=========================================="
echo "  WebXiewer Publish"
echo
echo "  Tag     : $TAG"
echo "  Branch  : $CURRENT_BRANCH"
echo "  Force   : $FORCE"
echo "=========================================="
echo

echo "[1/4] Checking Git status..."

if ! $FORCE; then
    if ! git diff --quiet; then
        echo
        echo "Error: working directory has uncommitted changes"
        echo "Please commit or stash them first, or use -f"
        exit 1
    fi

    if ! git diff --cached --quiet; then
        echo
        echo "Error: staged changes exist"
        echo "Please commit or unstage them, or use -f"
        exit 1
    fi
fi

echo "[2/4] Committing..."

git add -A

if git diff --cached --quiet; then
    echo "  No changes to commit"
else
    git commit -m "release: $TAG"
    echo "  Commit created"
fi

echo "[3/4] Creating tag..."

if git rev-parse "$TAG" >/dev/null 2>&1; then
    if ! $FORCE; then
        echo
        echo "Error: tag $TAG already exists"
        echo "Use -f to overwrite"
        exit 1
    fi
    echo "  Overwriting existing tag"
    git tag -d "$TAG"
fi

git tag "$TAG"
echo "  Tag created"

echo "[4/4] Pushing..."

if $FORCE; then
    echo "  Force mode: deleting remote tag if exists"
    git push --force origin "$CURRENT_BRANCH"
    git push origin ":refs/tags/$TAG" 2>/dev/null || true
    git push origin "$TAG"
else
    git push origin "$CURRENT_BRANCH"
    git push origin "$TAG"
fi

echo
echo "=========================================="
echo "  Publish Complete"
echo
echo "  Tag : $TAG"
echo
echo "  GitHub Actions will start building"
echo "=========================================="

#!/bin/sh
# Pulls the latest source code from the Git repository.
# If the repository is not cloned yet, it will be cloned.

set -e

# Check if git is installed
command -v git >/dev/null || { echo "Error: git is not installed. Please install git and try again."; exit 1; }

SCRIPT_DIR=$(dirname "$(readlink -f "$0")")
. "$SCRIPT_DIR/app.conf"

# Auto-detect development mode if not explicitly set
if [ -z "$DEV_MODE" ] && [ -f "$SCRIPT_DIR/pom.xml" ] && git -C "$SCRIPT_DIR" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  DEV_MODE=true
fi

if [ "$DEV_MODE" = "true" ]; then
  echo "Development environment detected."
  echo "Skipping git pull in development mode to preserve local working tree."
  exit 0
fi

TARGET_REF="${1:-$PARAM_BRANCH}"

if [ ! -d "$REPO_DIR" ]; then
  [ ! -d "$BUILD_DIR" ] && mkdir -p "$BUILD_DIR"
  cd "$BUILD_DIR"
  if [ -n "$TARGET_REF" ]; then
    echo "Cloning repository with branch/tag: $TARGET_REF ..."
    if ! git clone -b "$TARGET_REF" "$REPO_URL" "$APP_NAME"; then
      echo "[ERROR] Failed to clone branch or tag '$TARGET_REF' from $REPO_URL."
      echo "[ERROR] Please check if the branch or tag name exists."
      exit 1
    fi
  else
    echo "Cloning repository from $REPO_URL ..."
    if ! git clone "$REPO_URL" "$APP_NAME"; then
      echo "[ERROR] Failed to clone repository from $REPO_URL."
      exit 1
    fi
  fi
else
  cd "$REPO_DIR"
  echo "Fetching latest changes and tags from remote repository..."
  if ! git fetch --all --tags --prune; then
    echo "[ERROR] Failed to fetch updates from remote repository."
    exit 1
  fi

  if [ -n "$TARGET_REF" ]; then
    echo "Switching to branch or tag: $TARGET_REF ..."
    if git rev-parse --verify --quiet "refs/tags/$TARGET_REF" >/dev/null 2>&1; then
      echo "Checking out tag '$TARGET_REF'..."
      git checkout -q "refs/tags/$TARGET_REF"
    elif git rev-parse --verify --quiet "refs/heads/$TARGET_REF" >/dev/null 2>&1; then
      echo "Checking out local branch '$TARGET_REF'..."
      git checkout -q "$TARGET_REF"
      if git rev-parse --verify --quiet "origin/$TARGET_REF" >/dev/null 2>&1; then
        git pull --ff-only origin "$TARGET_REF" || git pull origin "$TARGET_REF"
      fi
    elif git rev-parse --verify --quiet "origin/$TARGET_REF" >/dev/null 2>&1; then
      echo "Checking out remote branch 'origin/$TARGET_REF'..."
      git checkout -B "$TARGET_REF" "origin/$TARGET_REF"
    elif git rev-parse --verify --quiet "$TARGET_REF^{commit}" >/dev/null 2>&1; then
      echo "Checking out commit '$TARGET_REF'..."
      git checkout -q "$TARGET_REF"
    else
      echo "[ERROR] Branch, tag, or commit '$TARGET_REF' not found in repository."
      echo "[ERROR] Please check the branch or tag name and try again."
      exit 1
    fi
  else
    echo "Pulling latest changes for current branch..."
    git pull
  fi
fi

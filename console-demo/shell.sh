#!/bin/sh
# Starts the interactive shell with debug mode enabled.

set -e

SCRIPT_DIR=$(dirname "$(readlink -f "$0")")
cd "$SCRIPT_DIR"

if [ "$SERVER_ENGINE" = "undertow" ]; then
  . "$SCRIPT_DIR/app-undertow.conf"
else
  . "$SCRIPT_DIR/app.conf"
fi

"$DEPLOY_DIR/bin/shell.sh" --debug "$@"

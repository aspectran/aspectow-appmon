#!/bin/sh
# Starts the interactive shell with debug mode enabled using Undertow engine.

set -e

SCRIPT_DIR=$(dirname "$(readlink -f "$0")")
cd "$SCRIPT_DIR"
. "$SCRIPT_DIR/app-undertow.conf"

"$DEPLOY_DIR/bin/shell.sh" --debug "$@"

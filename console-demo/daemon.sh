#!/bin/sh
# A wrapper script to run the jsvc daemon with the correct application context.

set -e

SCRIPT_DIR=$(dirname "$(readlink -f "$0")")
cd "$SCRIPT_DIR"

if [ "$SERVER_ENGINE" = "undertow" ]; then
  . "$SCRIPT_DIR/app-undertow.conf"
else
  . "$SCRIPT_DIR/app.conf"
fi

"$DEPLOY_DIR/bin/jsvc-daemon.sh" \
  --proc-name "$PROC_NAME" \
  --user "$DAEMON_USER" \
  "$@"

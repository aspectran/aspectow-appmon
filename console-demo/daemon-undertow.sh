#!/bin/sh
# A wrapper script to run the jsvc daemon with the Undertow application context.

set -e

SCRIPT_DIR=$(dirname "$(readlink -f "$0")")
cd "$SCRIPT_DIR"
. "$SCRIPT_DIR/app-undertow.conf"

"$DEPLOY_DIR/bin/jsvc-daemon.sh" \
  --proc-name "$PROC_NAME" \
  --user "$DAEMON_USER" \
  "$@"

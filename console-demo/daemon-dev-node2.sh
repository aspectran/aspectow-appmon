#!/bin/sh
# A wrapper script to run the jsvc daemon for node2 (Port: 8092).

set -e

SCRIPT_DIR=$(dirname "$(readlink -f "$0")")
cd "$SCRIPT_DIR"
. "$SCRIPT_DIR/app.conf"

NODE_ID="dev-node2"
PORT="8092"

[ -d "$DEPLOY_DIR" ] && DEPLOY_DIR="$(cd "$DEPLOY_DIR" && pwd)"

PROC_NAME="${APP_NAME}-${NODE_ID}"
WORK_DIR="$DEPLOY_DIR/work2"
TEMP_DIR="$DEPLOY_DIR/temp2"
COMMANDS_DIR="$DEPLOY_DIR/cmd2"
LOGS_DIR="$DEPLOY_DIR/logs2"

ASPECTRAN_OPTS="
-Duser.timezone=UTC
-Daspectran.profiles.active=dev,gateway
-Daspectran.profiles.base.console=dev,h2
-Daspectran.workPath=$WORK_DIR
-Daspectran.tempPath=$TEMP_DIR
-Daspectran.commandsPath=$COMMANDS_DIR
-Daspectran.logsDir=$LOGS_DIR
-Daspectow.node.id=$NODE_ID
-Djava.io.tmpdir=$TEMP_DIR
-Dtow.server.listener.http.port=$PORT
-Dtow.context.root.session.cookieName=JSESSIONID-$PORT
-Dtow.context.console.session.cookieName=JSESSIONID-$PORT
-Daspectow.console.config.db.h2.path_explicit=~/aspectow-console-demo
"

"$DEPLOY_DIR/bin/jsvc-daemon.sh" \
  --proc-name "$PROC_NAME" \
  --logs-dir "$LOGS_DIR" \
  --temp-dir "$TEMP_DIR" \
  --user "$DAEMON_USER" \
  "$@"

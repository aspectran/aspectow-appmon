#!/bin/sh
# A wrapper script to run the jsvc daemon for node1 (Port: 8091).

set -e

SCRIPT_DIR=$(dirname "$(readlink -f "$0")")
cd "$SCRIPT_DIR"
. "$SCRIPT_DIR/app.conf"

NODE_ID="dev-node1"
PORT="8091"

[ -d "$DEPLOY_DIR" ] && DEPLOY_DIR="$(cd "$DEPLOY_DIR" && pwd)"

PROC_NAME="${APP_NAME}-${NODE_ID}"
WORK_DIR="$DEPLOY_DIR/work1"
TEMP_DIR="$DEPLOY_DIR/temp1"
COMMANDS_DIR="$DEPLOY_DIR/cmd1"
LOGS_DIR="$DEPLOY_DIR/logs1"

SERVER_ENGINE="${SERVER_ENGINE:-netty}"

if [ "$SERVER_ENGINE" = "undertow" ]; then
  PROC_NAME="${PROC_NAME}-undertow"
  SERVER_OPTS="
-Daspectran.profiles.active=dev,gateway,console.custom-ui,undertow
-Daspectran.profiles.base.console=dev,h2,console.custom-ui,undertow
-Dtow.server.listener.http.port=$PORT
-Dtow.context.root.session.cookieName=JSESSIONID_$PORT
-Dtow.context.console.session.cookieName=JSESSIONID_$PORT
"
else
  SERVER_OPTS="
-Daspectran.profiles.active=dev,gateway,console.custom-ui
-Daspectran.profiles.base.console=dev,h2,console.custom-ui
-Dnetty.server.listener.http.port=$PORT
-Dnetty.context.root.session.cookieName=JSESSIONID_$PORT
-Dnetty.context.console.session.cookieName=JSESSIONID_$PORT
"
fi

ASPECTRAN_OPTS="
-Duser.timezone=UTC
-Daspectran.workPath=$WORK_DIR
-Daspectran.tempPath=$TEMP_DIR
-Daspectran.commandsPath=$COMMANDS_DIR
-Daspectran.logsDir=$LOGS_DIR
-Daspectow.node.id=$NODE_ID
-Djava.io.tmpdir=$TEMP_DIR
-Daspectow.console.config.db.h2.path_explicit=~/aspectow-console-demo
$SERVER_OPTS
"

"$DEPLOY_DIR/bin/jsvc-daemon.sh" \
  --proc-name "$PROC_NAME" \
  --logs-dir "$LOGS_DIR" \
  --temp-dir "$TEMP_DIR" \
  --user "$DAEMON_USER" \
  "$@"

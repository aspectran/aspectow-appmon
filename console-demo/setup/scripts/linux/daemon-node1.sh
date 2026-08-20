#!/bin/sh
# A wrapper script to run the jsvc daemon for node1 (Port: 8091).

set -e

SCRIPT_DIR=$(dirname "$(readlink -f "$0")")
. "$SCRIPT_DIR/app.conf"

PROC_NAME="${APP_NAME}-node1"

ASPECTRAN_OPTS="
-Duser.timezone=UTC
-Daspectran.profiles.active=dev,gateway,console.ui
-Daspectran.profiles.base.console=dev,h2,console.custom-ui
-Daspectran.workPath=$DEPLOY_DIR/work1
-Daspectran.tempPath=$DEPLOY_DIR/temp1
-Daspectran.commandsPath=$DEPLOY_DIR/cmd1
-Daspectran.logsDir=$DEPLOY_DIR/logs1
-Daspectow.node.id=node1
-Djava.io.tmpdir=$DEPLOY_DIR/temp1
-Dtow.server.listener.http.port=8091
-Dtow.context.root.session.cookieName=JSESSIONID-8091
-Dtow.context.console.session.cookieName=JSESSIONID-8091
-Daspectow.console.config.db.h2.path_explicit=~/aspectow-console-demo-node1
"

"$DEPLOY_DIR/bin/jsvc-daemon.sh" \
  --proc-name "$PROC_NAME" \
  --user "$DAEMON_USER" \
  "$@"

#!/bin/sh
# A wrapper script to run the jsvc daemon for node2 (Port: 8092).

set -e

SCRIPT_DIR=$(dirname "$(readlink -f "$0")")
. "$SCRIPT_DIR/app.conf"

PROC_NAME="${APP_NAME}-node2"

ASPECTRAN_OPTS="
-Duser.timezone=UTC
-Daspectran.profiles.active=dev,gateway
-Daspectran.profiles.base.console=dev,h2
-Daspectran.workPath=$DEPLOY_DIR/work2
-Daspectran.tempPath=$DEPLOY_DIR/temp2
-Daspectran.commandsPath=$DEPLOY_DIR/cmd2
-Daspectran.logsDir=$DEPLOY_DIR/logs2
-Daspectow.node.id=node2
-Djava.io.tmpdir=$DEPLOY_DIR/temp2
-Dtow.server.listener.http.port=8092
-Dtow.context.root.session.cookieName=JSESSIONID-8092
-Dtow.context.console.session.cookieName=JSESSIONID-8092
-Daspectow.console.config.db.h2.path_explicit=~/aspectow-console-demo-node2
"

"$DEPLOY_DIR/bin/jsvc-daemon.sh" \
  --proc-name "$PROC_NAME" \
  --user "$DAEMON_USER" \
  "$@"

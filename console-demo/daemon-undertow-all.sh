#!/bin/sh
# Controls all daemon instances with Undertow engine at once.
# Usage: ./daemon-undertow-all.sh [start|stop|restart|status]

SCRIPT_DIR=$(dirname "$(readlink -f "$0")")
cd "$SCRIPT_DIR"

SERVER_ENGINE="undertow" "$SCRIPT_DIR/daemon-all.sh" "$@"

#!/bin/sh
# Controls all daemon instances (daemon.sh, daemon-node1.sh, daemon-node2.sh) at once.
# Usage: ./daemon-all.sh [start|stop|restart|status]

SCRIPT_DIR=$(dirname "$(readlink -f "$0")")
cd "$SCRIPT_DIR"
SERVER_ENGINE="${SERVER_ENGINE:-netty}"
ARGS=""

for arg in "$@"; do
  case "$arg" in
    --undertow)
      SERVER_ENGINE="undertow"
      ;;
    *)
      if [ -z "$ACTION" ]; then
        ACTION="$arg"
      else
        ARGS="$ARGS $arg"
      fi
      ;;
  esac
done

ACTION="${ACTION:-restart}"
export SERVER_ENGINE

SCRIPTS="daemon.sh daemon-dev-node1.sh daemon-dev-node2.sh"

echo "================================================================================"
echo "Aspectow Console Demo: Running all daemons (Engine: $SERVER_ENGINE)"
echo "================================================================================"
echo ""

for script in $SCRIPTS; do
  if [ -f "$SCRIPT_DIR/$script" ]; then
    echo "--------------------------------------------------------------------------------"
    echo "[$script] Action: $ACTION (Engine: $SERVER_ENGINE)"
    echo "--------------------------------------------------------------------------------"
    "$SCRIPT_DIR/$script" "$ACTION" $ARGS || true
    echo ""
  fi
done

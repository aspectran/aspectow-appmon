#!/bin/sh
# Controls all daemon instances (daemon.sh, daemon-node1.sh, daemon-node2.sh) at once.
# Usage: ./daemon-all.sh [start|stop|restart|status]

SCRIPT_DIR=$(dirname "$(readlink -f "$0")")
ACTION="${1:-restart}"
[ $# -gt 0 ] && shift

SCRIPTS="daemon.sh daemon-node1.sh daemon-node2.sh"

for script in $SCRIPTS; do
  if [ -f "$SCRIPT_DIR/$script" ]; then
    echo "========================================================================"
    echo "[$script] Action: $ACTION"
    echo "========================================================================"
    "$SCRIPT_DIR/$script" "$ACTION" "$@" || true
    echo ""
  fi
done

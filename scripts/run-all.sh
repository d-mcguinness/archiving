#!/usr/bin/env bash
# Run backend + frontend together. Logs to ./logs/, PIDs tracked for stop-all.sh.
# Usage: scripts/run-all.sh [--debug] [profile]
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

DEBUG=0
if [ "${1:-}" = "--debug" ]; then
    DEBUG=1
    shift
fi
PROFILE="${1:-dev}"

mkdir -p logs
PID_DIR="$ROOT_DIR/.run"
mkdir -p "$PID_DIR"

BACKEND_SCRIPT="scripts/run-backend.sh"
FRONTEND_SCRIPT="scripts/run-frontend.sh"
if [ "$DEBUG" -eq 1 ]; then
    BACKEND_SCRIPT="scripts/debug-backend.sh"
    FRONTEND_SCRIPT="scripts/debug-frontend.sh"
    echo "🐞 DEBUG mode: backend jdwp=5005, frontend node-inspect=9229"
fi

echo "🍃 Starting backend → logs/backend.log"
"$ROOT_DIR/$BACKEND_SCRIPT" "$PROFILE" >"$ROOT_DIR/logs/backend.log" 2>&1 &
echo $! >"$PID_DIR/backend.pid"

echo "⚡ Starting frontend → logs/frontend.log"
"$ROOT_DIR/$FRONTEND_SCRIPT" >"$ROOT_DIR/logs/frontend.log" 2>&1 &
echo $! >"$PID_DIR/frontend.pid"

echo
echo "✅ Both services starting."
echo "   Backend  : http://localhost:2020   (pid $(cat $PID_DIR/backend.pid))"
echo "   Frontend : http://localhost:3001   (pid $(cat $PID_DIR/frontend.pid))"
echo "   Tail logs: tail -f logs/backend.log logs/frontend.log"
echo "   Stop    : scripts/stop-all.sh"

#!/usr/bin/env bash
# Debug backend + frontend together.
#   Backend  : Spring Boot on :2020, JDWP on :5005
#   Frontend : SvelteKit on :3001, Node inspector on :9229
# Logs to ./logs/, PIDs tracked for stop-all.sh.
# Usage: scripts/debug-all.sh [profile]
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

PROFILE="${1:-dev}"
JDWP_PORT="${JDWP_PORT:-5005}"
NODE_INSPECT_PORT="${NODE_INSPECT_PORT:-9229}"

mkdir -p logs
PID_DIR="$ROOT_DIR/.run"
mkdir -p "$PID_DIR"

# Fail fast if debug ports are already in use.
for port in "$JDWP_PORT" "$NODE_INSPECT_PORT"; do
    if lsof -ti tcp:"$port" >/dev/null 2>&1; then
        echo "❌ Port $port is already in use. Run scripts/stop-all.sh first."
        exit 1
    fi
done

echo "🐞 DEBUG mode"
echo "   Backend  : http://localhost:2020   (JDWP :$JDWP_PORT)"
echo "   Frontend : http://localhost:3001   (Node inspect :$NODE_INSPECT_PORT)"
echo "   Profile  : $PROFILE"
echo

echo "🍃 Starting backend → logs/backend.log"
"$ROOT_DIR/scripts/debug-backend.sh" "$PROFILE" "$JDWP_PORT" \
    >"$ROOT_DIR/logs/backend.log" 2>&1 &
echo $! >"$PID_DIR/backend.pid"

echo "⚡ Starting frontend → logs/frontend.log"
"$ROOT_DIR/scripts/debug-frontend.sh" "$NODE_INSPECT_PORT" \
    >"$ROOT_DIR/logs/frontend.log" 2>&1 &
echo $! >"$PID_DIR/frontend.pid"

echo
echo "✅ Both services starting in DEBUG mode."
echo "   Attach JVM debugger : localhost:$JDWP_PORT  (IntelliJ → Remote JVM Debug)"
echo "   Attach Node debugger: localhost:$NODE_INSPECT_PORT  (chrome://inspect or VS Code)"
echo "   Tail logs           : tail -f logs/backend.log logs/frontend.log"
echo "   Stop                : scripts/stop-all.sh"

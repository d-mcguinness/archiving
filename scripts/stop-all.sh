#!/usr/bin/env bash
# Stop backend + frontend started via run-all.sh.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_DIR="$ROOT_DIR/.run"

stop_pid_file() {
    local name="$1"
    local file="$PID_DIR/${name}.pid"
    if [ ! -f "$file" ]; then
        echo "⚠️  no pid file for $name"
        return
    fi
    local pid
    pid=$(cat "$file")
    if kill -0 "$pid" 2>/dev/null; then
        echo "🛑 Stopping $name (pid $pid) and children..."
        # Kill the whole process group so mvn/node spawned children die too.
        pkill -TERM -P "$pid" 2>/dev/null || true
        kill -TERM "$pid" 2>/dev/null || true
        sleep 1
        kill -KILL "$pid" 2>/dev/null || true
    else
        echo "ℹ️  $name (pid $pid) not running"
    fi
    rm -f "$file"
}

stop_pid_file backend
stop_pid_file frontend

# Belt-and-braces: kill anything still bound to our ports.
for port in 2020 3001 5005 9229; do
    if lsof -ti tcp:"$port" >/dev/null 2>&1; then
        echo "🧹 Freeing port $port"
        lsof -ti tcp:"$port" | xargs kill -9 2>/dev/null || true
    fi
done

echo "✅ Stopped. (Docker services left running — use 'docker compose -f compose.dev.yaml down' to stop db/localstack.)"
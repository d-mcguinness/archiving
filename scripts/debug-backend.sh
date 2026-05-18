#!/usr/bin/env bash
# Run the Spring Boot backend with JDWP debug agent on port 5005.
# Attach IntelliJ / VS Code "Remote JVM Debug" to localhost:5005.
# Usage: scripts/debug-backend.sh [profile] [debug-port]
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

PROFILE="${1:-dev}"
DEBUG_PORT="${2:-5005}"

echo "🐘 Ensuring Postgres + LocalStack are up (compose.dev.yaml)..."
docker compose -f compose.dev.yaml up -d db localstack

echo "⏳ Waiting for Postgres to be healthy..."
for i in {1..30}; do
    if docker compose -f compose.dev.yaml exec -T db pg_isready -U archiving_user >/dev/null 2>&1; then
        echo "✅ Postgres ready"
        break
    fi
    sleep 1
done

JVM_DEBUG_ARGS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:${DEBUG_PORT}"

echo "🐞 Starting Spring Boot in DEBUG mode"
echo "   profile=$PROFILE  app=2020  jdwp=$DEBUG_PORT"
echo "   Attach a debugger to localhost:${DEBUG_PORT}"

exec ./mvnw spring-boot:run \
    -Dspring-boot.run.profiles="$PROFILE" \
    -Dspring-boot.run.jvmArguments="$JVM_DEBUG_ARGS"
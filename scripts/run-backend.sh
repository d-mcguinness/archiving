#!/usr/bin/env bash
# Run the Spring Boot backend (port 2020).
# Usage: scripts/run-backend.sh [profile]
#   profile defaults to "dev"
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

PROFILE="${1:-dev}"

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

echo "🍃 Starting Spring Boot (profile=$PROFILE, port=2020)..."
exec ./mvnw spring-boot:run -Dspring-boot.run.profiles="$PROFILE"

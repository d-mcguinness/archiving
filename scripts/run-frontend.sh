#!/usr/bin/env bash
# Run the SvelteKit frontend dev server (port 3001).
# Proxies /graphql and /api to the backend on localhost:2020.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR/frontend"

if [ ! -d node_modules ]; then
    echo "📦 Installing frontend dependencies..."
    npm install
fi

echo "⚡ Starting SvelteKit dev server on http://localhost:3001"
exec npm run dev -- --host
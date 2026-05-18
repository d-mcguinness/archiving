#!/usr/bin/env bash
# Run the SvelteKit frontend with Node inspector for SSR debugging.
# Attach Chrome DevTools (chrome://inspect) or VS Code "Attach to Node" on port 9229.
# Browser-side debugging: open http://localhost:3001 and use DevTools.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR/frontend"

INSPECT_PORT="${1:-9229}"

if [ ! -d node_modules ]; then
    echo "📦 Installing frontend dependencies..."
    npm install
fi

echo "🐞 Starting SvelteKit dev server in DEBUG mode"
echo "   app=http://localhost:3001  node-inspector=$INSPECT_PORT"
echo "   Open chrome://inspect or attach VS Code to localhost:${INSPECT_PORT}"

# --inspect enables the V8 inspector for SSR / server hooks.
exec node --inspect="0.0.0.0:${INSPECT_PORT}" node_modules/vite/bin/vite.js dev --host
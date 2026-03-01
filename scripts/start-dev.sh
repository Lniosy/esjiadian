#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_PORT="${BACKEND_PORT:-8080}"
FRONTEND_PORT="${FRONTEND_PORT:-5173}"

"$ROOT_DIR/scripts/preflight.sh"

mkdir -p "$ROOT_DIR/.run"

BACKEND_LOG="$ROOT_DIR/.run/backend.log"
FRONTEND_LOG="$ROOT_DIR/.run/frontend.log"
BACKEND_PID_FILE="$ROOT_DIR/.run/backend.pid"
FRONTEND_PID_FILE="$ROOT_DIR/.run/frontend.pid"

nohup bash -lc "cd '$ROOT_DIR/backend' && mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=${BACKEND_PORT}" >"$BACKEND_LOG" 2>&1 &
echo $! > "$BACKEND_PID_FILE"

nohup bash -lc "cd '$ROOT_DIR/frontend' && VITE_PROXY_TARGET='http://localhost:${BACKEND_PORT}' npm run dev -- --host 0.0.0.0 --port ${FRONTEND_PORT}" >"$FRONTEND_LOG" 2>&1 &
echo $! > "$FRONTEND_PID_FILE"

echo "[OK] started backend pid=$(cat "$BACKEND_PID_FILE") log=$BACKEND_LOG"
echo "[OK] started frontend pid=$(cat "$FRONTEND_PID_FILE") log=$FRONTEND_LOG"
echo "backend: http://localhost:${BACKEND_PORT}"
echo "frontend: http://localhost:${FRONTEND_PORT}"

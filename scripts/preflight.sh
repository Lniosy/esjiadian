#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

need_cmd() {
  local cmd="$1"
  local hint="$2"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "[ERROR] missing '$cmd' - $hint"
    return 1
  fi
  echo "[OK] $cmd: $(command -v "$cmd")"
}

check_port() {
  local host="$1"
  local port="$2"
  local name="$3"
  if command -v nc >/dev/null 2>&1 && nc -z "$host" "$port" >/dev/null 2>&1; then
    echo "[OK] $name reachable at ${host}:${port}"
  else
    echo "[WARN] $name not reachable at ${host}:${port}"
  fi
}

echo "== Preflight: used-appliance-system =="
echo "root: $ROOT_DIR"

need_cmd java "Install JDK 17+" || exit 1
need_cmd mvn "Install Maven 3.9+" || exit 1
need_cmd node "Install Node.js 20+" || exit 1
need_cmd npm "Install npm with Node.js" || exit 1

echo "== Version Check =="
java -version 2>&1 | head -n 2
mvn -v | head -n 2
node -v
npm -v

echo "== Service Reachability =="
check_port 127.0.0.1 3306 "MySQL"
check_port 127.0.0.1 6379 "Redis"

echo "== Build Smoke Check =="
( cd "$ROOT_DIR/backend" && mvn -q -DskipTests compile )
( cd "$ROOT_DIR/frontend" && npm -s run build >/dev/null )

echo "[OK] preflight complete"

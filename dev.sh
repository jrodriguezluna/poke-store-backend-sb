#!/usr/bin/env bash
set -Eeuo pipefail

BACKEND_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_ARG="${1:-$BACKEND_DIR/../poke-store}"

if [[ ! -d "$FRONTEND_ARG" ]]; then
  echo "No se encontró el frontend: $FRONTEND_ARG" >&2
  echo "Uso: ./dev.sh [ruta/al/poke-store]" >&2
  exit 1
fi

FRONTEND_DIR="$(cd -- "$FRONTEND_ARG" && pwd)"
VITE="$FRONTEND_DIR/node_modules/.bin/vite"

if ! command -v java >/dev/null 2>&1; then
  echo "Java no está instalado o no está disponible en PATH." >&2
  exit 1
fi

if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven no está instalado o no está disponible en PATH." >&2
  exit 1
fi

if ! command -v node >/dev/null 2>&1; then
  echo "Node.js no está instalado o no está disponible en PATH." >&2
  exit 1
fi

if [[ ! -x "$VITE" ]]; then
  echo "No se encontró Vite en $VITE. Ejecuta npm ci en el frontend." >&2
  exit 1
fi

(
  cd "$BACKEND_DIR"
  export SPRING_PROFILES_ACTIVE=local
  exec mvn spring-boot:run
) &
BACKEND_PID=$!

(
  cd "$FRONTEND_DIR"
  exec "$VITE" --host 127.0.0.1
) &
FRONTEND_PID=$!

cleanup() {
  local status=$?
  trap - EXIT
  kill "$BACKEND_PID" "$FRONTEND_PID" 2>/dev/null || true
  wait "$BACKEND_PID" 2>/dev/null || true
  wait "$FRONTEND_PID" 2>/dev/null || true
  exit "$status"
}

trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

echo "Backend:  http://localhost:8000"
echo "Frontend: http://localhost:5173"
echo "Detén ambos servidores con Ctrl+C."

wait -n "$BACKEND_PID" "$FRONTEND_PID"

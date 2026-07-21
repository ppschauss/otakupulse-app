#!/bin/bash
# OtakuPulse-Companion-Backend (Docker-Run-Fallback, da der Unraid-Host kein compose-Plugin hat).
set -e
cd "$(dirname "$0")"

IMAGE=otakupulse-companion:latest
NAME=otakupulse-companion
NET=otakupulse-net          # dort hängt auch otakupulse-db
PORT=3005                   # Host-Port, NPM leitet app.otakupulse.de hierher

build() {
  # Build-Kontext ist server/, damit app/ und tests/ mit ins Image kommen.
  docker build -f build/Dockerfile -t "$IMAGE" .
}

up() {
  [ -f ./secrets.env ] || { echo "secrets.env fehlt (Vorlage: secrets.env.example)"; exit 1; }
  docker rm -f "$NAME" 2>/dev/null || true
  docker run -d --name "$NAME" --restart unless-stopped \
    --network "$NET" \
    --env-file ./secrets.env -e TZ=Europe/Berlin \
    -v "$(pwd)/secrets:/secrets:ro" \
    -p "$PORT:8000" \
    "$IMAGE"
  echo "gestartet auf Port $PORT."
}

case "$1" in
  build)   build ;;
  up)      up ;;
  rebuild) build && up ;;
  down)    docker rm -f "$NAME" ;;
  restart) docker restart "$NAME" ;;
  logs)    docker logs -f "$NAME" ;;
  test)    docker exec "$NAME" python -m pytest /app/tests -q ;;
  shell)   docker exec -it "$NAME" bash ;;
  *) echo "Nutzung: $0 {build|up|rebuild|down|restart|logs|test|shell}" ;;
esac

#!/usr/bin/env bash
# Регистрирует или обновляет коннекторы Debezium.
# PUT .../config идемпотентен: создаёт коннектор или обновляет конфиг существующего
# (POST /connectors при повторном запуске ответил бы 409).
set -euo pipefail

CONNECT_URL="${CONNECT_URL:-http://localhost:8083}"
DIR="$(cd "$(dirname "$0")" && pwd)"

register() {
  local name="$1" file="$2"
  echo "==> $name"
  curl -fsS -X PUT -H "Content-Type: application/json" \
    --data @"$DIR/$file" \
    "$CONNECT_URL/connectors/$name/config"
  echo
}

register profile-outbox profile-outbox.json

sleep 3
curl -fsS "$CONNECT_URL/connectors/profile-outbox/status"
echo

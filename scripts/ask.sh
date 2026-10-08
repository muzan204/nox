#!/data/data/com.termux/files/usr/bin/bash
set -e
MSG="$*"
if [ -z "$MSG" ]; then
  echo 'Uso: ~/nox/scripts/ask.sh "Olá NOX"'
  exit 1
fi
curl -sS http://127.0.0.1:8765/api/chat \
  -H 'Content-Type: application/json' \
  -d "$(python -c 'import json,sys; print(json.dumps({"message":" ".join(sys.argv[1:])},ensure_ascii=False))' "$@")"
echo

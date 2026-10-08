#!/data/data/com.termux/files/usr/bin/bash
set -e

ROOT="$HOME/nox"
LLAMA="$ROOT/ai/llama.cpp/build/bin/llama-server"
MODEL="$ROOT/models/qwen2.5-0.5b-instruct-q4_k_m.gguf"
LOG="$ROOT/logs"

# IMPORTANTE:
# Prioriza as bibliotecas do build atual do llama.cpp.
export LD_LIBRARY_PATH="$ROOT/ai/llama.cpp/build/bin:$PREFIX/lib"

termux-wake-lock 2>/dev/null || true

if [ ! -x "$LLAMA" ]; then
  echo "ERRO: llama-server não foi compilado:"
  echo "  $LLAMA"
  exit 1
fi

if [ ! -f "$MODEL" ]; then
  echo "ERRO: modelo não encontrado:"
  echo "  $MODEL"
  exit 1
fi

mkdir -p "$LOG"

pkill -f "$ROOT/ai/llama.cpp/build/bin/llama-server" 2>/dev/null || true
pkill -f "$ROOT/core/server/server.js" 2>/dev/null || true
sleep 2

echo "Iniciando llama-server..."

nohup "$LLAMA" \
  -m "$MODEL" \
  --host 127.0.0.1 \
  --port 8080 \
  -c 1024 \
  -t 4 \
  -lm none \
  > "$LOG/llama.log" 2>&1 &

echo "Aguardando modelo..."

READY=0

for i in $(seq 1 90); do
  if curl -sf http://127.0.0.1:8080/health >/dev/null 2>&1; then
    READY=1
    break
  fi
  sleep 1
done

if [ "$READY" -ne 1 ]; then
  echo "ERRO: llama-server não ficou online."
  echo ""
  echo "Últimas linhas do log:"
  tail -n 50 "$LOG/llama.log" || true
  exit 1
fi

echo "llama-server: OK"

echo "Iniciando NOX Core..."

nohup node "$ROOT/core/server/server.js" \
  > "$LOG/core.log" 2>&1 &

sleep 2

if curl -sf http://127.0.0.1:8765/api/health >/dev/null 2>&1; then
  echo ""
  echo "======================================"
  echo "          NOX ONLINE"
  echo "======================================"
  echo "LLAMA: http://127.0.0.1:8080"
  echo "CORE:  http://127.0.0.1:8765"
  echo "UI:    http://127.0.0.1:8765/"
  echo "======================================"
else
  echo ""
  echo "ERRO: NOX Core não ficou online."
  echo ""
  tail -n 50 "$LOG/core.log" || true
  exit 1
fi

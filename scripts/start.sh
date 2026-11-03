#!/data/data/com.termux/files/usr/bin/bash
set -e

ROOT="$HOME/nox"
LLAMA="$ROOT/ai/llama.cpp/build/bin/llama-server"
MODEL="$ROOT/models/qwen2.5-0.5b-instruct-q4_k_m.gguf"
LOG="$ROOT/logs"
CORE="$ROOT/core/server/server.js"
GATEWAY="$ROOT/core/server/lan-gateway.js"

export LD_LIBRARY_PATH="$ROOT/ai/llama.cpp/build/bin:$PREFIX/lib"
export NOX_CORE_HOST=127.0.0.1
export NOX_CORE_PORT=8765
export NOX_LAN_HOST=0.0.0.0
export NOX_LAN_PORT=8766

termux-wake-lock 2>/dev/null || true
mkdir -p "$LOG" "$ROOT/config"

for file in "$LLAMA" "$CORE" "$GATEWAY" "$MODEL"; do
  if [ ! -e "$file" ]; then echo "ERRO: arquivo não encontrado: $file"; exit 1; fi
done
if [ ! -x "$LLAMA" ]; then echo "ERRO: llama-server não executável: $LLAMA"; exit 1; fi

pkill -f "$ROOT/ai/llama.cpp/build/bin/llama-server" 2>/dev/null || true
pkill -f "$ROOT/core/server/server.js" 2>/dev/null || true
pkill -f "$ROOT/core/server/lan-gateway.js" 2>/dev/null || true
sleep 2

echo "[1/3] Iniciando modelo local..."
nohup "$LLAMA" -m "$MODEL" --host 127.0.0.1 --port 8080 -c 1024 -t 4 -lm none > "$LOG/llama.log" 2>&1 &
READY=0
for i in $(seq 1 90); do
  if curl -sf http://127.0.0.1:8080/health >/dev/null 2>&1; then READY=1; break; fi
  sleep 1
done
if [ "$READY" -ne 1 ]; then echo "ERRO: modelo não iniciou."; tail -n 50 "$LOG/llama.log"; exit 1; fi

echo "[2/3] Iniciando Core privado..."
nohup node "$CORE" > "$LOG/core.log" 2>&1 &
READY=0
for i in $(seq 1 20); do
  if curl -sf http://127.0.0.1:8765/api/health >/dev/null 2>&1; then READY=1; break; fi
  sleep 1
done
if [ "$READY" -ne 1 ]; then echo "ERRO: Core não iniciou."; tail -n 50 "$LOG/core.log"; exit 1; fi

echo "[3/3] Iniciando gateway Wi-Fi protegido..."
nohup node "$GATEWAY" > "$LOG/gateway.log" 2>&1 &
sleep 2
if ! pgrep -f "$ROOT/core/server/lan-gateway.js" >/dev/null; then
  echo "ERRO: gateway não iniciou."; tail -n 50 "$LOG/gateway.log"; exit 1
fi

echo ""
echo "======================================"
echo "             NOX 3.0 ONLINE"
echo "======================================"
echo "Core privado: 127.0.0.1:8765"
echo "Gateway Wi-Fi: porta 8766"
echo "Token e IPs:   $LOG/gateway.log"
echo "Logs:          $LOG/"
echo "======================================"
echo "No Redmi 8, use http://IP_DO_REDMI_15C:8766"
echo "O token de pareamento está no gateway.log."

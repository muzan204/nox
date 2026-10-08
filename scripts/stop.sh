#!/data/data/com.termux/files/usr/bin/bash
ROOT="$HOME/nox"
pkill -f "$ROOT/ai/llama.cpp/build/bin/llama-server" 2>/dev/null || true
pkill -f "$ROOT/core/server/server.js" 2>/dev/null || true
termux-wake-unlock 2>/dev/null || true
echo "NOX encerrado."

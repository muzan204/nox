#!/data/data/com.termux/files/usr/bin/bash
# NOX 0.4 — instalação base otimizada para Redmi 15C / Termux
set -e

MODEL_SIZE="${1:-0.5b}"
ROOT="$HOME/nox"

if [ "$MODEL_SIZE" != "0.5b" ]; then
  echo "Este instalador foi ajustado para o Redmi 15C."
  echo "Use 0.5b para a configuração recomendada."
  echo "1.5b/3b ficam fora do perfil padrão porque já houve pressão de memória no aparelho."
  exit 1
fi

echo ""
echo "=============================================="
echo " NOX 0.4 — Redmi 15C + Termux"
echo " Modelo: Qwen2.5 0.5B Q4_K_M"
echo "=============================================="
echo ""

pkg update -y
pkg install -y git cmake clang wget curl python

termux-setup-storage || true

mkdir -p "$ROOT"/{core/server,ai/{inference,prompts},models,memory,tools,config,logs,scripts,ui}

echo "[1/4] Compilando/atualizando llama.cpp..."
cd "$ROOT/ai"

if [ ! -d llama.cpp ]; then
  git clone --depth=1 https://github.com/ggml-org/llama.cpp
fi

cd llama.cpp
cmake -B build -DCMAKE_BUILD_TYPE=Release
cmake --build build --config Release -j4

echo "[2/4] Preparando modelo..."
MODEL="qwen2.5-0.5b-instruct-q4_k_m.gguf"
URL="https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/$MODEL"

if [ ! -f "$ROOT/models/$MODEL" ]; then
  wget -O "$ROOT/models/$MODEL" "$URL"
else
  echo "Modelo já existe. Não vou baixar novamente."
fi

echo "[3/4] Instalando arquivos do NOX..."
echo "Os arquivos desta pasta devem ser copiados pelo usuário para:"
echo "  $ROOT/config/nox.config.json"
echo "  $ROOT/core/server/server.js"
echo "  $ROOT/ui/face.html"
echo "  $ROOT/scripts/start.sh"
echo "  $ROOT/scripts/stop.sh"
echo "  $ROOT/scripts/ask.sh"

echo "[4/4] Verificação..."
"$ROOT/ai/llama.cpp/build/bin/llama-server" --version

echo ""
echo "BASE CONCLUÍDA."
echo "Depois rode: ~/nox/scripts/start.sh"
echo "Abra: http://127.0.0.1:8765/"

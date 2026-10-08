#!/data/data/com.termux/files/usr/bin/bash
set -e

TEXT="$1"

if [ -z "$TEXT" ]; then
    exit 1
fi

termux-tts-speak \
    -l pt-BR \
    -r 0.95 \
    -p 1.0 \
    "$TEXT"

#!/data/data/com.termux/files/usr/bin/bash
# Opcional: copie para ~/.termux/boot/ após instalar Termux:Boot.
LOG="$HOME/nox/logs/boot.log"
mkdir -p "$(dirname "$LOG")"
sleep 8
"$HOME/nox/scripts/start.sh" >> "$LOG" 2>&1

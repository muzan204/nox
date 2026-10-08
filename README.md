# NOX 0.4 — Redmi 15C + Termux

Configuração recomendada:
- Qwen2.5 0.5B Instruct Q4_K_M
- llama.cpp em ARM64
- contexto 1024
- 4 threads
- `-lm none`
- máximo de 48 tokens por resposta
- Core Node.js em `127.0.0.1:8765`
- llama-server em `127.0.0.1:8080`

## Estrutura

- `config/nox.config.json`
- `core/server/server.js`
- `ui/face.html`
- `scripts/start.sh`
- `scripts/stop.sh`
- `scripts/ask.sh`

## Instalação

Se o llama.cpp já terminou de compilar, não precisa recompilar agora.

Copie os arquivos para:

```text
~/nox/config/nox.config.json
~/nox/core/server/server.js
~/nox/ui/face.html
~/nox/scripts/start.sh
~/nox/scripts/stop.sh
~/nox/scripts/ask.sh
```

Depois:

```bash
chmod +x ~/nox/scripts/*.sh
~/nox/scripts/start.sh
```

Teste:

```bash
curl -s http://127.0.0.1:8765/api/status
~/nox/scripts/ask.sh "Olá NOX"
```

Interface:

```text
http://127.0.0.1:8765/
```

## Importante

Não use o Qwen 1.5B ou 3B como configuração padrão neste aparelho. O objetivo desta versão é estabilidade e menor pressão de memória.

# NOX 1.0

Assistente pessoal local para Android/Termux, com carinha nativa, voz, memória local, comandos seguros e conexão opcional com computador.

## Arquitetura

```
📱 NOX Android
├── 😶 carinha nativa
├── 👆 toque → microfone
├── 🎙️ palavra-chave “NOX”
├── 🔊 ElevenLabs via Core
└── 💾 memória local (SQLite)

        │ Wi-Fi / LAN
        ▼

💻 NOX Core
├── 🧠 llama.cpp + Qwen
├── 🛠️ comandos autorizados
├── 🎙️ STT quando executado no Termux
└── 🌐 API HTTP
```

O navegador deixa de ser a interface principal. A pasta `android/` contém o aplicativo Android nativo.

## Recursos

- carinha limpa, sem chat/botões permanentes;
- toque em qualquer lugar da carinha para falar;
- palavra de ativação **NOX**;
- conexão com o Core no próprio aparelho ou em um PC da mesma rede;
- voz ElevenLabs pelo endpoint `/api/voice`;
- comandos com allowlist;
- SQLite como banco planejado para memória;
- Core Node.js + llama.cpp;
- Qwen 0.5B Q4_K_M como configuração leve para ARM64.

## Android

Projeto em `android/`.

Com Gradle instalado:

```bash
cd ~/nox/android
gradle :app:assembleDebug
```

APK:

```
android/app/build/outputs/apk/debug/app-debug.apk
```

No primeiro uso, conceda acesso ao microfone.

### Conectar ao PC

Na tela do NOX, mantenha a carinha pressionada por aproximadamente 1 segundo para abrir a configuração do Core.

Informe, por exemplo:

```
http://192.168.1.10:8765
```

O PC e o Redmi precisam estar na mesma rede. O Core deve escutar em `0.0.0.0:8765`.

**Não exponha essa porta diretamente à internet.** Use apenas rede local ou uma camada autenticada/VPN.

## Core Termux

```bash
chmod +x ~/nox/scripts/*.sh
~/nox/scripts/start.sh
```

Verificação:

```bash
curl -s http://127.0.0.1:8765/api/health
curl -s http://127.0.0.1:8765/api/status
```

## Comandos

A API de comandos usa uma allowlist. Nesta primeira versão:

```
status
pwd
git status
```

Comandos arbitrários de shell não são aceitos pela IA.

## Voz

Configure no arquivo `config/.env`:

```
ELEVENLABS_API_KEY=...
ELEVENLABS_VOICE_ID=...
ELEVENLABS_MODEL_ID=eleven_multilingual_v2
```

O Android envia a resposta do Core para `/api/voice` e reproduz o áudio retornado.

## Memória

A arquitetura 1.0 reserva SQLite para:

- conversas;
- preferências;
- projetos;
- fatos autorizados pelo usuário.

A memória não deve salvar automaticamente tudo o que o usuário fala.

## Estrutura

```
nox/
├── ai/
├── android/
├── commands/
├── config/
├── core/
│   └── server/
├── memory/
├── scripts/
└── ui/
```

A `ui/face.html` continua disponível como fallback/diagnóstico, mas não é mais a interface principal.

## Status

**NOX 1.0 foundation**

A camada Android, conexão Core/PC, voz, palavra-chave e comandos seguros estão preparadas. A próxima etapa é finalizar o banco SQLite de memória e evoluir as ferramentas autorizadas.

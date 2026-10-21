# NOX 1.3

Assistente pessoal local para Android/Termux, com carinha nativa, voz, memória local, comandos seguros e conexão opcional com computador.

## Web (nova etapa)

O projeto também inclui uma interface para navegador, otimizada para celular, com IA online, voz do navegador, histórico e memórias locais e login protegido.

- Interface: `index.html`
- Funções de sessão e IA: `api/session.js`, `api/chat.js`
- Cabeçalhos de segurança: `vercel.json`
- Instruções e variáveis de ambiente: [NOX-WEB.md](NOX-WEB.md)

A versão web é independente do Core local: ela não executa comandos do Android/Termux e não usa automaticamente o modelo Qwen local. Configure as variáveis de ambiente necessárias na Vercel antes de testar. Não considere o site operacional até validar um deployment e a chave do provedor.

## Arquitetura local

```
📱 NOX Android
├── 😶 carinha nativa
├── 👆 toque → microfone
├── 🎙️ palavra-chave “NOX” (serviço em segundo plano)
├── 🔊 voz nativa do Android (TextToSpeech)
└── 💾 memória persistente no Core (SQLite)

        │ Wi-Fi / LAN
        ▼

💻 NOX Core
├── 🧠 llama.cpp + Qwen
├── 🛠️ comandos autorizados
├── 🎙️ STT quando executado no Termux
└── 🌐 API HTTP
```

O navegador deixa de ser a interface principal do aplicativo Android nativo, mas agora existe também uma versão web dedicada na raiz do repositório. A pasta `android/` continua preservada.

## Recursos

### Evolução 1.2

- personalidade determinística para identidade, criador, saudações e agradecimentos;
- chat geral com respostas mais completas e naturais;
- histórico curto de conversa para manter referências entre mensagens;
- busca online automática para perguntas atuais, com timeout e contexto enviado ao Qwen;
- estado facial `SURPRISED` adicionado ao Android;
- endpoint `GET /api/capabilities` para descoberta das capacidades do Core;
- status do Core com versão do Node, plataforma, arquitetura, uptime e quantidade de memórias;
- ferramentas adicionais para Node.js, plataforma, hora, Git remoto e informações do projeto;
- base preparada para evoluir comandos em módulos de habilidades (skills), mantendo a allowlist e a segurança.

- carinha nativa com estados IDLE, LISTENING, THINKING, SPEAKING, HAPPY, CONFUSED e ERROR;
- toque na carinha para falar e resposta por TTS do Android;
- toque longo para configurar o endereço do NOX Core;
- conexão PC/Redmi por Wi-Fi/LAN;
- memória persistente em SQLite com comandos explícitos "lembre que..." e "esqueça...";
- contexto de memória autorizado enviado ao Qwen;
- monitoramento de CPU, RAM, disco, rede, processos, uptime e hostname;
- ferramentas de Git e descoberta de projetos;
- abertura segura do VS Code, pasta do NOX e navegador;
- ações perigosas separadas e protegidas por confirmação explícita;
- ElevenLabs opcional no Core, com voz nativa como fallback;
- comandos naturais com allowlist; comandos arbitrários de shell continuam bloqueados.

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

## Comandos naturais

O NOX reconhece frases em português e executa apenas ferramentas autorizadas. Exemplos:

```
"quanto de RAM eu tenho?"
"qual a CPU?"
"qual o IP do computador?"
"mostre os processos"
"liste meus projetos"
"qual a branch atual?"
"abra o VS Code"
"lembre que meu projeto principal é o NOX"
"o que você lembra?"
"esqueça meu projeto principal"
```

Ações como desligar ou reiniciar **nunca são executadas imediatamente**. O NOX pede uma confirmação explícita.

## Memória local

A memória do Core usa SQLite e só recebe informações quando o usuário pede explicitamente para lembrar. APIs existentes:

```
GET  /api/memory
GET  /api/memory?q=projeto
POST /api/memory
```

Na versão web, histórico e memórias ficam no armazenamento local do navegador e não sincronizam entre dispositivos. As memórias web são enviadas ao provedor de IA junto com a mensagem; não salve informações secretas ou sensíveis.

## Voz

Configure no arquivo `config/.env` para a voz ElevenLabs do Core:

```
ELEVENLABS_API_KEY=...
ELEVENLABS_VOICE_ID=...
ELEVENLABS_MODEL_ID=eleven_multilingual_v2
```

O Core expõe `/api/voice` para a UI web antiga `ui/face.html`. O aplicativo Android usa a voz nativa do Android (TextToSpeech) como caminho principal. A nova versão web usa as APIs de voz disponíveis no navegador.

## Estrutura

```
nox/
├── ai/
├── android/
├── api/           # funções serverless da versão web
├── config/
├── core/
│   └── server/
├── memory/
├── scripts/
├── ui/            # interface legada/fallback do Core
├── index.html     # nova interface web
├── vercel.json
└── NOX-WEB.md
```

## Status

**NOX 1.3 — Core local + versão web em desenvolvimento**

A camada Android, conexão Core/PC, voz, palavra-chave e comandos seguros continuam preservados. A versão web foi adicionada em uma branch de implementação e ainda precisa de configuração das variáveis e validação em deployment Vercel antes de ser considerada pronta para uso.

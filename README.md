# ◉ NOX 3.1

**Assistente pessoal local por voz, com dois celulares conectados pela rede Wi-Fi.**

## Arquitetura

- **Redmi 15C (servidor):** Termux, Core Node.js, modelo local llama.cpp, memória SQLite e gateway autenticado.
- **Redmi 8 (interface):** APK Android com olhos e boca neon, microfone, reconhecimento de fala e voz sintetizada.
- **Transporte:** HTTP autenticado para chat e WebSocket autenticado para estado/monitoramento, com reconexão automática.

O Core e o llama.cpp ficam vinculados ao loopback do Redmi 15C. Apenas o gateway móvel escuta na rede local. Não exponha as portas 8765 ou 8766 à internet.

## Requisitos do servidor (Redmi 15C)

- Termux e Node.js compatível com `node:sqlite` (Node 22.5+; recomendado Node LTS atual).
- Build ARM64 do `llama-server`.
- Modelo GGUF Q4 pequeno, padrão Qwen2.5 0.5B.
- Os arquivos do projeto instalados em `$HOME/nox`.

### Iniciar

```bash
chmod +x ~/nox/scripts/*.sh
~/nox/scripts/start.sh
```

O script inicia o modelo local em `127.0.0.1:8080`, o Core privado em `127.0.0.1:8765` e o gateway Wi-Fi em `0.0.0.0:8766`.

Consulte `~/nox/logs/gateway.log` para obter o endereço IP local e o token de pareamento. O token é gerado automaticamente e salvo em `config/device.token` com permissões restritas.

### Parar

```bash
~/nox/scripts/stop.sh
```

### Inicialização opcional com Termux:Boot

Instale o app Termux:Boot pela mesma fonte do Termux, abra-o uma vez e copie o script:

```bash
mkdir -p ~/.termux/boot
cp ~/nox/scripts/termux-boot.sh ~/.termux/boot/nox
chmod +x ~/.termux/boot/nox
```

O Android pode limitar processos em segundo plano e otimização de bateria. O boot script não contorna essas regras; configure a bateria do Termux para não restringir o app.

## Parear o Redmi 8

1. Inicie o servidor no Redmi 15C.
2. Copie o IP e o token de `~/nox/logs/gateway.log`.
3. No APK NOX do Redmi 8, informe `http://IP_DO_REDMi_15C:8766` e o token.
4. Mantenha os dois aparelhos na mesma rede Wi-Fi.

O token é local e não é uma API key. NVIDIA/cloud é opcional e não é necessário para o modo local. Serviços de IA hospedados podem exigir conta, chave e/ou cota; o projeto não tenta contornar essas exigências.

## Monitoramento

- `GET /api/device-status` (autenticado): RAM, CPU, uptime, IPs e número de clientes WebSocket.
- WebSocket `/ws` (autenticado): snapshot de estado facial e status do Core a cada 2,5 segundos.
- Core SQLite: memórias pessoais e conhecimento web com fontes em `memory/nox.db`.

## Aprendizado contínuo com a internet

O NOX 3.1 pode pesquisar tópicos configurados automaticamente e guardar conteúdo textual com URL e data de consulta no SQLite. A cada seis horas, por padrão, ele pesquisa até dois tópicos; o primeiro ciclo começa um minuto depois da inicialização. O limite padrão é de 150 páginas, até dois resultados por tópico e 4.200 caracteres por página.

A configuração fica em `config/nox.config.json`, na seção `webLearning`. O Redmi 15C precisa estar conectado à internet. O aprendizado é recuperação de informação (RAG): o Qwen não altera seus pesos nem se retreina automaticamente. O conteúdo externo é tratado como dado não confiável e deve ser conferido nas fontes antes de decisões importantes.

Rotas autenticadas no gateway:
- `GET /api/learning`: estado do aprendizado e data do último ciclo.
- `POST /api/learning/run`: solicita um ciclo manual.
- `GET /api/knowledge?q=termo`: consulta o conhecimento salvo.

## Segurança

- O Core e llama.cpp ficam apenas em localhost.
- O gateway só encaminha rotas móveis permitidas.
- Toda chamada móvel exige `X-NOX-Token`.
- O token não deve ser publicado nem enviado a terceiros.
- Use somente em rede Wi-Fi confiável; não crie redirecionamento de porta no roteador.

## Build Android

Abra `android/` no Android Studio e gere o APK. O app usa OkHttp para WebSocket. Nenhum deploy é feito automaticamente.

## Pastas

- `core/server/`: Core, memória, comandos e gateway LAN.
- `android/`: interface nativa.
- `ai/llama.cpp/`: motor de inferência local.
- `models/`: modelo GGUF local.
- `config/`: configuração e token privado.
- `scripts/`: iniciar, parar e boot opcional.
- `site/`: interface web legada separada do modo de dois celulares.

# ◉ NOX

<p align="center">
  <strong>Seu assistente pessoal. Local quando precisa, web quando quiser.</strong><br>
  Android · Termux · Google Gemini · Vercel
</p>

<p align="center">
  <a href="https://github.com/muzan204/nox">Repositório</a> ·
  <a href="https://github.com/muzan204/nox/tree/feat/nox-web-vercel/site">NOX Web</a> ·
  <a href="https://github.com/muzan204/nox/pulls">Pull requests</a>
</p>

---

## ✦ Sobre

O **NOX** é um assistente pessoal em evolução, com duas partes separadas:

- **NOX Web** — interface para conversar no navegador, com IA online Gemini, voz do navegador e memória local.
- **NOX Local** — projeto Android/Termux com interface nativa, voz, memória persistente e ferramentas locais autorizadas.

A versão web foi organizada em uma pasta independente para facilitar a configuração da Vercel sem misturar seus arquivos com o projeto Android/Termux.

## ✧ NOX Web

<p align="center">
  <img src="https://img.shields.io/badge/Interface-Web-0f766e?style=for-the-badge" alt="Interface Web">
  <img src="https://img.shields.io/badge/IA-Google%20Gemini-4285F4?style=for-the-badge" alt="Google Gemini">
  <img src="https://img.shields.io/badge/Deploy-Vercel-black?style=for-the-badge" alt="Vercel">
</p>

### Recursos

- Interface responsiva para celular e desktop.
- Conversa com Gemini por função serverless; a chave fica no servidor.
- Entrada por voz e leitura em voz alta, quando disponíveis no navegador.
- Histórico e memórias guardados localmente no navegador.
- Opções para limpar e exportar a conversa e apagar memórias.

### Publicar na Vercel

1. Importe o repositório `muzan204/nox` na Vercel.
2. Em **Settings → Build and Deployment → Root Directory**, escolha **`site`**.
3. Em **Settings → Environment Variables**, configure:
   - `GEMINI_API_KEY` — chave privada criada no Google AI Studio.
   - `GEMINI_MODEL` — opcional; padrão `gemini-2.5-flash`.
4. Faça o deploy e teste as mensagens e os recursos de voz.

> **Importante:** não coloque a chave Gemini no HTML, em variáveis públicas ou no GitHub. O site não exige login; qualquer pessoa com o endereço pode consumir a quota da API. Configure quotas e alertas antes de divulgar o endereço. O limitador atual é básico e não substitui proteção distribuída.

Guia completo: [`site/README.md`](site/README.md).

## ✧ NOX Local — Android e Termux

A versão local permanece no repositório. Ela não é necessária para publicar o site na Vercel.

### Arquitetura

```text
NOX Android
├── Interface nativa
├── Microfone e voz do Android
└── Conexão opcional com o Core
          │ Wi-Fi / LAN
          ▼
NOX Core (Termux / computador)
├── API HTTP
├── Memória persistente SQLite
├── Ferramentas autorizadas
└── llama.cpp + modelo local (quando configurado)
```

### Iniciar o Core no Termux

```bash
chmod +x ~/nox/scripts/*.sh
~/nox/scripts/start.sh
```

Verificar se está respondendo:

```bash
curl -s http://127.0.0.1:8765/api/health
curl -s http://127.0.0.1:8765/api/status
```

### Compilar o Android

Com o ambiente Android/Gradle configurado:

```bash
cd ~/nox/android
gradle :app:assembleDebug
```

O projeto Android está em `android/`. A versão web está em `site/`; as duas partes não precisam ser publicadas juntas.

**Segurança:** não exponha a API do Core diretamente à internet. Use uma rede local confiável ou uma camada de acesso segura.

## ✧ Estrutura do repositório

```text
nox/
├── site/                   # Aplicação web pronta para configurar na Vercel
│   ├── api/chat.js          # Endpoint serverless Gemini
│   ├── index.html           # Interface web
│   ├── vercel.json          # Cabeçalhos de segurança e configuração
│   └── README.md            # Guia de deploy da versão web
├── android/                # Aplicativo Android nativo
├── core/server/             # API e lógica do Core local
├── config/                  # Configuração e exemplo de ambiente
├── scripts/                 # Inicialização e utilitários Termux
├── ui/                      # Interface auxiliar/legada do Core
├── voice/                   # Recursos de voz do Core
└── README.md                # Documentação principal
```

## ✦ Status

- [x] Interface web separada em `site/`
- [x] Endpoint Gemini movido para `site/api/chat.js`
- [x] Configuração Vercel dentro de `site/`
- [x] Documentação principal reorganizada
- [ ] Configurar `GEMINI_API_KEY` na Vercel
- [ ] Fazer deploy real e testar no navegador

O deploy ainda precisa ser executado e validado. O código no GitHub, por si só, não significa que o site já esteja online.

---

<p align="center"><sub>NOX · Feito para evoluir, do celular à nuvem.</sub></p>

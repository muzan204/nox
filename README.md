# ◉ NOX

**Assistente pessoal por voz, com interface neon e IA na nuvem.**  
Android · Termux · Vercel · Qwen3.8-27B

## NOX Web

A aplicação web fica em site/ e pode ser publicada na Vercel. A interface apresenta dois olhos ciano e boca animada sobre fundo preto, com entrada por voz, chat e leitura das respostas em voz alta quando o navegador oferece suporte.

### Publicar na Vercel

1. Importe o repositório muzan204/nox.
2. Defina **Root Directory** como **site**.
3. Em **Settings → Environment Variables**, configure:
   - **DASHSCOPE_API_KEY** — chave privada da Alibaba Cloud Model Studio (região internacional).
   - **QWEN_MODEL** — opcional; padrão qwen3.8-27b.
   - **QWEN_API_URL** — opcional; padrão https://dashscope-intl.aliyuncs.com/compatible-mode/v1.
4. Faça o deploy e teste as mensagens e o microfone em HTTPS.

**Atenção:** o Qwen na nuvem pode gerar cobrança por tokens. Confira os preços e limites do provedor antes de usar. Nunca salve a chave no HTML ou no GitHub. O site não tem autenticação por padrão; antes de divulgar o endereço, adicione proteção contra uso não autorizado.

Guia da versão web: site/README.md.

## NOX Local — Android e Termux

A parte local permanece separada da versão web. A arquitetura do Core pode rodar no Termux e usar modelo local quando configurado; ela não é necessária para publicar a interface web.

Para iniciar o Core (com o ambiente local já instalado):

    chmod +x ~/nox/scripts/*.sh
    ~/nox/scripts/start.sh
    curl -s http://127.0.0.1:8765/api/health
    curl -s http://127.0.0.1:8765/api/status

Não exponha o Core do Termux diretamente à internet.

## Estrutura

    nox/
    ├── site/        # Interface web e API serverless Qwen
    ├── android/     # Aplicativo Android nativo
    ├── core/        # API e lógica local
    ├── config/      # Configuração
    ├── scripts/     # Utilitários Termux
    ├── ui/          # Interface auxiliar/legada
    └── voice/       # Recursos de voz

## Status

- [x] Interface web com olhos e boca neon.
- [x] Integração serverless com Qwen3.8-27B.
- [x] Entrada por voz e fala sintetizada, quando suportadas.
- [ ] Configurar DASHSCOPE_API_KEY na Vercel.
- [ ] Fazer deploy e testar com uma chave válida.

O código no GitHub não significa que o deploy já foi executado ou validado.

# NOX Core local (Termux)

Este serviço é um adaptador opcional para um servidor de modelo local compatível com a API OpenAI, como o endpoint local do llama.cpp. Ele não substitui a API online hospedada na Vercel.

## Requisitos

- Node.js 18 ou superior no Termux.
- Um servidor de inferência local compatível com `POST /v1/chat/completions`, escutando em `127.0.0.1:8080`.
- Nunca coloque chaves da IA online neste serviço nem no navegador.

## Configuração

Na pasta `site/termux-core`, defina um token aleatório apenas para o serviço local e inicie-o:

```sh
export NOX_LOCAL_TOKEN="$(node -e 'console.log(require("node:crypto").randomBytes(32).toString("hex"))')"
export NOX_HOST=127.0.0.1
export NOX_PORT=8765
export NOX_LOCAL_MODEL_URL=http://127.0.0.1:8080/v1/chat/completions
export NOX_LOCAL_MODEL=local-model
node server.mjs
```

O token é segredo: não o publique, não o coloque no Git e não o embuta em JavaScript do site. O servidor fica vinculado a localhost por padrão e exige `Authorization: Bearer <token>` em `POST /chat`. A rota `GET /health` fornece apenas um status local.

## Arquitetura híbrida

- Interface: `site/index.html` (olhos e boca ciano, fundo preto, voz do navegador).
- IA online: `site/api/chat.js` na Vercel, com `GEMINI_API_KEY` configurada como variável privada.
- IA local: este adaptador, ligado a um modelo local que implemente o endpoint de chat compatível.
- Fallback: a aplicação pode usar a IA online se o serviço local estiver indisponível.

**Estado da integração:** este adaptador é uma base local independente. O navegador ainda não foi conectado a ele automaticamente; isso exige definir uma estratégia segura de pareamento entre a página HTTPS e o serviço localhost, sem expor o token no código público. Não exponha a porta 8765 à internet. HTTPS, CORS e permissões de microfone precisam ser validados no navegador real.

## Limites

- O adaptador valida método, rota, tamanho aproximado do corpo e mensagens; não substitui um gateway de segurança para exposição pública.
- O serviço local depende de um servidor de modelo já iniciado.
- Não foram executados testes neste ambiente nem realizado deploy da Vercel.

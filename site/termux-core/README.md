# NOX Core local (Termux)

Este serviço é um adaptador opcional para um servidor de modelo local compatível com a API Chat Completions, como llama.cpp. A documentação central dos provedores fica em [../.. /brain/README.md](../../../../brain/README.md).

## Requisitos

- Node.js 18 ou superior no Termux.
- Servidor local compatível com `POST /v1/chat/completions` em `127.0.0.1:8080`.
- Modelo local já baixado e servidor de inferência iniciado.

## Configuração

Na pasta `site/termux-core`:

```sh
export NOX_LOCAL_TOKEN="$(node -e 'console.log(require("node:crypto").randomBytes(32).toString("hex"))')"
export NOX_HOST=127.0.0.1
export NOX_PORT=8765
export NOX_LOCAL_MODEL_URL=http://127.0.0.1:8080/v1/chat/completions
export NOX_LOCAL_MODEL=local-model
node server.mjs
```

O token é segredo. Não o publique, não o coloque no Git e não o embuta no JavaScript do site. O servidor fica vinculado a localhost por padrão e exige `Authorization: Bearer <token>` em `POST /chat`.

## Limite importante de rede

A Vercel não pode acessar o localhost do celular. Se o site público chamar a API da Vercel, a variável `NOX_COMPAT_BASE_URL=http://127.0.0.1:...` apontará para a própria função hospedada, não para o Termux. Portanto, este adaptador local ainda não está ligado automaticamente ao roteador online. Para usar o cérebro local do telefone com o site hospedado, será necessário um canal privado e autenticado de pareamento/conectividade; não exponha a porta diretamente na internet.

Consulte `brain/README.md` na raiz do repositório para provedores online, fallback e variáveis de ambiente. Testes e deploy não foram realizados neste ambiente.

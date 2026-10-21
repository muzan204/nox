# NOX Web

Interface web do NOX, preparada para deploy na Vercel com diretório raiz `site/`.

## O que inclui

- Interface responsiva com identidade visual escura verde/aqua.
- Chat com Google Gemini via função serverless.
- Entrada por voz e leitura em voz alta, quando suportadas pelo navegador.
- Histórico e memórias locais no navegador.
- Cabeçalhos de segurança, validação de entrada e timeout.

## Deploy na Vercel

1. Importe o repositório `muzan204/nox`.
2. Em **Settings → Build and Deployment → Root Directory**, selecione `site`.
3. Em **Settings → Environment Variables**, adicione:
   - `GEMINI_API_KEY`: sua chave privada do Google AI Studio.
   - `GEMINI_MODEL`: opcional; padrão `gemini-2.5-flash`.
4. Faça o deploy e teste o chat e os recursos de voz no endereço publicado.

Não coloque a chave no HTML, no GitHub ou em variáveis públicas. Após alterar variáveis, faça um novo deploy.

## Estrutura

```
site/
├── api/
│   └── chat.js
├── index.html
├── vercel.json
└── README.md
```

## Privacidade e custos

O site não exige login. Qualquer pessoa que tenha o endereço pode usar o endpoint e consumir a quota/custo associado à chave Gemini. Configure quotas e alertas no Google AI Studio/Google Cloud antes de divulgar o site. O limitador em memória é básico e não substitui proteção distribuída.

O histórico e as memórias são guardados no armazenamento local deste navegador. As memórias escolhidas são enviadas à IA junto com a mensagem; não guarde informações secretas ou sensíveis.

## Limites

A versão web não executa comandos do Android/Termux e não usa automaticamente o modelo Qwen local. Voz depende das APIs disponíveis no navegador e pode variar entre dispositivos.

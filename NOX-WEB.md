# NOX Web (Vercel)

Esta etapa adiciona uma interface web mobile-first sem remover o Core Android/Termux. A página raiz usa funções serverless para a IA online e APIs próprias para sessão.

## Recursos
- Interface responsiva para navegador de celular.
- Conversa com provedor compatível com Chat Completions.
- Voz do navegador: reconhecimento de fala quando disponível e leitura em voz alta.
- Histórico e memórias explícitas no navegador via localStorage.
- Login por senha com cookie assinado, HttpOnly, SameSite=Lax e Secure em HTTPS.
- Chave de IA somente no servidor; validação de entrada, verificação de origem, timeout, limite de requisições por instância e cabeçalhos de segurança.

## Variáveis de ambiente
Configure em Vercel → Project → Settings → Environment Variables e faça um novo deployment depois de alterar variáveis:
- NOX_ACCESS_PASSWORD: senha longa, com pelo menos 12 caracteres.
- NOX_SESSION_SECRET: segredo aleatório com pelo menos 32 caracteres.
- OPENAI_API_KEY: chave privada do provedor.
- OPENAI_MODEL: opcional; padrão gpt-4o-mini.
- OPENAI_BASE_URL: opcional; padrão https://api.openai.com/v1. Pode ser endpoint compatível com OpenAI.

Nunca coloque chaves no HTML, no Git ou em variáveis públicas.

## Deploy
1. Importe muzan204/nox na Vercel e mantenha o diretório raiz na raiz do repositório.
2. Configure as variáveis acima.
3. Faça deploy de preview e teste login, mensagem, voz, histórico e logout antes de promover para produção.
4. Revise os logs da função se houver falha.

O endpoint de sessão falha fechado quando os segredos não estão configurados. O chat exige sessão válida. O limitador em memória reduz abuso casual, mas não é distribuído; para produção pública, adicione proteção distribuída e monitoramento. A senha é compartilhada entre usuários nesta versão inicial, portanto não há contas individuais.

## Limites conhecidos
- Memória e histórico são locais ao navegador; não sincronizam entre aparelhos e somem ao limpar os dados do site.
- Memórias são incluídas no contexto da IA quando uma mensagem é enviada; não salve dados secretos ou sensíveis.
- A fala depende das APIs e permissões do navegador. Reconhecimento de fala varia entre navegadores Android.
- Esta etapa não hospeda o Core Termux, não executa comandos do dispositivo e não usa o modelo Qwen local. Essas funções não devem ser expostas à internet sem redesenho de autorização.
- Ainda é necessário testar as funções em um deployment Vercel real.

## Teste local
A página pode ser servida como HTML, mas autenticação e funções /api/* precisam de runtime compatível com funções Vercel. Use a Vercel CLI (vercel dev) em ambiente Node.js com as variáveis configuradas para testar a aplicação completa.
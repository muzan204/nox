# NOX Web — Qwen3.8-27B

Interface de voz e chat do NOX, preparada para deploy na Vercel com o diretório raiz site/.

## Recursos

- Rosto digital com olhos e boca ciano animada em fundo preto.
- Conversa em português com Qwen3.8-27B hospedado na nuvem.
- Reconhecimento de fala e leitura em voz alta quando suportados pelo navegador.
- Histórico recente no navegador; até 12 mensagens são enviadas por requisição.
- Chave da API guardada somente em variável privada no servidor.
- Validação de entrada, timeout, cabeçalhos de segurança e limite básico de requisições.

## Configuração da Vercel

1. Importe muzan204/nox na Vercel.
2. Em **Settings → Build and Deployment → Root Directory**, defina **site**.
3. Em **Settings → Environment Variables**, crie:
   - **DASHSCOPE_API_KEY**: chave privada da Alibaba Cloud Model Studio (região internacional).
   - **QWEN_MODEL**: opcional; padrão qwen3.8-27b.
   - **QWEN_API_URL**: opcional; padrão https://dashscope-intl.aliyuncs.com/compatible-mode/v1.
4. Faça um novo deploy e teste o chat e o microfone.

Não coloque a chave no HTML, em variáveis públicas, nem no GitHub. Nunca compartilhe sua chave. Depois de alterar variáveis, faça novo deploy.

## Custos

O Qwen3.8-27B pela API na nuvem não é garantidamente gratuito. O preço depende do provedor, da região e dos tokens usados. Confira a tabela oficial atual antes de usar: https://www.alibabacloud.com/help/en/model-studio/qwen3-8-27b. Configure alertas/limites de gasto e não divulgue o endereço publicamente sem proteção adicional.

## Privacidade e segurança

- O navegador envia as mensagens recentes para a API da nuvem para obter respostas.
- O histórico fica no armazenamento local deste navegador. Use **Limpar conversa** para apagá-lo.
- Não guarde senhas, chaves, dados financeiros ou outras informações sensíveis no chat.
- O endpoint verifica a origem e possui um limitador simples por instância; isso não substitui autenticação nem rate limiting distribuído. Antes de abrir o site ao público, adicione uma camada de proteção e limites no provedor.
- O microfone e a voz dependem do navegador, de permissões e de HTTPS. O NOX não escuta continuamente em segundo plano.
- O site não executa comandos do Android/Termux nem acessa arquivos locais.

## Arquivos

- site/index.html — interface e lógica de voz.
- site/api/chat.js — proxy serverless para o Qwen.
- site/vercel.json — cabeçalhos de segurança.

## Desenvolvimento local

A função site/api/chat.js usa o runtime de funções da Vercel. Para testar localmente, instale a Vercel CLI e execute vercel dev dentro de site/. Configure DASHSCOPE_API_KEY no ambiente local sem adicionar o valor ao repositório.

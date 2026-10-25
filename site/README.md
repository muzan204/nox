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

## Cota gratuita do Qwen

A documentação atual do Alibaba Cloud lista cota gratuita de teste para novos usuários elegíveis em Singapore com escopo International, inclusive para alguns modelos Qwen. A disponibilidade, quantidade e validade dependem do modelo e da conta; confirme no console se `qwen3.8-27b` mostra cota gratuita antes de usar.

1. Abra o [Alibaba Cloud Model Studio](https://modelstudio.console.alibabacloud.com/) e conclua a configuração da conta.
2. Selecione **Singapore** e crie uma chave de API para o serviço internacional.
3. Na página de cota/uso do modelo, confirme a cota de `qwen3.8-27b`.
4. Ative **Free Quota Only**, se disponível, para que as chamadas parem quando a cota gratuita acabar, em vez de continuar com cobrança por uso.
5. Na Vercel, configure a chave apenas como variável privada `DASHSCOPE_API_KEY`.

Documentação oficial:
- [Cota gratuita para novos usuários](https://www.alibabacloud.com/help/en/model-studio/new-free-quota)
- [Preços e cota por modelo](https://www.alibabacloud.com/help/en/model-studio/model-pricing)

A cota não é ilimitada e pode expirar. Não use a API até confirmar a cota e ativar a proteção contra cobrança no painel, quando disponível. O código do NOX não consegue ativar essa opção por você.

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

# NOX Web (Vercel)

Interface mobile-first para conversar com NOX no navegador usando IA online do Google Gemini. O site fica aberto: não exige senha nem sessão de usuário.

## Recursos
- Interface responsiva para celular.
- IA online via Gemini API; a chave fica somente no servidor.
- Voz do navegador: reconhecimento quando disponível e leitura em voz alta.
- Histórico e memórias explícitas locais via localStorage, com opções para apagar/exportar.
- Validação de entradas, verificação de origem, timeout, limite básico por instância e cabeçalhos de segurança.

## Variáveis de ambiente
Na Vercel → Project → Settings → Environment Variables:
- `GEMINI_API_KEY`: chave privada criada no Google AI Studio.
- `GEMINI_MODEL`: opcional; padrão `gemini-2.5-flash`.

Nunca coloque a chave no HTML, no Git ou em variável pública. Após alterar variáveis, faça novo deployment.

## Deploy
1. Importe `muzan204/nox` na Vercel com o diretório raiz do repositório.
2. Configure `GEMINI_API_KEY` e, opcionalmente, `GEMINI_MODEL`.
3. Faça deploy de preview e teste mensagens, voz, histórico e memória.
4. Confira os logs se houver falha.

## Segurança e custos
Não há login, senha compartilhada ou cookie de sessão. Qualquer pessoa com o endereço pode enviar mensagens e consumir a quota/custo associado à chave Gemini. O limitador em memória é uma proteção básica, não distribuída. Antes de divulgar o site publicamente, configure quotas e alertas no provedor, limites de gastos quando disponíveis e proteção distribuída/firewall na Vercel. A verificação de origem não impede clientes automatizados. A chave permanece no servidor e não é enviada ao navegador.

## Memória e privacidade
- Histórico e memórias ficam neste navegador; não sincronizam entre aparelhos e somem ao limpar os dados do site.
- Memórias selecionadas são enviadas ao Gemini junto com as mensagens; não salve dados secretos ou sensíveis.
- A fala depende das APIs e permissões do navegador, e varia entre navegadores Android.

## Limites conhecidos
- A versão web não hospeda o Core Termux, não executa comandos do dispositivo e não usa automaticamente o modelo Qwen local.
- A aplicação precisa ser validada em um deployment Vercel real antes de ser considerada pronta.

## Teste local
Use um runtime compatível com funções Vercel (por exemplo, `vercel dev`). A página estática sozinha não inclui o endpoint de IA.

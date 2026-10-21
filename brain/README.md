# NOX Brain — camada de inteligência

O cérebro é responsável pela seleção de provedores, chaves privadas, fallback e integração com modelos. A interface não deve conter chaves de API nem credenciais de modelos.

## Provedores suportados pelo roteador online

Configure os provedores que você realmente possui acesso. Não existe uma chave universal que habilite todos os modelos; disponibilidade, custos, limites e termos dependem de cada serviço.

### Gemini (Google)
- `GEMINI_API_KEY`
- Opcional: `GEMINI_MODEL` (padrão: `gemini-2.5-flash`)

### OpenRouter
- `OPENROUTER_API_KEY`
- `OPENROUTER_MODEL` (nome exato de um modelo disponível na sua conta)

### Qualquer endpoint compatível com OpenAI Chat Completions
- `NOX_COMPAT_BASE_URL` — URL base, sem `/chat/completions`; use HTTPS para endpoints remotos ou localhost para modelos locais no mesmo ambiente.
- `NOX_COMPAT_MODEL`
- `NOX_COMPAT_API_KEY`
- Opcional: `NOX_COMPAT_PROVIDER`

Essa interface compatível pode ser usada com um servidor Ollama configurado para uma API compatível, llama.cpp server, LM Studio ou outro serviço que aceite `POST /v1/chat/completions` (a URL base deve terminar antes de `/chat/completions`). Verifique o endpoint específico do seu servidor.

## Fallback

A ordem de tentativa é:
1. Endpoint compatível configurado em `NOX_COMPAT_*`
2. OpenRouter, se configurado
3. Gemini, se configurado

O NOX tenta o próximo provedor quando o anterior falha. A primeira resposta válida encerra a seleção. Não há roteamento inteligente por tarefa nem varredura automática de catálogo nesta versão.

## Modelo local no Termux

O cérebro local pode rodar em Termux com um servidor compatível em `127.0.0.1`. Um serviço na Vercel **não consegue acessar automaticamente o localhost do celular**: localhost na Vercel significa o próprio ambiente da Vercel. Para conversar com um modelo no telefone, a interface precisa estar na mesma máquina/rede com uma estratégia segura de pareamento, ou usar uma conexão privada autenticada. Não exponha o servidor local diretamente à internet nem coloque o token local no JavaScript público.

O adaptador de Termux está em `site/termux-core` nesta etapa; a separação completa em implantação independente ainda requer configurar o backend e a URL da interface.

## Segredos e operação

- Configure chaves apenas como variáveis privadas no ambiente de hospedagem do cérebro.
- Nunca comite arquivos `.env`, tokens ou chaves.
- Use HTTPS para endpoints remotos.
- Não envie segredos do provedor ao navegador.
- Modelos locais exigem download, armazenamento, RAM/CPU e servidor de inferência próprios.
- Este documento descreve a configuração; não significa que os provedores foram conectados, que há chaves configuradas ou que o deploy/testes ocorreram.

# NOX Android

Aplicativo nativo do NOX. A carinha não depende mais do navegador.

## Comportamento

- toque em qualquer parte da tela, ou diga “NOX”, para abrir o microfone;
- toque longo (~1s) na carinha abre a configuração do endereço do Core;
- envia a fala para o NOX Core e lê a resposta com a voz nativa do Android (TextToSpeech);
- o Core pode estar no próprio Termux ou em um PC pela rede;
- a memória persistente (fatos, histórico) fica no Core, em SQLite;
- exige permissão de microfone e, a partir do Android 13, de notificações
  (usada pelo aviso do serviço de escuta em segundo plano).

## Servidor

Por padrão usa:

`http://127.0.0.1:8765`

Para usar um computador na mesma rede, altere o endereço salvo pelo app para:

`http://IP_DO_PC:8765`

A camada de rede e o Core permanecem separados da UI Android.

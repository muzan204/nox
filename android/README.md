# NOX Android

Aplicativo nativo do NOX. A carinha não depende mais do navegador.

## Comportamento

- toque em qualquer parte da tela para abrir o microfone;
- envia a fala para o NOX Core;
- o Core pode estar no próprio Termux ou em um PC pela rede;
- memória local pode usar SQLite;
- a voz pode ser devolvida pelo Core/ElevenLabs.

## Servidor

Por padrão usa:

`http://127.0.0.1:8765`

Para usar um computador na mesma rede, altere o endereço salvo pelo app para:

`http://IP_DO_PC:8765`

A camada de rede e o Core permanecem separados da UI Android.

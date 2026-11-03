# NOX Android 3.0 — Interface (Redmi 8)

O aplicativo Android é a interface do NOX: rosto neon, microfone, reconhecimento de fala, síntese de voz e conexão com o servidor no Redmi 15C.

## Pareamento

1. No Redmi 15C, inicie o Core com `~/nox/scripts/start.sh`.
2. Abra `~/nox/logs/gateway.log` e copie o endereço IPv4 e o token de pareamento.
3. No Redmi 8, abra o NOX. No primeiro uso, ou com toque longo na face, informe:
   - URL, por exemplo `http://192.168.1.20:8766`;
   - token exibido no log do gateway.
4. Os dois aparelhos devem estar na mesma rede Wi-Fi.

O token é uma credencial local de pareamento, não uma chave de API. Não o compartilhe e não exponha a porta 8766 à internet.

## Recursos

- Chat HTTP protegido por `X-NOX-Token`.
- WebSocket autenticado em `/ws` para estado facial e monitoramento.
- Reconexão automática com espera progressiva.
- Fallback de consulta HTTP do estado.
- SpeechRecognizer e TextToSpeech em português do Brasil.

## Build

Abra a pasta `android/` no Android Studio e gere o APK. A primeira compilação precisa baixar a dependência OkHttp.

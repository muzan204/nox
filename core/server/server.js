const http = require("node:http");
const path = require("node:path");
const fs = require("node:fs");
const { spawn, execFile } = require("node:child_process");

const ROOT = path.resolve(__dirname, "..", "..");

const CONFIG_PATH = path.join(ROOT, "config", "nox.config.json");
const FACE_PATH = path.join(ROOT, "ui", "face.html");
const ENV_FILE = path.join(ROOT, "config", ".env");

const LISTEN_COMMAND = "termux-speech-to-text";

// ============================================================
// ENV
// ============================================================

function loadEnv() {
  if (!fs.existsSync(ENV_FILE)) {
    return;
  }

  const envText = fs.readFileSync(ENV_FILE, "utf8");

  for (const line of envText.split(/\r?\n/)) {
    const match = line.match(
      /^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*("?)(.*?)\2\s*$/
    );

    if (match && !process.env[match[1]]) {
      process.env[match[1]] = match[3];
    }
  }
}

loadEnv();

// ============================================================
// CONFIG
// ============================================================

function loadConfig() {
  const raw = fs
    .readFileSync(CONFIG_PATH, "utf8")
    .replace(/^\uFEFF/, "");

  return JSON.parse(raw);
}

const config = loadConfig();

const HOST = config.server?.host || "127.0.0.1";
const PORT = Number(config.server?.port || 8765);

const LLAMA_BASE_URL =
  config.llamaServer?.baseUrl || "http://127.0.0.1:8080";

const MAX_TOKENS = Number(config.llamaServer?.maxTokens || 48);
const TEMPERATURE = Number(config.llamaServer?.temperature || 0.7);

const SYSTEM_PROMPT =
  config.assistant?.systemPrompt ||
  "Você é NOX, assistente pessoal local do senhor Gustavo. Responda em português do Brasil.";

const ELEVENLABS_API_KEY = process.env.ELEVENLABS_API_KEY || "";
const ELEVENLABS_VOICE_ID = process.env.ELEVENLABS_VOICE_ID || "";
const ELEVENLABS_MODEL_ID =
  process.env.ELEVENLABS_MODEL_ID || "eleven_multilingual_v2";

// ============================================================
// STATE
// ============================================================

const STATES = {
  IDLE: "IDLE",
  LISTENING: "LISTENING",
  THINKING: "THINKING",
  SPEAKING: "SPEAKING",
  HAPPY: "HAPPY",
  CONFUSED: "CONFUSED",
  SLEEPING: "SLEEPING",
  ERROR: "ERROR"
};

let faceState = STATES.IDLE;

let chatBusy = false;
let voiceBusy = false;
let listenBusy = false;

const clients = new Set();

// ============================================================
// HELPERS
// ============================================================

function json(res, status, data) {
  const body = JSON.stringify(data);

  res.writeHead(status, {
    "Content-Type": "application/json; charset=utf-8",
    "Cache-Control": "no-cache",
    "Access-Control-Allow-Origin": "*"
  });

  res.end(body);
}

function text(res, status, body, contentType = "text/plain; charset=utf-8") {
  res.writeHead(status, {
    "Content-Type": contentType,
    "Cache-Control": "no-cache"
  });

  res.end(body);
}

function setFaceState(state) {
  if (!Object.values(STATES).includes(state)) {
    return false;
  }

  faceState = state;

  const payload = JSON.stringify({
    type: "face-state",
    state: faceState
  });

  for (const client of clients) {
    try {
      client.write(`data: ${payload}\n\n`);
    } catch {
      clients.delete(client);
    }
  }

  return true;
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    let body = "";

    req.on("data", chunk => {
      body += chunk;

      if (body.length > 1024 * 1024) {
        reject(new Error("Requisição muito grande."));
        req.destroy();
      }
    });

    req.on("end", () => {
      if (!body) {
        resolve({});
        return;
      }

      try {
        resolve(JSON.parse(body));
      } catch {
        reject(new Error("JSON inválido."));
      }
    });

    req.on("error", reject);
  });
}

// ============================================================
// LLAMA
// ============================================================

async function chatWithLlamaServer(message) {
  const memories = memory.context(8);
  const enrichedMessage = memories
    ? message + "\n\nContexto de memória autorizado do NOX:\n" + memories
    : message;

  const response = await fetch(
    `${LLAMA_BASE_URL}/v1/chat/completions`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        messages: [
          {
            role: "system",
            content: SYSTEM_PROMPT
          },
          {
            role: "user",
            content: enrichedMessage
          }
        ],
        temperature: TEMPERATURE,
        max_tokens: MAX_TOKENS,
        stream: false
      })
    }
  );

  if (!response.ok) {
    const errorText = await response.text();

    throw new Error(
      `llama-server HTTP ${response.status}: ${errorText.slice(0, 500)}`
    );
  }

  const data = await response.json();

  const answer =
    data?.choices?.[0]?.message?.content?.trim() || "";

  if (!answer) {
    throw new Error("O modelo não retornou uma resposta.");
  }

  return answer;
}

// ============================================================
// ELEVENLABS
// ============================================================

async function elevenLabsTTS(inputText) {
  if (!ELEVENLABS_API_KEY) {
    throw new Error("ELEVENLABS_API_KEY não configurada.");
  }

  if (!ELEVENLABS_VOICE_ID) {
    throw new Error("ELEVENLABS_VOICE_ID não configurado.");
  }

  const cleanText = String(inputText || "").trim();

  if (!cleanText) {
    throw new Error("Texto vazio para voz.");
  }

  const url =
    `https://api.elevenlabs.io/v1/text-to-speech/` +
    `${encodeURIComponent(ELEVENLABS_VOICE_ID)}`;

  const response = await fetch(url, {
    method: "POST",

    headers: {
      "Content-Type": "application/json",
      "Accept": "audio/mpeg",
      "xi-api-key": ELEVENLABS_API_KEY
    },

    body: JSON.stringify({
      text: cleanText,
      model_id: ELEVENLABS_MODEL_ID,

      voice_settings: {
        stability: 0.5,
        similarity_boost: 0.75,
        style: 0.2,
        use_speaker_boost: true
      }
    })
  });

  if (!response.ok) {
    const errorText = await response.text();

    throw new Error(
      `ElevenLabs HTTP ${response.status}: ${errorText.slice(0, 800)}`
    );
  }

  return Buffer.from(await response.arrayBuffer());
}

// ============================================================
// SPEECH TO TEXT
// ============================================================

function listenText() {
  if (listenBusy) {
    return Promise.reject(
      new Error("O NOX já está ouvindo.")
    );
  }

  listenBusy = true;

  setFaceState(STATES.LISTENING);

  return new Promise((resolve, reject) => {
    const child = spawn(LISTEN_COMMAND, [], {
      stdio: ["ignore", "pipe", "pipe"]
    });

    let stdout = "";
    let stderr = "";

    child.stdout.on("data", chunk => {
      stdout += chunk.toString();
    });

    child.stderr.on("data", chunk => {
      stderr += chunk.toString();
    });

    child.on("error", error => {
      listenBusy = false;
      setFaceState(STATES.ERROR);
      reject(error);
    });

    child.on("close", code => {
      listenBusy = false;

      const result = stdout.trim();

      if (code !== 0) {
        setFaceState(STATES.ERROR);

        reject(
          new Error(
            stderr.trim() ||
              `Reconhecimento de voz terminou com código ${code}.`
          )
        );

        return;
      }

      setFaceState(STATES.IDLE);

      resolve(result);
    });
  });
}

// ============================================================
// COMMANDS
// ============================================================

const {
  executeCommand,
  executeNaturalCommands
} = require("./commands");
const memory = require("./memory");
const { createToken } = require("./permissions");

let pendingDangerous = null;

function dangerousRequest(message) {
  const lower = String(message || "").toLowerCase();
  if (/(desligar|desligue|desliga).*(computador|pc|máquina|maquina)/.test(lower)) return "shutdown";
  if (/(reiniciar|reinicie|reinicia).*(computador|pc|máquina|maquina)/.test(lower)) return "restart";
  return null;
}

function confirmationRequest(message) {
  return /^(confirmo|confirmar|sim,? confirmo|pode executar|pode fazer)$/i.test(String(message || "").trim());
}

function runDangerous(action) {
  return new Promise((resolve, reject) => {
    if (process.platform !== "win32") {
      const file = action === "shutdown" ? "shutdown" : "reboot";
      const args = action === "shutdown" ? ["-h", "now"] : [];
      execFile(file, args, { timeout: 5000 }, error => error ? reject(error) : resolve());
      return;
    }
    const args = action === "shutdown" ? ["/s", "/t", "5"] : ["/r", "/t", "5"];
    execFile("shutdown.exe", args, { timeout: 5000, windowsHide: true }, error => error ? reject(error) : resolve());
  });
}

// ============================================================
// HTTP SERVER
// ============================================================

const server = http.createServer(async (req, res) => {
  try {
    const url = new URL(
      req.url,
      `http://${req.headers.host || `${HOST}:${PORT}`}`
    );

    // --------------------------------------------------------
    // CORS
    // --------------------------------------------------------

    if (req.method === "OPTIONS") {
      res.writeHead(204, {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Methods":
          "GET,POST,OPTIONS",
        "Access-Control-Allow-Headers":
          "Content-Type"
      });

      res.end();
      return;
    }

    // --------------------------------------------------------
    // UI
    // --------------------------------------------------------

    if (
      req.method === "GET" &&
      (url.pathname === "/" ||
        url.pathname === "/index.html")
    ) {
      const html = fs.readFileSync(FACE_PATH);

      res.writeHead(200, {
        "Content-Type": "text/html; charset=utf-8",
        "Cache-Control": "no-cache"
      });

      res.end(html);
      return;
    }

    // --------------------------------------------------------
    // HEALTH
    // --------------------------------------------------------

    if (
      req.method === "GET" &&
      url.pathname === "/api/health"
    ) {
      json(res, 200, {
        ok: true,
        service: "NOX Core",
        version: "0.7.0",
        faceState,
        time: new Date().toISOString()
      });

      return;
    }

    // --------------------------------------------------------
    // STATUS
    // --------------------------------------------------------

    if (
      req.method === "GET" &&
      url.pathname === "/api/status"
    ) {
      json(res, 200, {
        ok: true,
        service: "NOX Core",
        version: "0.6.0",
        faceState,
        chatBusy,
        voiceBusy,
        listenBusy,
        elevenlabs: {
          configured:
            Boolean(ELEVENLABS_API_KEY) &&
            Boolean(ELEVENLABS_VOICE_ID),
          voiceId: ELEVENLABS_VOICE_ID
            ? "configured"
            : null,
          model: ELEVENLABS_MODEL_ID
        },
        llama: {
          baseUrl: LLAMA_BASE_URL
        }
      });

      return;
    }

    // --------------------------------------------------------
    // SSE
    // --------------------------------------------------------

    if (
      req.method === "GET" &&
      url.pathname === "/api/events"
    ) {
      res.writeHead(200, {
        "Content-Type": "text/event-stream; charset=utf-8",
        "Cache-Control": "no-cache",
        Connection: "keep-alive",
        "Access-Control-Allow-Origin": "*"
      });

      res.write(
        `data: ${JSON.stringify({
          type: "face-state",
          state: faceState
        })}\n\n`
      );

      clients.add(res);

      req.on("close", () => {
        clients.delete(res);
      });

      return;
    }

    // --------------------------------------------------------
    // FACE STATE GET
    // --------------------------------------------------------

    if (
      req.method === "GET" &&
      url.pathname === "/api/face-state"
    ) {
      json(res, 200, {
        ok: true,
        state: faceState
      });

      return;
    }

    // --------------------------------------------------------
    // FACE STATE POST
    // --------------------------------------------------------

    if (
      req.method === "POST" &&
      url.pathname === "/api/face-state"
    ) {
      const body = await readBody(req);

      if (!setFaceState(body.state)) {
        json(res, 400, {
          ok: false,
          error: "Estado facial inválido."
        });

        return;
      }

      json(res, 200, {
        ok: true,
        state: faceState
      });

      return;
    }

    // --------------------------------------------------------
    // CHAT
    // --------------------------------------------------------

    if (
      req.method === "POST" &&
      url.pathname === "/api/chat"
    ) {
      if (chatBusy) {
        json(res, 429, {
          ok: false,
          error: "O NOX já está processando uma mensagem."
        });

        return;
      }

      const body = await readBody(req);
      const message = String(body.message || "").trim();

      if (!message) {
        json(res, 400, {
          ok: false,
          error: "Mensagem vazia."
        });

        return;
      }

      chatBusy = true;
      setFaceState(STATES.THINKING);

      try {
        if (confirmationRequest(message) && pendingDangerous && pendingDangerous.expiresAt > Date.now()) {
          const action = pendingDangerous.action;
          pendingDangerous = null;
          await runDangerous(action);
          setFaceState(STATES.HAPPY);
          json(res, 200, {
            ok: true,
            text: action === "shutdown"
              ? "Confirmado. O computador será desligado em alguns segundos."
              : "Confirmado. O computador será reiniciado em alguns segundos.",
            source: "dangerous-action",
            action
          });
          return;
        }

        const danger = dangerousRequest(message);
        if (danger) {
          pendingDangerous = {
            action: danger,
            token: createToken(danger),
            expiresAt: Date.now() + 30000
          };
          json(res, 200, {
            ok: true,
            text: danger === "shutdown"
              ? "Posso desligar o computador. Essa ação exige confirmação. Diga 'confirmo' para continuar."
              : "Posso reiniciar o computador. Essa ação exige confirmação. Diga 'confirmo' para continuar.",
            source: "confirmation-required",
            action: danger,
            expiresInMs: 30000
          });
          return;
        }

        const commandResult = await executeNaturalCommands(
          message,
          { root: ROOT }
        );

        if (commandResult.matched) {
          setFaceState(STATES.HAPPY);

          json(res, 200, {
            ok: true,
            text: commandResult.text,
            source: "command",
            commands: commandResult.commands
          });

          setFaceState(STATES.IDLE);
          return;
        }

        const answer = await chatWithLlamaServer(message);

        setFaceState(STATES.IDLE);

        json(res, 200, {
          ok: true,
          text: answer,
          source: "llama"
        });
      } catch (error) {
        setFaceState(STATES.ERROR);

        json(res, 500, {
          ok: false,
          error: error.message
        });
      } finally {
        chatBusy = false;
      }

      return;
    }

    // --------------------------------------------------------
    // ELEVENLABS VOICE
    // --------------------------------------------------------

    if (
      req.method === "POST" &&
      url.pathname === "/api/voice"
    ) {
      if (voiceBusy) {
        json(res, 429, {
          ok: false,
          error: "O NOX já está gerando uma voz."
        });

        return;
      }

      const body = await readBody(req);
      const voiceText = String(body.text || "").trim();

      if (!voiceText) {
        json(res, 400, {
          ok: false,
          error: "Texto vazio."
        });

        return;
      }

      voiceBusy = true;
      setFaceState(STATES.SPEAKING);

      try {
        const audio = await elevenLabsTTS(voiceText);

        res.writeHead(200, {
          "Content-Type": "audio/mpeg",
          "Content-Length": audio.length,
          "Cache-Control": "no-cache",
          "Access-Control-Allow-Origin": "*"
        });

        res.end(audio);
      } catch (error) {
        setFaceState(STATES.ERROR);

        json(res, 500, {
          ok: false,
          error: error.message
        });
      } finally {
        voiceBusy = false;

        if (faceState === STATES.SPEAKING) {
          setFaceState(STATES.IDLE);
        }
      }

      return;
    }

    // --------------------------------------------------------
    // LISTEN
    // --------------------------------------------------------

    if (
      req.method === "POST" &&
      url.pathname === "/api/listen"
    ) {
      try {
        const result = await listenText();

        json(res, 200, {
          ok: true,
          text: result
        });
      } catch (error) {
        json(res, 500, {
          ok: false,
          error: error.message
        });
      }

      return;
    }

    // --------------------------------------------------------
    // COMMANDS
    // --------------------------------------------------------

    if (
      req.method === "POST" &&
      url.pathname === "/api/command"
    ) {
      const body = await readBody(req);
      const command = String(body.command || "").trim();

      if (!command) {
        json(res, 400, { ok: false, error: "Comando vazio." });
        return;
      }

      try {
        const result = await executeCommand(command, { root: ROOT });
        json(res, 200, { ok: true, ...result });
      } catch (error) {
        json(res, 403, { ok: false, error: error.message });
      }
      return;
    }

    // --------------------------------------------------------
    // 404
    // --------------------------------------------------------

    json(res, 404, {
      ok: false,
      error: "Rota não encontrada."
    });
  } catch (error) {
    console.error("[NOX]", error);

    try {
      json(res, 500, {
        ok: false,
        error: error.message || "Erro interno."
      });
    } catch {}
  }
});

// ============================================================
// START
// ============================================================

server.listen(PORT, HOST, () => {
  console.log("");
  console.log("======================================");
  console.log("        NOX CORE 0.6 (Termux)");
  console.log("======================================");
  console.log(`UI:      http://${HOST}:${PORT}/`);
  console.log(`API:     http://${HOST}:${PORT}/api/health`);
  console.log(`Chat:    POST http://${HOST}:${PORT}/api/chat`);
  console.log(`Voice:   POST http://${HOST}:${PORT}/api/voice`);
  console.log(`Listen:  POST http://${HOST}:${PORT}/api/listen`);
  console.log("");

  console.log(
    `ElevenLabs: ${
      ELEVENLABS_API_KEY && ELEVENLABS_VOICE_ID
        ? "CONFIGURADO"
        : "NÃO CONFIGURADO"
    }`
  );
  console.log(
    `Voice ID: ${
      ELEVENLABS_VOICE_ID ? "CONFIGURADO" : "NÃO CONFIGURADO"
    }`
  );
  console.log(`Model: ${ELEVENLABS_MODEL_ID}`);
  console.log("======================================");
  console.log("");
});

process.on("SIGINT", () => {
  console.log("\nEncerrando NOX Core...");

  for (const client of clients) {
    try {
      client.end();
    } catch {}
  }

  server.close(() => {
    process.exit(0);
  });
});

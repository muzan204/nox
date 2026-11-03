#!/usr/bin/env node
"use strict";

// NOX 3.0 LAN gateway: exposes only the mobile API, protected by a pairing token.
// The Core and llama.cpp stay bound to loopback; this gateway is the Wi-Fi boundary.

const http = require("node:http");
const os = require("node:os");
const fs = require("node:fs");
const path = require("node:path");
const crypto = require("node:crypto");

const ROOT = path.resolve(__dirname, "..", "..");
const TOKEN_FILE = path.join(ROOT, "config", "device.token");
const CORE_HOST = process.env.NOX_CORE_HOST || "127.0.0.1";
const CORE_PORT = Number(process.env.NOX_CORE_PORT || 8765);
const HOST = process.env.NOX_LAN_HOST || "0.0.0.0";
const PORT = Number(process.env.NOX_LAN_PORT || 8766);
const CLIENTS = new Set();

function loadToken() {
  fs.mkdirSync(path.dirname(TOKEN_FILE), { recursive: true });
  if (!fs.existsSync(TOKEN_FILE)) {
    fs.writeFileSync(TOKEN_FILE, crypto.randomBytes(24).toString("base64url") + "\n", { mode: 0o600, flag: "wx" });
  }
  try { fs.chmodSync(TOKEN_FILE, 0o600); } catch {}
  const token = fs.readFileSync(TOKEN_FILE, "utf8").trim();
  if (token.length < 32) throw new Error("Token de pareamento inválido; remova config/device.token para gerar outro.");
  return token;
}

const TOKEN = loadToken();

function authorized(req) {
  const value = String(req.headers["x-nox-token"] || req.headers.authorization || "");
  return value === TOKEN || value === "Bearer " + TOKEN;
}

function json(res, code, data) {
  const body = JSON.stringify(data);
  res.writeHead(code, { "Content-Type": "application/json; charset=utf-8", "Cache-Control": "no-store" });
  res.end(body);
}

function localStatus() {
  const cpus = os.cpus() || [];
  const total = os.totalmem();
  const free = os.freemem();
  const nets = Object.entries(os.networkInterfaces()).flatMap(([name, entries]) =>
    (entries || []).filter(x => !x.internal && x.family === "IPv4").map(x => ({ interface: name, address: x.address }))
  );
  return {
    device: os.hostname(),
    platform: os.platform(),
    arch: os.arch(),
    cpuCores: cpus.length,
    loadAverage: os.loadavg(),
    memory: { totalBytes: total, freeBytes: free, usedBytes: total - free, usedPercent: total ? Math.round((total - free) / total * 100) : 0 },
    uptimeSeconds: Math.floor(os.uptime()),
    network: nets,
    connectedInterfaces: CLIENTS.size,
    timestamp: new Date().toISOString()
  };
}

async function coreSnapshot() {
  let faceState = "UNKNOWN";
  let core = null;
  try {
    const [faceResponse, statusResponse] = await Promise.all([
      fetch(`http://${CORE_HOST}:${CORE_PORT}/api/face-state`, { signal: AbortSignal.timeout(1500) }),
      fetch(`http://${CORE_HOST}:${CORE_PORT}/api/status`, { signal: AbortSignal.timeout(1500) })
    ]);
    if (faceResponse.ok) faceState = (await faceResponse.json()).state || faceState;
    if (statusResponse.ok) core = await statusResponse.json();
  } catch {}
  return { type: "snapshot", faceState, core, device: localStatus() };
}

function wsFrame(value) {
  const payload = Buffer.from(JSON.stringify(value));
  let header;
  if (payload.length < 126) {
    header = Buffer.from([0x81, payload.length]);
  } else if (payload.length < 65536) {
    header = Buffer.alloc(4);
    header[0] = 0x81; header[1] = 126; header.writeUInt16BE(payload.length, 2);
  } else {
    header = Buffer.alloc(10);
    header[0] = 0x81; header[1] = 127; header.writeBigUInt64BE(BigInt(payload.length), 2);
  }
  return Buffer.concat([header, payload]);
}

function controlFrame(opcode, payload = Buffer.alloc(0)) {
  return Buffer.concat([Buffer.from([0x80 | opcode, payload.length]), payload]);
}

function acceptWebSocket(req, socket, head) {
  if (!authorized(req)) {
    socket.write("HTTP/1.1 401 Unauthorized\r\nConnection: close\r\n\r\n");
    socket.destroy();
    return;
  }
  const key = req.headers["sec-websocket-key"];
  if (!key || req.headers["sec-websocket-version"] !== "13") {
    socket.write("HTTP/1.1 400 Bad Request\r\nConnection: close\r\n\r\n");
    socket.destroy();
    return;
  }
  const accept = crypto.createHash("sha1")
    .update(key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
    .digest("base64");
  socket.write(
    "HTTP/1.1 101 Switching Protocols\r\n" +
    "Upgrade: websocket\r\nConnection: Upgrade\r\n" +
    "Sec-WebSocket-Accept: " + accept + "\r\n\r\n"
  );
  const client = { socket, buffer: Buffer.alloc(0) };
  CLIENTS.add(client);
  socket.on("data", chunk => {
    client.buffer = Buffer.concat([client.buffer, chunk]);
    while (client.buffer.length >= 2) {
      const first = client.buffer[0];
      const second = client.buffer[1];
      let length = second & 0x7f;
      let offset = 2;
      if (length === 126) {
        if (client.buffer.length < 4) break;
        length = client.buffer.readUInt16BE(2); offset = 4;
      } else if (length === 127) {
        if (client.buffer.length < 10) break;
        const big = client.buffer.readBigUInt64BE(2);
        if (big > 65536n) { socket.destroy(); return; }
        length = Number(big); offset = 10;
      }
      const masked = Boolean(second & 0x80);
      if (masked) offset += 4;
      if (client.buffer.length < offset + length) break;
      let payload = client.buffer.subarray(offset, offset + length);
      if (masked) {
        const mask = client.buffer.subarray(offset - 4, offset);
        payload = Buffer.from(payload);
        for (let i = 0; i < payload.length; i++) payload[i] ^= mask[i % 4];
      }
      client.buffer = client.buffer.subarray(offset + length);
      const opcode = first & 0x0f;
      if (opcode === 0x8) { socket.end(controlFrame(0x8)); return; }
      if (opcode === 0x9) socket.write(controlFrame(0xA, payload));
    }
  });
  socket.on("close", () => CLIENTS.delete(client));
  socket.on("error", () => CLIENTS.delete(client));
  socket.write(wsFrame({ type: "connected", service: "NOX 3.0 LAN", timestamp: new Date().toISOString() }));
}

const ALLOWED = new Set([
  "GET /api/health", "GET /api/status", "GET /api/capabilities",
  "GET /api/memory", "GET /api/face-state", "GET /api/events",
  "POST /api/chat", "POST /api/face-state", "POST /api/memory"
]);

const server = http.createServer((req, res) => {
  if (!authorized(req)) return json(res, 401, { ok: false, error: "Dispositivo não pareado." });
  const url = new URL(req.url, "http://" + (req.headers.host || "localhost"));
  const route = req.method + " " + url.pathname;
  if (route === "GET /api/device-status") return json(res, 200, { ok: true, device: localStatus() });
  if (!ALLOWED.has(route)) return json(res, 404, { ok: false, error: "Rota não disponível no gateway móvel." });

  const headers = { ...req.headers, host: CORE_HOST + ":" + CORE_PORT };
  delete headers["x-nox-token"];
  delete headers.authorization;
  delete headers.connection;
  delete headers["content-length"];
  const proxy = http.request({
    hostname: CORE_HOST, port: CORE_PORT, path: req.url, method: req.method, headers
  }, upstream => {
    res.writeHead(upstream.statusCode || 502, upstream.headers);
    upstream.pipe(res);
  });
  proxy.on("error", error => {
    if (!res.headersSent) json(res, 502, { ok: false, error: "Core local indisponível: " + error.message });
    else res.destroy(error);
  });
  req.pipe(proxy);
});

server.on("upgrade", (req, socket, head) => {
  const url = new URL(req.url, "http://" + (req.headers.host || "localhost"));
  if (url.pathname !== "/ws") {
    socket.write("HTTP/1.1 404 Not Found\r\nConnection: close\r\n\r\n");
    socket.destroy();
    return;
  }
  acceptWebSocket(req, socket, head);
});

const broadcast = async () => {
  if (!CLIENTS.size) return;
  const snapshot = await coreSnapshot();
  const frame = wsFrame(snapshot);
  for (const client of CLIENTS) {
    if (client.socket.destroyed) { CLIENTS.delete(client); continue; }
    try { client.socket.write(frame); } catch { CLIENTS.delete(client); }
  }
};

const timer = setInterval(broadcast, 2500);
timer.unref();

server.listen(PORT, HOST, () => {
  const addresses = Object.entries(os.networkInterfaces()).flatMap(([name, entries]) =>
    (entries || []).filter(x => !x.internal && x.family === "IPv4").map(x => "http://" + x.address + ":" + PORT + " (" + name + ")")
  );
  console.log("[NOX 3.0] Gateway protegido ativo em " + HOST + ":" + PORT);
  console.log("[NOX 3.0] Endereços da rede: " + (addresses.join(" | ") || "não detectados"));
  console.log("[NOX 3.0] Token de pareamento (insira apenas no app NOX): " + TOKEN);
  console.log("[NOX 3.0] WebSocket: ws://IP_DO_REDMi: " + PORT + "/ws");
});

function shutdown() {
  clearInterval(timer);
  for (const client of CLIENTS) {
    try { client.socket.end(controlFrame(0x8)); } catch {}
  }
  server.close(() => process.exit(0));
}
process.on("SIGINT", shutdown);
process.on("SIGTERM", shutdown);

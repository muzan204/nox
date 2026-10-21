import http from "node:http";

const HOST = process.env.NOX_HOST || "127.0.0.1";
const PORT = Number(process.env.NOX_PORT || 8765);
const TOKEN = process.env.NOX_LOCAL_TOKEN || "";
const MODEL_URL = (process.env.NOX_LOCAL_MODEL_URL || "http://127.0.0.1:8080/v1/chat/completions").replace(/\/$/, "");
const MODEL = process.env.NOX_LOCAL_MODEL || "local-model";
const MAX_BODY = 24 * 1024;

function json(res, status, body) {
  res.writeHead(status, {
    "content-type": "application/json; charset=utf-8",
    "cache-control": "no-store",
    "x-content-type-options": "nosniff",
    "access-control-allow-origin": "null",
    "access-control-allow-methods": "POST, OPTIONS",
    "access-control-allow-headers": "content-type, authorization"
  });
  res.end(JSON.stringify(body));
}
function readBody(req) {
  return new Promise((resolve, reject) => {
    let data = "";
    req.on("data", chunk => {
      data += chunk;
      if (Buffer.byteLength(data) > MAX_BODY) {
        reject(new Error("BODY_TOO_LARGE"));
        req.destroy();
      }
    });
    req.on("end", () => {
      try { resolve(JSON.parse(data || "{}")); }
      catch { reject(new Error("INVALID_JSON")); }
    });
    req.on("error", reject);
  });
}
const server = http.createServer(async (req, res) => {
  if (req.method === "OPTIONS") return json(res, 204, {});
  if (req.url !== "/health" && req.url !== "/chat") return json(res, 404, { error: "Not found" });
  if (req.url === "/health" && req.method === "GET") return json(res, 200, { ok: true, model: MODEL });
  if (req.method !== "POST" || req.url !== "/chat") return json(res, 405, { error: "Use POST /chat" });
  if (!TOKEN || req.headers.authorization !== `Bearer ${TOKEN}`) return json(res, 401, { error: "Unauthorized" });

  let input;
  try { input = await readBody(req); }
  catch (err) { return json(res, err.message === "BODY_TOO_LARGE" ? 413 : 400, { error: "Invalid request body" }); }
  if (!Array.isArray(input.messages) || input.messages.length < 1 || input.messages.length > 12) {
    return json(res, 400, { error: "Send 1 to 12 recent messages" });
  }
  const messages = [{ role: "system", content: "Você é NOX, assistente pessoal. Responda em português brasileiro, seja útil e honesto. Não invente ações nem informações." }];
  for (const m of input.messages) {
    if (!m || !["user", "assistant"].includes(m.role) || typeof m.content !== "string" || !m.content.trim() || m.content.length > 4000) {
      return json(res, 400, { error: "Invalid message" });
    }
    messages.push({ role: m.role, content: m.content.trim() });
  }
  if (messages[messages.length - 1].role !== "user") return json(res, 400, { error: "Last message must be user" });
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 45000);
  try {
    const upstream = await fetch(MODEL_URL, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ model: MODEL, messages, temperature: 0.7, max_tokens: 800, stream: false }),
      signal: controller.signal
    });
    if (!upstream.ok) return json(res, 502, { error: "Local model unavailable" });
    const result = await upstream.json();
    const reply = result.choices?.[0]?.message?.content;
    if (typeof reply !== "string" || !reply.trim()) return json(res, 502, { error: "Empty local model response" });
    return json(res, 200, { reply: reply.trim(), model: MODEL, source: "local" });
  } catch {
    return json(res, 502, { error: "Could not reach local model server" });
  } finally { clearTimeout(timeout); }
});
server.listen(PORT, HOST, () => {
  console.log(`NOX local core listening on http://${HOST}:${PORT}`);
  console.log("Keep this service bound to localhost; do not expose it directly to the internet.");
});

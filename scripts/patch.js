const fs=require('fs'),os=require('os');
const p=os.homedir()+'/nox/core/server/server.js';
let s=fs.readFileSync(p,'utf8');
function rep(a,b){if(!s.includes(a)){console.error('Âncora não achada: '+a.slice(0,50));process.exit(1)}s=s.replace(a,()=>b)}
rep('const config = loadConfig();',String.raw`const config = loadConfig();
const { execFile } = require("node:child_process");
const MEM_DIR = path.join(ROOT, "memory");
const HIST = path.join(MEM_DIR, "history.json");
const FACTS = path.join(MEM_DIR, "facts.json");
function readJson(f, d) { try { return JSON.parse(fs.readFileSync(f, "utf8")); } catch { return d; } }
function writeJson(f, v) { fs.mkdirSync(MEM_DIR, { recursive: true }); fs.writeFileSync(f, JSON.stringify(v, null, 2)); }
let history = readJson(HIST, []);
let facts = readJson(FACTS, []);
function speak(t) { execFile("termux-tts-speak", ["-l", "pt-BR", t], () => {}); }`);
rep('{ role: "system", content: config.assistant.systemPrompt },',String.raw`{ role: "system", content: config.assistant.systemPrompt + (facts.length ? " Fatos que você sabe sobre o usuário: " + facts.join("; ") : "") },
            ...history.slice(-8),`);
rep('setFaceState("SPEAKING");',String.raw`const reply = data?.choices?.[0]?.message?.content || "";
    history.push({ role: "user", content: message }, { role: "assistant", content: reply });
    history = history.slice(-20);
    writeJson(HIST, history);
    if (reply) speak(reply);
    setFaceState("SPEAKING");`);
rep('const answer = await chatWithLlamaServer(message);',String.raw`const m = message.match(/^(?:nox,?\s*)?(?:lembre(?:-se)?|anote)\s+(?:que\s+)?(.+)/i);
      if (m) { facts.push(m[1].trim()); writeJson(FACTS, facts); const t = "Anotado! Vou lembrar disso."; speak(t); sendJson(res, 200, { ok: true, text: t }); return; }
      if (/^(?:nox,?\s*)?esque[cç]a tudo/i.test(message)) { facts = []; history = []; writeJson(FACTS, facts); writeJson(HIST, history); sendJson(res, 200, { ok: true, text: "Memória apagada." }); return; }
      const answer = await chatWithLlamaServer(message);`);
rep('if (req.method === "GET" && url.pathname === "/api/health") {',String.raw`if (req.method === "GET" && (url.pathname === "/" || url.pathname === "/face")) {
    res.writeHead(200, { "Content-Type": "text/html; charset=utf-8" });
    res.end(fs.readFileSync(path.join(__dirname, "face.html")));
    return;
  }

  if (req.method === "GET" && url.pathname === "/api/health") {`);
fs.writeFileSync(p+'.bak',fs.readFileSync(p));
fs.writeFileSync(p,s);
console.log('Patch aplicado.');

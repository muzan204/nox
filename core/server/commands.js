const os = require("node:os");
const fs = require("node:fs");
const path = require("node:path");
const { execFile, spawn } = require("node:child_process");
const memory = require("./memory");

const SAFE = {
  status: async () => ({
    text: "Sistema: " + os.platform() + " " + os.arch() +
      " | CPU: " + os.cpus().length +
      " | RAM: " + Math.round(os.freemem() / 1024 / 1024) + " MB livre"
  }),
  pwd: async ({ root }) => ({ text: root }),
  hostname: async () => ({ text: os.hostname() }),
  uptime: async () => ({ text: formatUptime(os.uptime()) }),
  memory: async () => ({
    text: "RAM: " + Math.round(os.totalmem() / 1024 / 1024) +
      " MB total | " + Math.round(os.freemem() / 1024 / 1024) + " MB livre"
  }),
  cpu: async () => ({
    text: "CPU: " + os.cpus().length + " núcleos | carga: " +
      os.loadavg().map(v => v.toFixed(2)).join(" / ")
  }),
  disk: async ({ root }) => diskInfo(root),
  network: async () => ({
    text: Object.entries(os.networkInterfaces())
      .flatMap(([name, items]) => (items || []).filter(x => !x.internal && x.family === "IPv4").map(x => name + ": " + x.address))
      .join("\n") || "Nenhuma interface IPv4 externa encontrada."
  }),
  gitStatus: async ({ root }) => run("git", ["-C", root, "status", "--short"]),
  gitBranch: async ({ root }) => run("git", ["-C", root, "branch", "--show-current"]),
  gitLog: async ({ root }) => run("git", ["-C", root, "log", "-5", "--oneline", "--decorate"]),
  listRoot: async ({ root }) => ({ text: listDirectory(root) }),
  projects: async ({ root }) => ({ text: listProjects(root) }),
  processes: async () => processList(),
  memoryList: async () => ({ text: formatMemories(memory.search("", 12)) }),
  memoryStats: async () => ({ text: "Memórias salvas: " + memory.stats() }),
  openVscode: async ({ root }) => openApp("code.cmd", [root], root, "VS Code"),
  openFolder: async ({ root }) => openApp(process.platform === "win32" ? "explorer.exe" : "xdg-open", [root], root, "pasta do NOX"),
  openBrowser: async () => openApp(process.platform === "win32" ? "cmd.exe" : "xdg-open", process.platform === "win32" ? ["/c", "start", "", "https://www.google.com"] : ["https://www.google.com"], process.cwd(), "navegador")
};

function formatUptime(seconds) {
  const total = Math.floor(Number(seconds) || 0);
  const days = Math.floor(total / 86400);
  const hours = Math.floor((total % 86400) / 3600);
  const minutes = Math.floor((total % 3600) / 60);
  return "Tempo ligado: " + days + "d " + hours + "h " + minutes + "min";
}

function listDirectory(directory) {
  const entries = fs.readdirSync(directory, { withFileTypes: true })
    .sort((a, b) => a.name.localeCompare(b.name));
  if (!entries.length) return "Pasta vazia.";
  return entries.slice(0, 80)
    .map(entry => (entry.isDirectory() ? "[DIR] " : "[FILE] ") + entry.name)
    .join("\n");
}

function listProjects(root) {
  const candidates = [
    root,
    path.join(path.dirname(root), "Meus-projetos"),
    path.join(os.homedir(), "Documents", "Meus-projetos"),
    path.join(os.homedir(), "projetos")
  ];
  const seen = new Set();
  const out = [];
  for (const base of candidates) {
    if (!fs.existsSync(base) || !fs.statSync(base).isDirectory()) continue;
    for (const entry of fs.readdirSync(base, { withFileTypes: true })) {
      if (!entry.isDirectory()) continue;
      const full = path.resolve(base, entry.name);
      if (!seen.has(full)) { seen.add(full); out.push(entry.name + " — " + full); }
    }
  }
  return out.slice(0, 60).join("\n") || "Nenhum projeto encontrado.";
}

function diskInfo(root) {
  if (process.platform === "win32") {
    return run("powershell.exe", ["-NoProfile", "-Command", "$d=Get-PSDrive -Name " + root[0] + "; 'Disco ' + $d.Name + ': ' + [math]::Round(($d.Used/1GB),2) + ' GB usado | ' + [math]::Round(($d.Free/1GB),2) + ' GB livre'"]);
  }
  return run("df", ["-h", root]);
}

function processList() {
  if (process.platform === "win32") {
    return run("tasklist", ["/fo", "csv", "/nh"]);
  }
  return run("ps", ["-eo", "pid,comm,%cpu,%mem", "--sort=-%cpu"]);
}

function formatMemories(rows) {
  if (!rows.length) return "Nenhuma memória salva.";
  return rows.map(row => "#" + row.id + " " + (row.key ? "[" + row.key + "] " : "") + row.value).join("\n");
}

function run(file, args) {
  return new Promise((resolve, reject) => {
    execFile(file, args, {
      timeout: 15000,
      maxBuffer: 256 * 1024,
      windowsHide: true
    }, (error, stdout, stderr) => {
      if (error) { reject(new Error((stderr || error.message).trim())); return; }
      resolve({ text: (stdout || "OK").trim() });
    });
  });
}

function openApp(file, args, cwd, label) {
  try {
    const child = spawn(file, args, { cwd, detached: true, stdio: "ignore", windowsHide: true });
    child.unref();
    return Promise.resolve({ text: label + " aberto." });
  } catch (error) {
    return Promise.reject(error);
  }
}

function normalize(input) {
  return String(input || "").trim().toLowerCase().normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "").replace(/[?!.]+$/g, "").replace(/\s+/g, " ");
}

function detectCommandKeys(input) {
  const lower = normalize(input);
  const keys = [];
  const add = key => { if (!keys.includes(key)) keys.push(key); };

  if (lower === "status" || lower.includes("status do sistema") || lower.includes("status atual do sistema") || lower.includes("como esta o sistema")) add("status");
  if (lower === "pwd" || lower.includes("qual a pasta atual") || lower.includes("qual e a pasta atual") || lower.includes("pasta atual")) add("pwd");
  if (lower === "git status" || lower.includes("status do git") || lower.includes("git esta limpo")) add("gitStatus");
  if (lower === "git branch" || lower.includes("qual a branch") || lower.includes("branch atual") || lower.includes("branch do git")) add("gitBranch");
  if (lower === "git log" || lower.includes("ultimos commits") || lower.includes("historico do git")) add("gitLog");
  if (lower === "hostname" || lower.includes("nome do computador") || lower.includes("nome desta maquina") || lower.includes("nome da maquina")) add("hostname");
  if (lower === "uptime" || lower.includes("tempo ligado") || lower.includes("ha quanto tempo o computador") || lower.includes("ha quanto tempo esta ligado")) add("uptime");
  if (lower === "memoria" || lower === "ram" || lower.includes("quanto de ram") || lower.includes("ram livre") || lower.includes("memoria livre") || lower.includes("memoria do computador") || lower.includes("quanto de memoria")) add("memory");
  if (lower === "cpu" || lower.includes("uso da cpu") || lower.includes("carga da cpu") || lower.includes("quantos nucleos")) add("cpu");
  if (lower === "disco" || lower.includes("espaco em disco") || lower.includes("quanto de armazenamento")) add("disk");
  if (lower === "rede" || lower.includes("ip do computador") || lower.includes("endereco ip")) add("network");
  if (lower === "ls" || lower === "dir" || lower.includes("listar arquivos") || lower.includes("listar a pasta") || lower.includes("o que tem nessa pasta")) add("listRoot");
  if (lower.includes("listar projetos") || lower.includes("meus projetos") || lower.includes("projetos do computador")) add("projects");
  if (lower.includes("processos") || lower.includes("programas abertos") || lower.includes("processos rodando")) add("processes");
  if (lower.includes("mostrar memorias") || lower.includes("minhas memorias") || lower.includes("o que voce lembra")) add("memoryList");
  if (lower.includes("quantas memorias") || lower.includes("quantas coisas voce lembra")) add("memoryStats");
  if (lower.includes("abrir vscode") || lower.includes("abrir vs code")) add("openVscode");
  if (lower.includes("abrir a pasta do nox") || lower.includes("abrir pasta do nox")) add("openFolder");
  if (lower.includes("abrir navegador")) add("openBrowser");

  return keys;
}

function parseMemoryCommand(input) {
  const raw = String(input || "").trim();
  const normalized = normalize(raw);
  const rememberMatch = normalized.match(/^(lembre que|lembre|guarde que|guarde)\s+(.+)$/);
  if (rememberMatch) return { type: "remember", value: raw.replace(/^(lembre que|lembre|guarde que|guarde)\s+/i, "") };
  const forgetMatch = normalized.match(/^(esqueca|esqueça|apague da memoria|apague da memória)\s+(.+)$/i);
  if (forgetMatch) return { type: "forget", value: raw.replace(/^(esqueca|esqueça|apague da memoria|apague da memória)\s+/i, "") };
  return null;
}

async function executeNaturalCommands(input, ctx) {
  const memCommand = parseMemoryCommand(input);
  if (memCommand) {
    if (memCommand.type === "remember") {
      memory.remember(memCommand.value);
      return { matched: true, text: "Entendido. Salvei isso na minha memória.", commands: ["remember"] };
    }
    const count = memory.forget(memCommand.value);
    return { matched: true, text: count ? "Apaguei " + count + " memória(s) relacionada(s)." : "Não encontrei uma memória com esse conteúdo.", commands: ["forget"] };
  }

  const keys = detectCommandKeys(input);
  if (!keys.length) return { matched: false };

  const labels = {
    status: "Status do sistema", hostname: "Nome do computador", memory: "Memória",
    cpu: "CPU", disk: "Disco", network: "Rede", gitBranch: "Branch do Git",
    gitStatus: "Status do Git", gitLog: "Últimos commits", uptime: "Tempo ligado",
    pwd: "Pasta atual", listRoot: "Conteúdo da pasta", projects: "Projetos",
    processes: "Processos", memoryList: "Memórias", memoryStats: "Memória do NOX",
    openVscode: "VS Code", openFolder: "Pasta", openBrowser: "Navegador"
  };

  const results = [];
  for (const key of keys) {
    try {
      const result = await SAFE[key](ctx);
      results.push(labels[key] + ": " + result.text);
    } catch (error) {
      results.push(labels[key] + ": erro — " + error.message);
    }
  }

  return { matched: true, text: results.join("\n"), commands: keys };
}

async function executeCommand(input, ctx) {
  const keys = detectCommandKeys(input);
  if (keys.length !== 1) {
    throw new Error(keys.length > 1 ? "Use uma frase com um único comando neste endpoint." : "Comando não permitido.");
  }
  return SAFE[keys[0]](ctx);
}

module.exports = { executeCommand, executeNaturalCommands, detectCommandKeys, normalize };
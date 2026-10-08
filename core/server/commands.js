const os = require("node:os");
const fs = require("node:fs");
const { execFile } = require("node:child_process");

const SAFE = {
  status: async () => ({
    text: `Sistema: ${os.platform()} ${os.arch()} | CPU: ${os.cpus().length} | RAM: ${Math.round(os.freemem() / 1024 / 1024)} MB livre`
  }),
  pwd: async ({ root }) => ({ text: root }),
  hostname: async () => ({ text: os.hostname() }),
  uptime: async () => ({ text: formatUptime(os.uptime()) }),
  memory: async () => ({
    text: `RAM: ${Math.round(os.totalmem() / 1024 / 1024)} MB total | ${Math.round(os.freemem() / 1024 / 1024)} MB livre`
  }),
  gitStatus: async ({ root }) => run("git", ["-C", root, "status", "--short"]),
  gitBranch: async ({ root }) => run("git", ["-C", root, "branch", "--show-current"]),
  gitLog: async ({ root }) => run("git", ["-C", root, "log", "-5", "--oneline", "--decorate"]),
  listRoot: async ({ root }) => ({ text: listDirectory(root) })
};

function formatUptime(seconds) {
  const total = Math.floor(Number(seconds) || 0);
  const days = Math.floor(total / 86400);
  const hours = Math.floor((total % 86400) / 3600);
  const minutes = Math.floor((total % 3600) / 60);
  return `Tempo ligado: ${days}d ${hours}h ${minutes}min`;
}

function listDirectory(directory) {
  const entries = fs.readdirSync(directory, { withFileTypes: true })
    .sort((a, b) => a.name.localeCompare(b.name));

  if (!entries.length) return "Pasta vazia.";

  return entries.slice(0, 80)
    .map(entry => `${entry.isDirectory() ? "[DIR]" : "[FILE]"} ${entry.name}`)
    .join("\n");
}

function run(file, args) {
  return new Promise((resolve, reject) => {
    execFile(file, args, {
      timeout: 15000,
      maxBuffer: 256 * 1024,
      windowsHide: true
    }, (error, stdout, stderr) => {
      if (error) {
        reject(new Error((stderr || error.message).trim()));
        return;
      }
      resolve({ text: (stdout || "OK").trim() });
    });
  });
}

function normalize(input) {
  return String(input || "")
    .trim()
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[?!.]+$/g, "")
    .replace(/\s+/g, " ");
}

function detectCommandKeys(input) {
  const lower = normalize(input);
  const keys = [];

  const add = key => {
    if (!keys.includes(key)) keys.push(key);
  };

  if (
    lower === "status" ||
    lower.includes("status do sistema") ||
    lower.includes("status atual do sistema") ||
    lower.includes("como esta o sistema")
  ) add("status");

  if (
    lower === "pwd" ||
    lower.includes("qual a pasta atual") ||
    lower.includes("qual e a pasta atual") ||
    lower.includes("pasta atual")
  ) add("pwd");

  if (
    lower === "git status" ||
    lower.includes("status do git") ||
    lower.includes("git esta limpo")
  ) add("gitStatus");

  if (
    lower === "git branch" ||
    lower.includes("qual a branch") ||
    lower.includes("branch atual") ||
    lower.includes("branch do git")
  ) add("gitBranch");

  if (
    lower === "git log" ||
    lower.includes("ultimos commits") ||
    lower.includes("historico do git")
  ) add("gitLog");

  if (
    lower === "hostname" ||
    lower.includes("nome do computador") ||
    lower.includes("nome desta maquina") ||
    lower.includes("nome da maquina")
  ) add("hostname");

  if (
    lower === "uptime" ||
    lower.includes("tempo ligado") ||
    lower.includes("ha quanto tempo o computador") ||
    lower.includes("ha quanto tempo esta ligado")
  ) add("uptime");

  if (
    lower === "memoria" ||
    lower === "ram" ||
    lower.includes("quanto de ram") ||
    lower.includes("ram livre") ||
    lower.includes("memoria livre") ||
    lower.includes("memoria do computador") ||
    lower.includes("quanto de memoria")
  ) add("memory");

  if (
    lower === "ls" ||
    lower === "dir" ||
    lower.includes("listar arquivos") ||
    lower.includes("listar a pasta") ||
    lower.includes("o que tem nessa pasta")
  ) add("listRoot");

  return keys;
}

async function executeNaturalCommands(input, ctx) {
  const keys = detectCommandKeys(input);

  if (!keys.length) {
    return { matched: false };
  }

  const labels = {
    status: "Status do sistema",
    hostname: "Nome do computador",
    memory: "Memória",
    gitBranch: "Branch do Git",
    gitStatus: "Status do Git",
    gitLog: "Últimos commits",
    uptime: "Tempo ligado",
    pwd: "Pasta atual",
    listRoot: "Conteúdo da pasta"
  };

  const results = [];

  for (const key of keys) {
    try {
      const result = await SAFE[key](ctx);
      results.push(`${labels[key]}: ${result.text}`);
    } catch (error) {
      results.push(`${labels[key]}: erro — ${error.message}`);
    }
  }

  return {
    matched: true,
    text: results.join("\n"),
    commands: keys
  };
}

async function executeCommand(input, ctx) {
  const keys = detectCommandKeys(input);

  if (keys.length !== 1) {
    throw new Error(
      keys.length > 1
        ? "Use uma frase com um único comando neste endpoint."
        : "Comando não permitido. O NOX só executa comandos explicitamente autorizados."
    );
  }

  return SAFE[keys[0]](ctx);
}

module.exports = {
  executeCommand,
  executeNaturalCommands
};

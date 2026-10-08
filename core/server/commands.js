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
    .replace(/[?!.]+$/g, "")
    .replace(/\s+/g, " ");
}

async function executeCommand(input, ctx) {
  const lower = normalize(input);

  if (lower === "status" || lower.includes("status do sistema") || lower.includes("como está o sistema"))
    return SAFE.status(ctx);

  if (lower === "pwd" || lower.includes("onde estou") || lower.includes("qual a pasta atual"))
    return SAFE.pwd(ctx);

  if (lower === "git status" || lower.includes("status do git") || lower.includes("git está limpo"))
    return SAFE.gitStatus(ctx);

  if (lower === "git branch" || lower.includes("qual a branch") || lower.includes("branch atual"))
    return SAFE.gitBranch(ctx);

  if (lower === "git log" || lower.includes("últimos commits") || lower.includes("historico do git") || lower.includes("histórico do git"))
    return SAFE.gitLog(ctx);

  if (lower === "hostname" || lower.includes("nome do computador") || lower.includes("nome da máquina") || lower.includes("nome da maquina"))
    return SAFE.hostname(ctx);

  if (lower === "uptime" || lower.includes("tempo ligado") || lower.includes("há quanto tempo") || lower.includes("ha quanto tempo"))
    return SAFE.uptime(ctx);

  if (lower === "memória" || lower === "memoria" || lower.includes("quanto de ram") || lower.includes("memória livre") || lower.includes("memoria livre"))
    return SAFE.memory(ctx);

  if (lower === "ls" || lower === "dir" || lower.includes("listar arquivos") || lower.includes("listar a pasta") || lower.includes("o que tem nessa pasta"))
    return SAFE.listRoot(ctx);

  throw new Error("Comando não permitido. O NOX só executa comandos explicitamente autorizados.");
}

module.exports = { executeCommand };

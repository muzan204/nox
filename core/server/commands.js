const os = require("node:os");
const { execFile } = require("node:child_process");

const SAFE = {
  status: async () => ({
    text: `Sistema: ${os.platform()} ${os.arch()} | CPU: ${os.cpus().length} | RAM: ${Math.round(os.freemem()/1024/1024)} MB livre`
  }),
  pwd: async ({ root }) => ({ text: root }),
  git: async ({ root }) => run("git", ["-C", root, "status", "--short"]),
};

function run(file, args) {
  return new Promise((resolve, reject) => {
    execFile(file, args, { timeout: 15000, maxBuffer: 256 * 1024 }, (error, stdout, stderr) => {
      if (error) return reject(new Error((stderr || error.message).trim()));
      resolve({ text: (stdout || "OK").trim() });
    });
  });
}

async function executeCommand(input, ctx) {
  const text = String(input).trim();
  const lower = text.toLowerCase();

  if (lower === "status" || lower.includes("status do sistema")) return SAFE.status(ctx);
  if (lower === "pwd" || lower.includes("onde estou")) return SAFE.pwd(ctx);
  if (lower === "git status" || lower.includes("status do git")) return SAFE.git(ctx);

  throw new Error("Comando não permitido. O NOX só executa comandos explicitamente autorizados.");
}

module.exports = { executeCommand };

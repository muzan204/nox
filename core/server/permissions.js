const LEVELS = Object.freeze({
  SAFE: 1,
  CHANGE: 2,
  DANGEROUS: 3
});

const ACTIONS = Object.freeze({
  READ: 1,
  OPEN_APP: 2,
  START_SERVER: 2,
  WRITE_FILE: 2,
  DELETE: 3,
  SHUTDOWN: 3,
  RESTART: 3
});

function needsConfirmation(level) {
  return Number(level) >= LEVELS.DANGEROUS;
}

function createToken(action) {
  return Buffer.from("nox:" + String(action || "").trim().toLowerCase() + ":" + Date.now()).toString("base64url");
}

module.exports = { LEVELS, ACTIONS, needsConfirmation, createToken };
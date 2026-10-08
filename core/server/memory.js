const fs = require("node:fs");
const path = require("node:path");
const DEFAULT_PATH = path.join(__dirname, "..", "..", "memory", "nox.db");

let db = null;

function getDb() {
  if (db) return db;
  fs.mkdirSync(path.dirname(DEFAULT_PATH), { recursive: true });

  try {
    const { DatabaseSync } = require("node:sqlite");
    db = new DatabaseSync(DEFAULT_PATH);
    db.exec("PRAGMA journal_mode = WAL; CREATE TABLE IF NOT EXISTS memories (id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT NOT NULL DEFAULT 'fact', key TEXT, value TEXT NOT NULL, created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP); CREATE INDEX IF NOT EXISTS idx_memories_key ON memories(key);");
    return db;
  } catch (error) {
    throw new Error("Memória SQLite indisponível neste Node.js: " + error.message);
  }
}

function remember(value, key = null, kind = "fact") {
  const text = String(value || "").trim();
  if (!text) throw new Error("Memória vazia.");
  const database = getDb();

  if (key) {
    const existing = database.prepare("SELECT id FROM memories WHERE key = ? ORDER BY id DESC LIMIT 1").get(String(key));
    if (existing) {
      database.prepare("UPDATE memories SET value = ?, kind = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?").run(text, kind, existing.id);
      return { id: existing.id, updated: true };
    }
  }

  const result = database.prepare("INSERT INTO memories(kind, key, value) VALUES (?, ?, ?)").run(kind, key ? String(key) : null, text);
  return { id: Number(result.lastInsertRowid), updated: false };
}

function search(query = "", limit = 12) {
  const database = getDb();
  const q = String(query || "").trim();
  return q
    ? database.prepare("SELECT id, kind, key, value, created_at, updated_at FROM memories WHERE value LIKE ? OR key LIKE ? ORDER BY updated_at DESC LIMIT ?").all("%" + q + "%", "%" + q + "%", Number(limit))
    : database.prepare("SELECT id, kind, key, value, created_at, updated_at FROM memories ORDER BY updated_at DESC LIMIT ?").all(Number(limit));
}

function forget(query) {
  const database = getDb();
  const q = String(query || "").trim();
  if (!q) throw new Error("Informe o que devo esquecer.");
  const result = database.prepare("DELETE FROM memories WHERE value LIKE ? OR key LIKE ?").run("%" + q + "%", "%" + q + "%");
  return Number(result.changes);
}

function context(limit = 8) {
  return search("", limit)
    .map(row => "- " + (row.key ? row.key + ": " : "") + row.value)
    .join("\n");
}

function stats() {
  return Number(getDb().prepare("SELECT COUNT(*) AS total FROM memories").get().total);
}

module.exports = { remember, search, forget, context, stats };
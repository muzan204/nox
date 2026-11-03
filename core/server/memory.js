"use strict";

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
    db.exec(`
      PRAGMA journal_mode = WAL;
      CREATE TABLE IF NOT EXISTS memories (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        kind TEXT NOT NULL DEFAULT 'fact',
        key TEXT,
        value TEXT NOT NULL,
        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
      );
      CREATE INDEX IF NOT EXISTS idx_memories_key ON memories(key);
      CREATE TABLE IF NOT EXISTS web_knowledge (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        title TEXT NOT NULL,
        url TEXT NOT NULL UNIQUE,
        topic TEXT NOT NULL DEFAULT '',
        content TEXT NOT NULL,
        source TEXT NOT NULL DEFAULT 'web',
        fetched_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
      );
      CREATE INDEX IF NOT EXISTS idx_web_knowledge_fetched ON web_knowledge(fetched_at);
    `);
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

function saveKnowledge(item, maxItems = 150) {
  const title = String(item?.title || "").trim().slice(0, 220);
  const url = String(item?.url || "").trim();
  const topic = String(item?.topic || "").trim().slice(0, 240);
  const content = String(item?.content || "").trim().slice(0, 6000);
  if (!title || !/^https:\/\//i.test(url) || content.length < 120) {
    throw new Error("Registro de conhecimento inválido.");
  }
  const database = getDb();
  const result = database.prepare(`
    INSERT INTO web_knowledge(title, url, topic, content, source)
    VALUES (?, ?, ?, ?, ?)
    ON CONFLICT(url) DO UPDATE SET
      title = excluded.title,
      topic = excluded.topic,
      content = excluded.content,
      source = excluded.source,
      fetched_at = CURRENT_TIMESTAMP
  `).run(title, url, topic, content, String(item?.source || "web"));
  database.prepare(`
    DELETE FROM web_knowledge
    WHERE id NOT IN (
      SELECT id FROM web_knowledge ORDER BY fetched_at DESC, id DESC LIMIT ?
    )
  `).run(Math.max(20, Math.min(500, Number(maxItems) || 150)));
  return { id: Number(result.lastInsertRowid || 0), saved: true };
}

function searchKnowledge(query = "", limit = 4) {
  const database = getDb();
  const rows = database.prepare(
    "SELECT id, title, url, topic, content, source, fetched_at FROM web_knowledge ORDER BY fetched_at DESC, id DESC LIMIT 200"
  ).all();
  const terms = String(query || "").toLowerCase()
    .normalize("NFD").replace(/[\u0300-\u036f]/g, "")
    .split(/[^a-z0-9]+/).filter(word => word.length >= 3).slice(0, 10);
  const ranked = rows.map(row => {
    const haystack = (row.title + " " + row.topic + " " + row.content)
      .toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "");
    const score = terms.reduce((sum, term) => sum + (haystack.includes(term) ? 1 : 0), 0);
    return { ...row, score };
  }).filter(row => !terms.length || row.score > 0)
    .sort((a, b) => b.score - a.score || String(b.fetched_at).localeCompare(String(a.fetched_at)));
  return ranked.slice(0, Math.max(1, Math.min(10, Number(limit) || 4)));
}

function knowledgeStats() {
  const row = getDb().prepare("SELECT COUNT(*) AS total, MAX(fetched_at) AS lastFetched FROM web_knowledge").get();
  return { count: Number(row.total || 0), lastFetched: row.lastFetched || null };
}

function stats() {
  return Number(getDb().prepare("SELECT COUNT(*) AS total FROM memories").get().total);
}

module.exports = {
  remember, search, forget, context, stats,
  saveKnowledge, searchKnowledge, knowledgeStats
};

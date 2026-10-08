package com.muzan204.nox

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class NoxMemory(context: Context) : SQLiteOpenHelper(context, "nox.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE memories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                kind TEXT NOT NULL,
                content TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun save(kind: String, content: String) {
        writableDatabase.insert("memories", null, ContentValues().apply {
            put("kind", kind)
            put("content", content)
            put("created_at", System.currentTimeMillis())
        })
    }

    fun recent(limit: Int = 20): List<Pair<String,String>> {
        val out = mutableListOf<Pair<String,String>>()
        readableDatabase.rawQuery(
            "SELECT kind, content FROM memories ORDER BY id DESC LIMIT ?",
            arrayOf(limit.toString())
        ).use { c ->
            while (c.moveToNext()) out += c.getString(0) to c.getString(1)
        }
        return out
    }
}

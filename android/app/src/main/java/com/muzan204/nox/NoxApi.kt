package com.muzan204.nox

import android.content.Context
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class NoxApi(private val context: Context) {
    private val prefs = context.getSharedPreferences("nox", Context.MODE_PRIVATE)

    private fun base(): String = prefs.getString("server_url", "http://127.0.0.1:8765")!!.trimEnd('/')

    fun chat(message: String): String? = request("/api/chat", JSONObject().put("message", message))

    fun command(command: String): String? = request("/api/command", JSONObject().put("command", command))

    private fun request(path: String, body: JSONObject): String? {
        return try {
            val c = URL(base() + path).openConnection() as HttpURLConnection
            c.requestMethod = "POST"
            c.connectTimeout = 5000
            c.readTimeout = 120000
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.outputStream.use { it.write(body.toString().toByteArray()) }
            val text = c.inputStream.bufferedReader().readText()
            JSONObject(text).optString("text", text)
        } catch (_: Exception) { null }
    }
}

package com.muzan204.nox

import android.content.Context
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

data class ChatResult(val text: String)

class NoxApi(private val context: Context) {
    private val prefs = context.getSharedPreferences("nox", Context.MODE_PRIVATE)

    fun baseUrl(): String = prefs.getString("server_url", "http://127.0.0.1:8765")!!.trimEnd('/')
    fun saveBaseUrl(url: String) { prefs.edit().putString("server_url", url.trim().trimEnd('/')).apply() }

    fun chat(message: String): ChatResult? {
        val body = request("/api/chat", JSONObject().put("message", message)) ?: return null
        return ChatResult(body.optString("text", ""))
    }

    fun command(command: String): String? =
        request("/api/command", JSONObject().put("command", command))?.optString("text")

    fun voice(text: String): ByteArray? = try {
        val c = URL(baseUrl() + "/api/voice").openConnection() as HttpURLConnection
        c.requestMethod = "POST"
        c.connectTimeout = 5000
        c.readTimeout = 120000
        c.doOutput = true
        c.setRequestProperty("Content-Type", "application/json")
        c.outputStream.use { it.write(JSONObject().put("text", text).toString().toByteArray()) }
        if (c.responseCode !in 200..299) null else c.inputStream.use { it.readBytes() }
    } catch (_: Exception) { null }

    private fun request(path: String, body: JSONObject): JSONObject? = try {
        val c = URL(baseUrl() + path).openConnection() as HttpURLConnection
        c.requestMethod = "POST"
        c.connectTimeout = 5000
        c.readTimeout = 120000
        c.doOutput = true
        c.setRequestProperty("Content-Type", "application/json")
        c.outputStream.use { it.write(body.toString().toByteArray()) }
        if (c.responseCode !in 200..299) null
        else JSONObject(c.inputStream.bufferedReader().readText())
    } catch (_: Exception) { null }
}

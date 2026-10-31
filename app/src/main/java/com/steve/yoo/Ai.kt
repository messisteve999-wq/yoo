package com.steve.yoo

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Ai {
    // Appeler uniquement depuis un thread en arrière-plan.
    fun reply(sender: String, message: String, mode: String): String {
        val conn = URL(Store.serverUrl).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.connectTimeout = 15000
        conn.readTimeout = 60000
        conn.doOutput = true
        val body = JSONObject()
            .put("contact", sender)
            .put("message", message)
            .put("mode", mode)
            .toString()
        conn.outputStream.use { it.write(body.toByteArray()) }
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        return JSONObject(text).optString("reply", "")
    }
}

package ir.panahannet.panah

import java.net.HttpURLConnection
import java.net.URL

object Fetcher {
    fun get(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 25000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Panah/1.0 (Android)")
            setRequestProperty("Accept", "*/*")
        }
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (code !in 200..299) throw RuntimeException("HTTP $code")
            if (body.isBlank()) throw RuntimeException("پاسخ خالی")
            return body
        } finally {
            conn.disconnect()
        }
    }
}

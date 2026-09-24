package ir.panahannet.panah

import android.net.Uri
import android.util.Base64
import org.json.JSONObject
import java.nio.charset.Charset

object SubParser {
    private val known = setOf(
        "vmess", "vless", "trojan", "ss", "ssr",
        "hysteria2", "hy2", "hysteria", "tuic",
        "wireguard", "wg", "socks", "socks5", "http", "https"
    )

    fun parse(body: String): List<Node> {
        val text = decodeMaybeBase64(body.trim())
        return text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { parseLine(it) }
            .toList()
    }

    private fun decodeMaybeBase64(s: String): String {
        if (s.contains("://")) return s
        val cleaned = s.replace("\\s".toRegex(), "")
        return try {
            val decoded = String(Base64.decode(cleaned, Base64.DEFAULT), Charset.forName("UTF-8"))
            if (decoded.contains("://") || decoded.contains('\n')) decoded else s
        } catch (_: Exception) {
            s
        }
    }

    private fun parseLine(line: String): Node? {
        val scheme = line.substringBefore("://", "").lowercase()
        if (scheme.isEmpty() || scheme !in known) return null
        return if (scheme) {
            "vmess" -> parseVmess(line)
            "ss" -> parseSs(line)
            else -> parseUri(line, scheme)
        }
    }

    private fun parseVmess(line: String): Node {
        return try {
            val b64 = line.removePrefix("vmess://")
            val json = String(Base64.decode(b64, Base64.DEFAULT), Charset.forName("UTF-8"))
            val o = JSONObject(json)
            val name = o.optString("ps").ifEmpty { o.optString("add") }.ifEmpty { "VMess" }
            Node(line, "VMess", name, o.optString("add"), o.optString("port"))
        } catch (_: Exception) {
            Node(line, "VMess", "VMess", "", "")
        }
    }

    private fun parseUri(line: String, scheme: String): Node {
        val uri = Uri.parse(line)
        val name = uri.fragment?.let { Uri.decode(it) }?.takeIf { it.isNotBlank() }
            ?: uri.host
            ?: scheme
        val host = uri.host.orEmpty()
        val port = if (uri.port != -1) uri.port.toString() else ""
        val proto = if (scheme) {
            "vless" -> "VLESS"
            "trojan" -> "Trojan"
            "hysteria2", "hy2" -> "HY2"
            "hysteria" -> "HY"
            "tuic" -> "TUIC"
            "wireguard", "wg" -> "WG"
            "socks", "socks5" -> "SOCKS"
            else -> scheme.uppercase()
        }
        return Node(line, proto, name, host, port)
    }

    private fun parseSs(line: String): Node {
        return try {
            val uri = Uri.parse(line)
            val name = uri.fragment?.let { Uri.decode(it) }?.takeIf { it.isNotBlank() } ?: "Shadowsocks"
            if (!uri.host.isNullOrEmpty() && !uri.userInfo.isNullOrEmpty()) {
                return Node(
                    line, "SS", name, uri.host.orEmpty(),
                    if (uri.port != -1) uri.port.toString() else ""
                )
            }
            val payload = line.removePrefix("ss://").substringBefore("#").substringBefore("?")
            val decoded = String(
                Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING),
                Charset.forName("UTF-8")
            )
            val hostPort = decoded.substringAfter("@", "")
            val host = hostPort.substringBefore(":")
            val port = hostPort.substringAfter(":", "")
            Node(line, "SS", name, host, port)
        } catch (_: Exception) {
            Node(line, "SS", "Shadowsocks", "", "")
        }
    }
}

package ir.panahannet.panah

data class Sub(
    val id: String,
    val name: String,
    val url: String,
    val cache: String = "",
    val updatedAt: Long = 0L,
    val lastError: String = "",
    val nodeCount: Int = 0
)

data class Node(
    val raw: String,
    val protocol: String,
    val name: String,
    val host: String,
    val port: String
) {
    fun endpoint(): String = if (host.isEmpty()) "—" else if (port.isEmpty()) host else "$host:$port"
}

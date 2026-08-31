package ir.panahannet.panah

data class Sub(
    val id: String,
    var name: String,
    var url: String,
    var cache: String = "",
    var updatedAt: Long = 0L,
    var lastError: String = "",
    var nodeCount: Int = 0
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

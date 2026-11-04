package reader.core.source

interface DocumentSource {
    val id: String
    val capabilities: Set<SourceCapability>
    suspend fun list(path: String): List<SourceEntry>
    suspend fun stat(path: String): SourceEntry
    suspend fun open(path: String): SourceHandle
}

enum class SourceCapability { LIST, READ, RANDOM_ACCESS, RANGE_READ, WRITE, DELETE, WATCH }

interface SourceHandle : AutoCloseable {
    val size: Long?
    suspend fun read(offset: Long, length: Int): ByteArray
    override fun close()
}

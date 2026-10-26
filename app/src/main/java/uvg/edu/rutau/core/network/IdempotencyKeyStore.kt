package uvg.edu.rutau.core.network

import java.util.UUID

/** Conserva una clave mientras una creación puede reintentarse sin duplicarse. */
class IdempotencyKeyStore {
    private val keys = mutableMapOf<String, String>()

    @Synchronized
    fun keyFor(operation: String): String = keys.getOrPut(operation) { UUID.randomUUID().toString() }

    @Synchronized
    fun complete(operation: String) {
        keys.remove(operation)
    }
}

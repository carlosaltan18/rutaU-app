package uvg.edu.rutau.core.data.catalog

import uvg.edu.rutau.core.network.BackendApi
import uvg.edu.rutau.core.network.requireEnvelope

/** Representa una opción real de un catálogo con su identificador y nombre. */
data class CatalogItem(
    val id: String,
    val name: String,
)

/** Indica cómo obtener universidades, campus y zonas del servidor. */
interface CatalogRepository {
    suspend fun universities(): List<CatalogItem>
    suspend fun campuses(universityId: String): List<CatalogItem>
    suspend fun zones(query: String? = null): List<CatalogItem>
}

/** Consulta los catálogos publicados por el backend de RutaU. */
class RemoteCatalogRepository(
    private val backendApi: BackendApi,
) : CatalogRepository {
    override suspend fun universities(): List<CatalogItem> = buildList {
        var cursor: String? = null
        do {
            val page = backendApi.getUniversities(cursor = cursor).requireEnvelope()
            addAll(page.data.map { CatalogItem(it.id, it.name) })
            cursor = page.meta.nextCursor
        } while (cursor != null)
    }

    override suspend fun campuses(universityId: String): List<CatalogItem> = buildList {
        if (universityId.isBlank()) return@buildList
        var cursor: String? = null
        do {
            val page = backendApi.getCampuses(universityId, cursor = cursor).requireEnvelope()
            addAll(page.data.map { CatalogItem(it.id, it.name) })
            cursor = page.meta.nextCursor
        } while (cursor != null)
    }

    override suspend fun zones(query: String?): List<CatalogItem> = buildList {
        var cursor: String? = null
        do {
            val page = backendApi.getZones(cursor = cursor, query = query).requireEnvelope()
            addAll(page.data.map { CatalogItem(it.id, it.name) })
            cursor = page.meta.nextCursor
        } while (cursor != null)
    }
}

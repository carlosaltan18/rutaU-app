package uvg.edu.rutau.core.data.catalog

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uvg.edu.rutau.core.network.BackendApi
import uvg.edu.rutau.core.network.requireEnvelope

/** Busca los nombres de catálogos para mostrar texto útil en vez de identificadores. */
class CatalogLabelResolver(
    private val backendApi: BackendApi,
) {
    private val mutex = Mutex()
    private val zones = mutableMapOf<String, String>()
    private val universities = mutableMapOf<String, String>()
    private val campuses = mutableMapOf<String, String>()
    private val loadedCampusUniversities = mutableSetOf<String>()

    suspend fun loadFor(universityIds: Set<String>) = mutex.withLock {
        if (zones.isEmpty()) loadZones()
        if (universities.isEmpty()) loadUniversities()
        for (universityId in universityIds) {
            if (universityId.isNotBlank() && loadedCampusUniversities.add(universityId)) {
                loadCampuses(universityId)
            }
        }
    }

    fun zoneName(id: String): String = zones[id] ?: id
    fun universityName(id: String): String = universities[id] ?: id
    fun campusName(id: String): String = campuses[id] ?: id

    private suspend fun loadZones() {
        var cursor: String? = null
        do {
            val page = backendApi.getZones(cursor = cursor).requireEnvelope()
            page.data.forEach { zones[it.id] = it.name }
            cursor = page.meta.nextCursor
        } while (cursor != null)
    }

    private suspend fun loadUniversities() {
        var cursor: String? = null
        do {
            val page = backendApi.getUniversities(cursor = cursor).requireEnvelope()
            page.data.forEach { universities[it.id] = it.name }
            cursor = page.meta.nextCursor
        } while (cursor != null)
    }

    private suspend fun loadCampuses(universityId: String) {
        var cursor: String? = null
        do {
            val page = backendApi.getCampuses(universityId, cursor = cursor).requireEnvelope()
            page.data.forEach { campuses[it.id] = it.name }
            cursor = page.meta.nextCursor
        } while (cursor != null)
    }
}

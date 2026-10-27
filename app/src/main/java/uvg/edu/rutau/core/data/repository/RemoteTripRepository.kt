package uvg.edu.rutau.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import uvg.edu.rutau.core.data.catalog.CatalogLabelResolver
import uvg.edu.rutau.core.model.Trip
import uvg.edu.rutau.core.model.TripInput
import uvg.edu.rutau.core.model.TripMatch
import uvg.edu.rutau.core.model.TripRole
import uvg.edu.rutau.core.network.BackendApi
import uvg.edu.rutau.core.network.BackendMatchDto
import uvg.edu.rutau.core.network.BackendTripDto
import uvg.edu.rutau.core.network.CreateTripRequestDto
import uvg.edu.rutau.core.network.IdempotencyKeyStore
import uvg.edu.rutau.core.network.UpdateTripRequestDto
import uvg.edu.rutau.core.network.nextTravelDate
import uvg.edu.rutau.core.network.requireData
import uvg.edu.rutau.core.network.requireEnvelope
import uvg.edu.rutau.core.network.toIsoDayOfWeek
import uvg.edu.rutau.core.network.toTrip
import uvg.edu.rutau.core.network.toTripMatch

/** Consulta y actualiza los trayectos usando los datos reales del servidor. */
class RemoteTripRepository(
    private val backendApi: BackendApi,
) : TripRepository {
    private val trips = MutableStateFlow<List<Trip>>(emptyList())
    private val versions = mutableMapOf<String, Int>()
    private val idempotencyKeys = IdempotencyKeyStore()
    private val labels = CatalogLabelResolver(backendApi)

    override fun observeTrips(): Flow<List<Trip>> = flow {
        refreshTrips()
        emitAll(trips)
    }

    override fun observeTrip(tripId: String): Flow<Trip?> = flow {
        val response = backendApi.getTrip(tripId).requireData()
        val trip = response.toDisplayTrip()
        versions[trip.id] = response.version
        replaceTrip(trip)
        emit(trip)
    }

    override suspend fun createTrip(input: TripInput): String {
        val operation = "trip:$input"
        val created = backendApi.createTrip(
            idempotencyKey = idempotencyKeys.keyFor(operation),
            body = input.toCreateRequest(),
        ).requireData()
        idempotencyKeys.complete(operation)
        val trip = created.toDisplayTrip()
        versions[trip.id] = created.version
        trips.value = (trips.value + trip).sortedByDescending { it.id }
        return trip.id
    }

    override suspend fun updateTrip(tripId: String, input: TripInput) {
        val version = versions[tripId] ?: backendApi.getTrip(tripId).requireData().also {
            versions[tripId] = it.version
        }.version
        val updated = backendApi.updateTrip(tripId, input.toUpdateRequest(version)).requireData()
        versions[tripId] = updated.version
        replaceTrip(updated.toDisplayTrip())
    }

    override suspend fun deleteTrip(tripId: String) {
        backendApi.deleteTrip(tripId).requireData()
        versions.remove(tripId)
        trips.value = trips.value.filterNot { it.id == tripId }
    }

    override suspend fun deactivateTrip(tripId: String) {
        val updated = backendApi.deactivateTrip(tripId).requireData()
        versions[tripId] = updated.version
        replaceTrip(updated.toDisplayTrip())
    }

    override fun observeMatches(tripId: String): Flow<List<TripMatch>> = flow {
        val source = trips.value.firstOrNull { it.id == tripId }
            ?: backendApi.getTrip(tripId).requireData().toDisplayTrip().also(::replaceTrip)
        val response = loadMatches(tripId, nextTravelDate(source.dayOfWeek))
        labels.loadFor(response.map { it.profile.universityId }.toSet())
        val matches = response.map {
            it.toTripMatch(
                originName = labels.zoneName(it.originZoneId),
                destinationName = labels.campusName(it.destinationCampusId),
                universityName = labels.universityName(it.profile.universityId),
                campusName = labels.campusName(it.profile.campusId),
            )
        }
        emit(matches)
    }

    private suspend fun loadMatches(tripId: String, travelDate: String): List<BackendMatchDto> = buildList {
        var cursor: String? = null
        do {
            val page = backendApi.getMatches(tripId, travelDate, cursor = cursor).requireEnvelope()
            addAll(page.data)
            cursor = page.meta.nextCursor
        } while (cursor != null)
    }

    private suspend fun refreshTrips() {
        loadLabels()
        val all = mutableListOf<Trip>()
        var cursor: String? = null
        do {
            val page = backendApi.getTrips(cursor = cursor).requireEnvelope()
            all += page.data.map { response ->
                versions[response.id] = response.version
                response.toDisplayTrip()
            }
            cursor = page.meta.nextCursor
        } while (cursor != null)
        trips.value = all
    }

    private fun replaceTrip(trip: Trip) {
        trips.value = trips.value.filterNot { it.id == trip.id } + trip
    }

    private suspend fun BackendTripDto.toDisplayTrip(): Trip {
        loadLabels()
        return toTrip(
            originName = labels.zoneName(originZoneId),
            destinationName = labels.campusName(destinationCampusId),
        )
    }

    private suspend fun loadLabels() {
        val profile = backendApi.getMe().requireData()
        labels.loadFor(setOf(profile.universityId))
    }
}

/** Convierte lo escrito en el formulario al cuerpo que valida el servidor. */
private fun TripInput.toCreateRequest(): CreateTripRequestDto = CreateTripRequestDto(
    originZoneId = originZone,
    destinationCampusId = destinationCampus,
    dayOfWeek = dayOfWeek.toIsoDayOfWeek(),
    departureTime = departureTime.toString(),
    role = role.name,
    seatCapacity = if (role == TripRole.DRIVER) offeredSeats else null,
)

/** Convierte una edición al cuerpo que incluye la versión observada. */
private fun TripInput.toUpdateRequest(version: Int): UpdateTripRequestDto = UpdateTripRequestDto(
    version = version,
    originZoneId = originZone,
    destinationCampusId = destinationCampus,
    dayOfWeek = dayOfWeek.toIsoDayOfWeek(),
    departureTime = departureTime.toString(),
    role = role.name,
    seatCapacity = if (role == TripRole.DRIVER) offeredSeats else null,
)

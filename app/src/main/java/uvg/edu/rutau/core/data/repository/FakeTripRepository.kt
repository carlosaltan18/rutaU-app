package uvg.edu.rutau.core.data.repository

import java.time.Duration
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import uvg.edu.rutau.core.data.mock.MockRutaUStore
import uvg.edu.rutau.core.model.Trip
import uvg.edu.rutau.core.model.TripInput
import uvg.edu.rutau.core.model.TripMatch
import uvg.edu.rutau.core.model.TripRole

class FakeTripRepository(
    private val store: MockRutaUStore,
) : TripRepository {
    override fun observeTrips(): Flow<List<Trip>> =
        combine(store.trips, store.currentUser) { trips, user ->
            trips.filter { it.ownerId == user?.id }
        }

    override fun observeTrip(tripId: String): Flow<Trip?> =
        store.trips.map { trips -> trips.firstOrNull { it.id == tripId } }

    override suspend fun createTrip(input: TripInput): String {
        validate(input)
        val ownerId = checkNotNull(store.currentUser.value).id
        val id = "trip-${UUID.randomUUID()}"
        store.trips.value += input.toTrip(id = id, ownerId = ownerId)
        return id
    }

    override suspend fun updateTrip(tripId: String, input: TripInput) {
        validate(input)
        store.trips.value = store.trips.value.map { existing ->
            if (existing.id == tripId) {
                input.toTrip(
                    id = existing.id,
                    ownerId = existing.ownerId,
                    occupiedSeats = existing.occupiedSeats.coerceAtMost(input.offeredSeats),
                    active = existing.active,
                )
            } else {
                existing
            }
        }
    }

    override suspend fun deleteTrip(tripId: String) {
        store.trips.value = store.trips.value.filterNot { it.id == tripId }
    }

    override suspend fun deactivateTrip(tripId: String) {
        store.trips.value = store.trips.value.map { trip ->
            if (trip.id == tripId) trip.copy(active = false) else trip
        }
    }

    override fun observeMatches(tripId: String): Flow<List<TripMatch>> =
        combine(store.trips, store.students) { trips, students ->
            val source = trips.firstOrNull { it.id == tripId } ?: return@combine emptyList()
            trips.asSequence()
                .filter { candidate -> candidate.isCompatibleWith(source) }
                .mapNotNull { candidate ->
                    students.firstOrNull { it.id == candidate.ownerId }?.let { student ->
                        TripMatch(
                            trip = candidate,
                            student = student,
                            timeDifferenceMinutes = kotlin.math.abs(
                                Duration.between(source.departureTime, candidate.departureTime).toMinutes().toInt(),
                            ),
                        )
                    }
                }
                .sortedWith(compareBy(TripMatch::timeDifferenceMinutes, { it.student.fullName }))
                .toList()
        }

    private fun Trip.isCompatibleWith(source: Trip): Boolean {
        val difference = kotlin.math.abs(Duration.between(source.departureTime, departureTime).toMinutes())
        return id != source.id &&
            ownerId != source.ownerId &&
            active && source.active &&
            originZone == source.originZone &&
            destinationCampus == source.destinationCampus &&
            dayOfWeek == source.dayOfWeek &&
            role != source.role &&
            difference <= 30 &&
            (role != TripRole.DRIVER || availableSeats > 0) &&
            (source.role != TripRole.DRIVER || source.availableSeats > 0)
    }

    private fun validate(input: TripInput) {
        require(input.originZone.isNotBlank())
        require(input.destinationCampus.isNotBlank())
        require(input.dayOfWeek.isNotBlank())
        if (input.role == TripRole.DRIVER) {
            require(input.offeredSeats in 1..3)
        } else {
            require(input.offeredSeats == 0)
        }
    }

    private fun TripInput.toTrip(
        id: String,
        ownerId: String,
        occupiedSeats: Int = 0,
        active: Boolean = true,
    ) = Trip(
        id = id,
        ownerId = ownerId,
        originZone = originZone.trim(),
        destinationCampus = destinationCampus,
        dayOfWeek = dayOfWeek,
        departureTime = departureTime,
        role = role,
        offeredSeats = offeredSeats,
        occupiedSeats = occupiedSeats,
        active = active,
    )
}

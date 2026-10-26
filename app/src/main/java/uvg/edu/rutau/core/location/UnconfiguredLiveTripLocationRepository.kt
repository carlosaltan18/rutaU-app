package uvg.edu.rutau.core.location

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Evita compartir ubicación hasta que exista una base Realtime configurada. */
class UnconfiguredLiveTripLocationRepository : LiveTripLocationRepository {
    override suspend fun publish(tripOccurrenceId: String, location: LiveTripLocation) {
        throw IllegalStateException("Configura Realtime Database antes de compartir ubicación.")
    }

    override suspend fun stopSharing(tripOccurrenceId: String) = Unit

    override fun observe(tripOccurrenceId: String): Flow<LiveTripLocation?> = flowOf(null)
}

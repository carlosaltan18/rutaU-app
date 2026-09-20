package uvg.edu.rutau.core.data.repository

import kotlinx.coroutines.flow.Flow
import uvg.edu.rutau.core.model.Trip
import uvg.edu.rutau.core.model.TripInput
import uvg.edu.rutau.core.model.TripMatch

interface TripRepository {
    fun observeTrips(): Flow<List<Trip>>
    fun observeTrip(tripId: String): Flow<Trip?>
    suspend fun createTrip(input: TripInput): String
    suspend fun updateTrip(tripId: String, input: TripInput)
    suspend fun deleteTrip(tripId: String)
    suspend fun deactivateTrip(tripId: String)
    fun observeMatches(tripId: String): Flow<List<TripMatch>>
}

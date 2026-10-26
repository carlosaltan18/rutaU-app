package uvg.edu.rutau.core.location

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

/** Guarda una ubicación compartida durante un viaje activo. */
data class LiveTripLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val bearing: Float,
    val speed: Float,
)

/** Envía y elimina ubicaciones de un viaje que está en curso. */
interface LiveTripLocationRepository {
    suspend fun publish(tripOccurrenceId: String, location: LiveTripLocation)
    suspend fun stopSharing(tripOccurrenceId: String)
    fun observe(tripOccurrenceId: String): Flow<LiveTripLocation?>
}

/** Guarda únicamente la última ubicación de un viaje activo. */
class FirebaseLiveTripLocationRepository(
    realtimeDatabaseUrl: String,
) : LiveTripLocationRepository {
    private val database = FirebaseDatabase.getInstance(
        realtimeDatabaseUrl.ifBlank {
            throw IllegalStateException("Configura RUTAU_REALTIME_DATABASE_URL para usar ubicación en vivo.")
        },
    )

    override suspend fun publish(
        tripOccurrenceId: String,
        location: LiveTripLocation,
    ) {
        if (tripOccurrenceId.isBlank()) return
        val value = mapOf<String, Any>(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "accuracy" to location.accuracy,
            "bearing" to location.bearing,
            "speed" to location.speed,
            "updatedAt" to ServerValue.TIMESTAMP,
        )
        database.reference.child("liveTrips/$tripOccurrenceId/location").setValue(value).await()
    }

    override suspend fun stopSharing(tripOccurrenceId: String) = Unit

    override fun observe(tripOccurrenceId: String): Flow<LiveTripLocation?> {
        if (tripOccurrenceId.isBlank()) return flowOf(null)
        return callbackFlow {
            val reference = database.reference.child("liveTrips/$tripOccurrenceId/location")
            val listener = reference.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val latitude = snapshot.child("latitude").getValue(Double::class.java)
                    val longitude = snapshot.child("longitude").getValue(Double::class.java)
                    if (latitude == null || longitude == null) {
                        trySend(null)
                        return
                    }
                    trySend(
                        LiveTripLocation(
                            latitude = latitude,
                            longitude = longitude,
                            accuracy = snapshot.child("accuracy").getValue(Double::class.java)?.toFloat() ?: 0f,
                            bearing = snapshot.child("bearing").getValue(Double::class.java)?.toFloat() ?: 0f,
                            speed = snapshot.child("speed").getValue(Double::class.java)?.toFloat() ?: 0f,
                        ),
                    )
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    close(error.toException())
                }
            })
            awaitClose { reference.removeEventListener(listener) }
        }
    }
}

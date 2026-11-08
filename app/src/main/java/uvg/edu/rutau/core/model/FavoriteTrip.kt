package uvg.edu.rutau.core.model

/** Resume una persona compatible que se guardó para revisarla incluso sin conexión. */
data class FavoriteTrip(
    val tripId: String,
    val studentName: String,
    val studentPhotoUrl: String?,
    val routeDescription: String,
    val scheduleDescription: String,
    val savedAtMillis: Long,
)

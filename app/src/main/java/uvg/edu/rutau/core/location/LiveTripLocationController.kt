package uvg.edu.rutau.core.location

/** Representa las condiciones necesarias para compartir una ubicación. */
data class LocationSharingSession(
    val tripOccurrenceId: String,
    val isConfirmed: Boolean,
    val hasLocationPermission: Boolean,
)

/** Permite iniciar y detener el servicio sin conocer Android directamente. */
interface LocationSharingService {
    fun start(tripOccurrenceId: String)
    fun stop()
}

/** Evita iniciar ubicación antes de confirmar el viaje y el permiso. */
class LiveTripLocationController(
    private val service: LocationSharingService,
) {
    fun start(session: LocationSharingSession): Boolean {
        if (!session.isConfirmed || !session.hasLocationPermission || session.tripOccurrenceId.isBlank()) return false
        service.start(session.tripOccurrenceId)
        return true
    }

    fun stop() {
        service.stop()
    }
}

package uvg.edu.rutau.core.model

import java.time.LocalDate
import java.time.LocalTime

/** El rol pertenece a un trayecto, nunca a la cuenta del estudiante. */
enum class TripRole {
    DRIVER,
    PASSENGER,
}

/** Indica si la coordinación fue iniciada por un pasajero o por un conductor. */
enum class RequestType {
    JOIN_REQUEST,
    DRIVER_INVITATION,
}

/** Estados posibles de una solicitud o invitación durante el MVP. */
enum class RequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELLED,
    EXPIRED,
}

/** Información pública mínima de una persona mostrada dentro de la aplicación. */
data class Student(
    val id: String,
    val fullName: String,
    val university: String,
    val campus: String,
    val photoUrl: String?,
)

/** Private account data that must not be exposed from public candidate profiles. */
data class UserAccount(
    val id: String,
    val fullName: String,
    val university: String,
    val campus: String,
    val email: String,
    val photoUrl: String?,
)

data class SignUpInput(
    val fullName: String,
    val university: String,
    val campus: String,
    val email: String,
    val password: String,
    val photoUrl: String? = null,
)

data class UpdateProfileInput(
    val fullName: String,
    val university: String,
    val campus: String,
    val photoUrl: String?,
)

data class UpdateEmailInput(
    val email: String,
    val currentPassword: String,
)

data class UpdatePasswordInput(
    val currentPassword: String,
    val newPassword: String,
)

/**
 * Trayecto habitual registrado por un estudiante.
 *
 * Para [TripRole.DRIVER], [offeredSeats] está entre 1 y 3. Para
 * [TripRole.PASSENGER], ambos campos de plazas deben permanecer en cero.
 */
data class Trip(
    val id: String,
    val ownerId: String,
    val originZone: String,
    val destinationCampus: String,
    val dayOfWeek: String,
    val departureTime: LocalTime,
    val role: TripRole,
    val offeredSeats: Int,
    val occupiedSeats: Int,
    val active: Boolean,
) {
    /** Plazas que todavía puede confirmar el conductor. */
    val availableSeats: Int
        get() = offeredSeats - occupiedSeats
}

/**
 * Solicitud de un pasajero o invitación de un conductor para una fecha concreta.
 * El importe se almacena en centavos; por ejemplo, Q 10.00 se representa como 1000.
 */
data class RideRequest(
    val id: String,
    val senderTripId: String,
    val targetTripId: String,
    val type: RequestType,
    val status: RequestStatus,
    val rideDate: LocalDate,
    val message: String?,
    val contributionCents: Long,
)

package uvg.edu.rutau.core.network

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import uvg.edu.rutau.core.model.Coordination
import uvg.edu.rutau.core.model.RequestStatus
import uvg.edu.rutau.core.model.RequestType
import uvg.edu.rutau.core.model.RideRequest
import uvg.edu.rutau.core.model.RideRequestDetails
import uvg.edu.rutau.core.model.Student
import uvg.edu.rutau.core.model.Trip
import uvg.edu.rutau.core.model.TripMatch
import uvg.edu.rutau.core.model.TripRole
import uvg.edu.rutau.core.model.UserAccount

/** Convierte las respuestas del servidor a los modelos que usa la aplicación. */
fun PrivateProfileDto.toUserAccount(
    universityName: String = universityId,
    campusName: String = campusId,
): UserAccount = UserAccount(
    id = id,
    fullName = displayName,
    university = universityName,
    campus = campusName,
    email = institutionalEmail,
    photoUrl = photoUrl,
    phone = phone,
    universityId = universityId,
    campusId = campusId,
)

/** Convierte un perfil público sin mostrar datos privados. */
fun PublicProfileDto.toStudent(): Student = Student(
    id = id,
    fullName = displayName,
    university = universityId,
    campus = campusId,
    photoUrl = photoUrl,
)

/** Convierte un trayecto del servidor y conserva su identificador real. */
fun BackendTripDto.toTrip(
    ownerId: String = "",
    originName: String = originZoneId,
    destinationName: String = destinationCampusId,
): Trip = Trip(
    id = id,
    ownerId = ownerId,
    originZone = originName,
    destinationCampus = destinationName,
    dayOfWeek = dayName(dayOfWeek),
    departureTime = departureTime.toLocalTimeOrStart(),
    role = role.toTripRole(),
    offeredSeats = seatCapacity ?: 0,
    occupiedSeats = 0,
    active = status == "ACTIVE",
    originZoneId = originZoneId,
    destinationCampusId = destinationCampusId,
)

/** Convierte un resumen de trayecto recibido en una solicitud. */
fun TripSummaryDto.toTrip(
    ownerId: String,
    availableSeats: Int = 0,
    originName: String = originZoneId,
    destinationName: String = destinationCampusId,
): Trip = Trip(
    id = id,
    ownerId = ownerId,
    originZone = originName,
    destinationCampus = destinationName,
    dayOfWeek = dayName(dayOfWeek),
    departureTime = departureTime.toLocalTimeOrStart(),
    role = role.toTripRole(),
    offeredSeats = if (role == "DRIVER") availableSeats.coerceAtLeast(0) else 0,
    occupiedSeats = 0,
    active = true,
    originZoneId = originZoneId,
    destinationCampusId = destinationCampusId,
)

/** Convierte una coincidencia segura para las pantallas de trayectos. */
fun BackendMatchDto.toTripMatch(
    originName: String = originZoneId,
    destinationName: String = destinationCampusId,
): TripMatch = TripMatch(
    trip = Trip(
        id = candidateTripId,
        ownerId = profile.id,
        originZone = originName,
        destinationCampus = destinationName,
        dayOfWeek = dayName(dayOfWeek),
        departureTime = departureTime.toLocalTimeOrStart(),
        role = role.toTripRole(),
        offeredSeats = if (role == "DRIVER") availableSeats.coerceAtLeast(0) else 0,
        occupiedSeats = 0,
        active = true,
        originZoneId = originZoneId,
        destinationCampusId = destinationCampusId,
    ),
    student = profile.toStudent(),
    timeDifferenceMinutes = timeDifferenceMinutes,
)

/** Convierte una solicitud completa para el detalle y su lista. */
fun BackendRideRequestDto.toRideRequestDetails(): RideRequestDetails {
    val request = RideRequest(
        id = id,
        senderTripId = senderTrip.id,
        targetTripId = targetTrip.id,
        type = type.toRequestType(),
        status = status.toRequestStatus(),
        rideDate = LocalDate.parse(travelDate),
        message = message,
        contributionCents = offeredContributionMinor,
        createdAt = createdAt.toLocalDateTimeOrNow(),
    )
    return RideRequestDetails(
        request = request,
        senderTrip = senderTrip.toTrip(sender.id, availableSeats),
        targetTrip = targetTrip.toTrip(target.id, availableSeats),
        sender = sender.toStudent(),
        target = target.toStudent(),
    )
}

/** Convierte una coordinación sin añadir campos que el servidor no entrega. */
fun BackendCoordinationDto.toCoordination(): Coordination = Coordination(
    id = id,
    requestId = rideRequestId,
    driverTripId = "",
    passengerTripId = "",
    rideDate = LocalDate.parse(travelDate),
    contributionCents = agreedContributionMinor,
    liveOccurrenceId = liveLocation.occurrenceId,
    canStartLiveLocation = liveLocation.canStart,
    canEndLiveLocation = liveLocation.canEnd,
)

/** Convierte el texto del día del formulario al número ISO que usa el servidor. */
fun String.toIsoDayOfWeek(): Int = when (trim().lowercase()) {
    "lunes" -> 1
    "martes" -> 2
    "miércoles", "miercoles" -> 3
    "jueves" -> 4
    "viernes" -> 5
    "sábado", "sabado" -> 6
    "domingo" -> 7
    else -> throw IllegalArgumentException("Selecciona un día válido.")
}

/** Obtiene el siguiente día de calendario que corresponde al día del trayecto. */
fun nextTravelDate(dayName: String): String {
    val today = LocalDate.now()
    val target = dayName.toIsoDayOfWeek()
    val difference = (target - today.dayOfWeek.value + 7) % 7
    return today.plusDays(if (difference == 0) 7 else difference.toLong()).toString()
}

private fun String.toTripRole(): TripRole = when (this) {
    "DRIVER" -> TripRole.DRIVER
    "PASSENGER" -> TripRole.PASSENGER
    else -> throw IllegalArgumentException("El servidor devolvió un rol desconocido.")
}

private fun String.toRequestType(): RequestType = when (this) {
    "JOIN_REQUEST" -> RequestType.JOIN_REQUEST
    "DRIVER_INVITATION" -> RequestType.DRIVER_INVITATION
    else -> throw IllegalArgumentException("El servidor devolvió un tipo desconocido.")
}

private fun String.toRequestStatus(): RequestStatus = when (this) {
    "PENDING" -> RequestStatus.PENDING
    "ACCEPTED" -> RequestStatus.ACCEPTED
    "REJECTED" -> RequestStatus.REJECTED
    "CANCELLED" -> RequestStatus.CANCELLED
    "EXPIRED" -> RequestStatus.EXPIRED
    else -> throw IllegalArgumentException("El servidor devolvió un estado desconocido.")
}

private fun String.toLocalTimeOrStart(): LocalTime = runCatching { LocalTime.parse(take(5)) }
    .getOrDefault(LocalTime.MIDNIGHT)

private fun String?.toLocalDateTimeOrNow(): LocalDateTime = this?.let { value ->
    runCatching { OffsetDateTime.parse(value).toLocalDateTime() }
        .recoverCatching { LocalDateTime.parse(value) }
        .getOrNull()
} ?: LocalDateTime.now()

private fun dayName(value: Int): String = when (value) {
    1 -> "Lunes"
    2 -> "Martes"
    3 -> "Miércoles"
    4 -> "Jueves"
    5 -> "Viernes"
    6 -> "Sábado"
    7 -> "Domingo"
    else -> "Lunes"
}

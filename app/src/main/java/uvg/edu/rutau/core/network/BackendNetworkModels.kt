package uvg.edu.rutau.core.network

import com.google.gson.annotations.SerializedName

/** Representa la envoltura que usa el servidor para respuestas correctas. */
data class ApiEnvelopeDto<T>(
    @SerializedName("data") val data: T,
    @SerializedName("meta") val meta: ApiMetaDto,
)

/** Guarda datos técnicos de una respuesta sin datos personales. */
data class ApiMetaDto(
    @SerializedName("requestId") val requestId: String? = null,
    @SerializedName("nextCursor") val nextCursor: String? = null,
)

/** Guarda los datos aceptados al crear o recuperar una sesión. */
data class SessionRequestDto(
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("institutionalEmail") val institutionalEmail: String? = null,
    @SerializedName("universityId") val universityId: String? = null,
    @SerializedName("campusId") val campusId: String? = null,
    @SerializedName("photoUrl") val photoUrl: String? = null,
    @SerializedName("phone") val phone: String? = null,
)

/** Describe una sesión creada o recuperada por el servidor. */
data class BackendSessionDto(
    @SerializedName("authenticated") val authenticated: Boolean,
    @SerializedName("created") val created: Boolean,
    @SerializedName("profile") val profile: PrivateProfileDto,
)

/** Describe el perfil completo que solo recibe la persona propietaria. */
data class PrivateProfileDto(
    @SerializedName("id") val id: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("photoUrl") val photoUrl: String? = null,
    @SerializedName("institutionalEmail") val institutionalEmail: String,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("emailVerified") val emailVerified: Boolean,
    @SerializedName("universityId") val universityId: String,
    @SerializedName("campusId") val campusId: String,
    @SerializedName("accountStatus") val accountStatus: String,
)

/** Describe una persona de forma pública dentro de un trayecto. */
data class PublicProfileDto(
    @SerializedName("id") val id: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("photoUrl") val photoUrl: String? = null,
    @SerializedName("universityId") val universityId: String,
    @SerializedName("campusId") val campusId: String,
)

/** Describe una universidad disponible al registrar una cuenta. */
data class UniversityDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
)

/** Describe un campus disponible al registrar una cuenta o trayecto. */
data class CampusDto(
    @SerializedName("id") val id: String,
    @SerializedName("universityId") val universityId: String,
    @SerializedName("name") val name: String,
)

/** Describe una zona disponible como punto de origen. */
data class ZoneDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("municipality") val municipality: String,
    @SerializedName("department") val department: String,
)

/** Guarda los datos que el servidor permite cambiar de una cuenta. */
data class UpdateMeRequestDto(
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("photoUrl") val photoUrl: String? = null,
    @SerializedName("universityId") val universityId: String? = null,
    @SerializedName("campusId") val campusId: String? = null,
    @SerializedName("phone") val phone: String? = null,
)

/** Guarda los datos necesarios para crear un trayecto. */
data class CreateTripRequestDto(
    @SerializedName("originZoneId") val originZoneId: String,
    @SerializedName("destinationCampusId") val destinationCampusId: String,
    @SerializedName("dayOfWeek") val dayOfWeek: Int,
    @SerializedName("departureTime") val departureTime: String,
    @SerializedName("role") val role: String,
    @SerializedName("seatCapacity") val seatCapacity: Int?,
    @SerializedName("version") val version: Int = 0,
)

/** Guarda los cambios permitidos para un trayecto. */
data class UpdateTripRequestDto(
    @SerializedName("version") val version: Int,
    @SerializedName("originZoneId") val originZoneId: String? = null,
    @SerializedName("destinationCampusId") val destinationCampusId: String? = null,
    @SerializedName("dayOfWeek") val dayOfWeek: Int? = null,
    @SerializedName("departureTime") val departureTime: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("seatCapacity") val seatCapacity: Int? = null,
)

/** Describe un trayecto habitual devuelto por el servidor. */
data class BackendTripDto(
    @SerializedName("id") val id: String,
    @SerializedName("originZoneId") val originZoneId: String,
    @SerializedName("destinationCampusId") val destinationCampusId: String,
    @SerializedName("dayOfWeek") val dayOfWeek: Int,
    @SerializedName("departureTime") val departureTime: String,
    @SerializedName("role") val role: String,
    @SerializedName("seatCapacity") val seatCapacity: Int?,
    @SerializedName("status") val status: String,
    @SerializedName("version") val version: Int,
)

/** Describe el trayecto resumido que acompaña una solicitud. */
data class TripSummaryDto(
    @SerializedName("id") val id: String,
    @SerializedName("originZoneId") val originZoneId: String,
    @SerializedName("destinationCampusId") val destinationCampusId: String,
    @SerializedName("dayOfWeek") val dayOfWeek: Int,
    @SerializedName("departureTime") val departureTime: String,
    @SerializedName("role") val role: String,
)

/** Describe una coincidencia de trayecto que puede recibir una solicitud. */
data class BackendMatchDto(
    @SerializedName("candidateTripId") val candidateTripId: String,
    @SerializedName("profile") val profile: PublicProfileDto,
    @SerializedName("role") val role: String,
    @SerializedName("originZoneId") val originZoneId: String,
    @SerializedName("destinationCampusId") val destinationCampusId: String,
    @SerializedName("dayOfWeek") val dayOfWeek: Int,
    @SerializedName("departureTime") val departureTime: String,
    @SerializedName("timeDifferenceMinutes") val timeDifferenceMinutes: Int,
    @SerializedName("reasons") val reasons: List<String>,
    @SerializedName("availableSeats") val availableSeats: Int,
    @SerializedName("canRequest") val canRequest: Boolean,
)

/** Guarda los datos necesarios para solicitar o invitar un viaje. */
data class CreateRideRequestRequestDto(
    @SerializedName("senderTripId") val senderTripId: String,
    @SerializedName("targetTripId") val targetTripId: String,
    @SerializedName("type") val type: String,
    @SerializedName("travelDate") val travelDate: String,
    @SerializedName("offeredContributionMinor") val offeredContributionMinor: Long,
    @SerializedName("currency") val currency: String = "GTQ",
    @SerializedName("message") val message: String? = null,
)

/** Describe una solicitud o invitación devuelta por el servidor. */
data class BackendRideRequestDto(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("status") val status: String,
    @SerializedName("travelDate") val travelDate: String,
    @SerializedName("message") val message: String? = null,
    @SerializedName("offeredContributionMinor") val offeredContributionMinor: Long,
    @SerializedName("currency") val currency: String,
    @SerializedName("senderTrip") val senderTrip: TripSummaryDto,
    @SerializedName("targetTrip") val targetTrip: TripSummaryDto,
    @SerializedName("sender") val sender: PublicProfileDto,
    @SerializedName("target") val target: PublicProfileDto,
    @SerializedName("timeDifferenceMinutes") val timeDifferenceMinutes: Int,
    @SerializedName("availableSeats") val availableSeats: Int,
    @SerializedName("coordination") val coordination: CoordinationSummaryDto? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
)

/** Describe la coordinación resumida que acompaña una solicitud aceptada. */
data class CoordinationSummaryDto(
    @SerializedName("id") val id: String,
    @SerializedName("status") val status: String,
    @SerializedName("plannedDepartureTime") val plannedDepartureTime: String,
)

/** Describe el estado de ubicación en vivo de una coordinación. */
data class LiveLocationDto(
    @SerializedName("occurrenceId") val occurrenceId: String,
    @SerializedName("status") val status: String,
    @SerializedName("canStart") val canStart: Boolean,
    @SerializedName("canEnd") val canEnd: Boolean,
)

/** Describe una coordinación visible para uno de sus participantes. */
data class BackendCoordinationDto(
    @SerializedName("id") val id: String,
    @SerializedName("rideRequestId") val rideRequestId: String,
    @SerializedName("status") val status: String,
    @SerializedName("travelDate") val travelDate: String,
    @SerializedName("plannedDepartureTime") val plannedDepartureTime: String,
    @SerializedName("originZoneId") val originZoneId: String,
    @SerializedName("destinationCampusId") val destinationCampusId: String,
    @SerializedName("agreedContributionMinor") val agreedContributionMinor: Long,
    @SerializedName("currency") val currency: String,
    @SerializedName("driver") val driver: PublicProfileDto,
    @SerializedName("passenger") val passenger: PublicProfileDto,
    @SerializedName("liveLocation") val liveLocation: LiveLocationDto,
)

/** Guarda el token de FCM que se asocia con el dispositivo actual. */
data class RegisterDeviceRequestDto(
    @SerializedName("token") val token: String,
    @SerializedName("platform") val platform: String = "ANDROID",
)

/** Describe el dispositivo que quedó asociado a la sesión. */
data class RegisteredDeviceDto(
    @SerializedName("id") val id: String,
    @SerializedName("platform") val platform: String,
)

/** Guarda los datos de una notificación interna de RutaU. */
data class BackendNotificationDto(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("resourceId") val resourceId: String? = null,
    @SerializedName("readAt") val readAt: String? = null,
)

/** Guarda el error que el servidor publica de forma uniforme. */
data class ApiErrorEnvelopeDto(
    @SerializedName("error") val error: ApiErrorDto,
)

/** Guarda el texto seguro de un error del servidor. */
data class ApiErrorDto(
    @SerializedName("code") val code: String,
    @SerializedName("message") val message: String,
    @SerializedName("fieldErrors") val fieldErrors: List<ApiFieldErrorDto> = emptyList(),
    @SerializedName("requestId") val requestId: String? = null,
)

/** Guarda un campo que el servidor no pudo validar. */
data class ApiFieldErrorDto(
    @SerializedName("field") val field: String,
    @SerializedName("message") val message: String,
)

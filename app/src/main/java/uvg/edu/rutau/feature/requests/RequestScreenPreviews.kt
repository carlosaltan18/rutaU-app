package uvg.edu.rutau.feature.requests

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import java.time.LocalDate
import java.time.LocalTime
import uvg.edu.rutau.core.model.Coordination
import uvg.edu.rutau.core.model.RequestStatus
import uvg.edu.rutau.core.model.RequestType
import uvg.edu.rutau.core.model.RideRequest
import uvg.edu.rutau.core.model.RideRequestDetails
import uvg.edu.rutau.core.model.Student
import uvg.edu.rutau.core.model.Trip
import uvg.edu.rutau.core.model.TripRole
import uvg.edu.rutau.ui.theme.RutaUTheme

/** Contiene ejemplos visuales de las pantallas de solicitudes. */
@Preview(showBackground = true)
@Composable
private fun RequestsScreenPreview() {
    RutaUTheme {
        RequestsScreen(
            state = RequestsUiState(requests = listOf(previewDetail)),
            onTabSelected = {},
            onFilterSelected = {},
            onOpenRequest = {},
        )
    }
}

/** Muestra un ejemplo visual del detalle de una solicitud pendiente. */
@Preview(showBackground = true)
@Composable
private fun RequestDetailScreenPreview() {
    RutaUTheme {
        RequestDetailScreen(
            state = RequestDetailUiState(detail = previewDetail, currentUserId = "user-mateo"),
            onBack = {},
            onAccept = {},
            onReject = {},
            onCancel = {},
            onOpenCoordinatedRide = {},
            onSearchAgain = {},
        )
    }
}

/** Muestra un ejemplo visual de un viaje ya aceptado. */
@Preview(showBackground = true)
@Composable
private fun CoordinatedRideScreenPreview() {
    RutaUTheme {
        CoordinatedRideScreen(
            state = CoordinatedRideUiState(
                detail = previewDetail.copy(request = previewDetail.request.copy(status = RequestStatus.ACCEPTED)),
                coordination = Coordination(
                    id = "coord-preview",
                    requestId = "request-preview",
                    driverTripId = "trip-mateo",
                    passengerTripId = "trip-carlos",
                    rideDate = LocalDate.of(2026, 9, 22),
                    contributionCents = 1000,
                ),
                currentUserId = "user-mateo",
            ),
            onBack = {},
            onCancelRide = {},
            onCancelParticipation = {},
        )
    }
}

/** Muestra el estado visual de un vehículo con todas sus plazas ocupadas. */
@Preview(showBackground = true)
@Composable
private fun VehicleCapacityFullPreview() {
    RutaUTheme {
        VehicleCapacityCard(
            driver = previewDetail.targetTrip.copy(occupiedSeats = 3),
        )
    }
}

/** Reúne datos cortos para mostrar los ejemplos visuales. */
private val previewDetail = RideRequestDetails(
    request = RideRequest(
        id = "request-preview",
        senderTripId = "trip-carlos",
        targetTripId = "trip-mateo",
        type = RequestType.JOIN_REQUEST,
        status = RequestStatus.PENDING,
        rideDate = LocalDate.of(2026, 9, 22),
        message = "Hola Mateo, ¿puedo unirme a tu viaje?",
        contributionCents = 1000,
    ),
    senderTrip = Trip(
        id = "trip-carlos",
        ownerId = "user-carlos",
        originZone = "Zona 11",
        destinationCampus = "Campus Central",
        dayOfWeek = "Martes",
        departureTime = LocalTime.of(7, 0),
        role = TripRole.PASSENGER,
        offeredSeats = 0,
        occupiedSeats = 0,
        active = true,
    ),
    targetTrip = Trip(
        id = "trip-mateo",
        ownerId = "user-mateo",
        originZone = "Zona 11",
        destinationCampus = "Campus Central",
        dayOfWeek = "Martes",
        departureTime = LocalTime.of(7, 0),
        role = TripRole.DRIVER,
        offeredSeats = 3,
        occupiedSeats = 2,
        active = true,
    ),
    sender = Student("user-carlos", "Carlos Méndez", "Universidad San Carlos", "Campus Central", null),
    target = Student("user-mateo", "Mateo Morales", "Universidad San Carlos", "Campus Central", null),
)

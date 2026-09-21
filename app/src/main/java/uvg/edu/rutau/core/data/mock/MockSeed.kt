package uvg.edu.rutau.core.data.mock

import java.time.LocalDate
import java.time.LocalTime
import uvg.edu.rutau.core.model.RequestStatus
import uvg.edu.rutau.core.model.RequestType
import uvg.edu.rutau.core.model.Coordination
import uvg.edu.rutau.core.model.RideRequest
import uvg.edu.rutau.core.model.Student
import uvg.edu.rutau.core.model.Trip
import uvg.edu.rutau.core.model.TripRole
import uvg.edu.rutau.core.model.UserAccount

/** Reúne los datos de ejemplo que usa la aplicación mientras no hay servidor. */
object MockSeed {
    const val DefaultPassword = "RutaU123"

    val currentUser = UserAccount(
        id = "user-mateo",
        fullName = "Mateo Morales Silva",
        university = "Universidad San Carlos",
        campus = "Campus Central",
        email = "mateo@ejemplo.com",
        photoUrl = null,
    )

    val students = listOf(
        Student("user-andrea", "Andrea López", "Universidad San Carlos", "Campus Central", null),
        Student("user-diego", "Diego Pérez", "Universidad San Carlos", "Campus Central", null),
        Student("user-sofia", "Sofía Ramírez", "Universidad San Carlos", "Campus Central", null),
        Student("user-carlos", "Carlos Méndez", "Universidad San Carlos", "Campus Central", null),
    )

    val trips = listOf(
        Trip(
            id = "trip-mateo-passenger",
            ownerId = currentUser.id,
            originZone = "Zona 11",
            destinationCampus = "Campus Central",
            dayOfWeek = "Lunes",
            departureTime = LocalTime.of(6, 30),
            role = TripRole.PASSENGER,
            offeredSeats = 0,
            occupiedSeats = 0,
            active = true,
        ),
        Trip(
            id = "trip-mateo-driver",
            ownerId = currentUser.id,
            originZone = "Zona 11",
            destinationCampus = "Campus Central",
            dayOfWeek = "Martes",
            departureTime = LocalTime.of(7, 0),
            role = TripRole.DRIVER,
            offeredSeats = 3,
            occupiedSeats = 2,
            active = true,
        ),
        Trip(
            id = "trip-andrea-driver",
            ownerId = "user-andrea",
            originZone = "Zona 11",
            destinationCampus = "Campus Central",
            dayOfWeek = "Lunes",
            departureTime = LocalTime.of(6, 15),
            role = TripRole.DRIVER,
            offeredSeats = 3,
            occupiedSeats = 1,
            active = true,
        ),
        Trip(
            id = "trip-diego-driver",
            ownerId = "user-diego",
            originZone = "Zona 11",
            destinationCampus = "Campus Central",
            dayOfWeek = "Lunes",
            departureTime = LocalTime.of(6, 30),
            role = TripRole.DRIVER,
            offeredSeats = 1,
            occupiedSeats = 0,
            active = true,
        ),
        Trip(
            id = "trip-sofia-passenger",
            ownerId = "user-sofia",
            originZone = "Zona 11",
            destinationCampus = "Campus Central",
            dayOfWeek = "Martes",
            departureTime = LocalTime.of(6, 45),
            role = TripRole.PASSENGER,
            offeredSeats = 0,
            occupiedSeats = 0,
            active = true,
        ),
        Trip(
            id = "trip-carlos-passenger",
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
        Trip(
            id = "trip-andrea-driver-full",
            ownerId = "user-andrea",
            originZone = "Zona 10",
            destinationCampus = "Campus Central",
            dayOfWeek = "Miércoles",
            departureTime = LocalTime.of(7, 0),
            role = TripRole.DRIVER,
            offeredSeats = 3,
            occupiedSeats = 3,
            active = true,
        ),
    )

    val requests = listOf(
        RideRequest(
            id = "request-pending",
            senderTripId = "trip-mateo-passenger",
            targetTripId = "trip-andrea-driver",
            type = RequestType.JOIN_REQUEST,
            status = RequestStatus.PENDING,
            rideDate = LocalDate.of(2026, 9, 21),
            message = "Hola Andrea, ¿podemos coordinar este lunes?",
            contributionCents = 1000,
        ),
        RideRequest(
            id = "request-invitation-pending",
            senderTripId = "trip-mateo-driver",
            targetTripId = "trip-sofia-passenger",
            type = RequestType.DRIVER_INVITATION,
            status = RequestStatus.PENDING,
            rideDate = LocalDate.of(2026, 9, 22),
            message = "Hola Sofía, todavía tengo una plaza disponible.",
            contributionCents = 1000,
        ),
        RideRequest(
            id = "request-received-join",
            senderTripId = "trip-carlos-passenger",
            targetTripId = "trip-mateo-driver",
            type = RequestType.JOIN_REQUEST,
            status = RequestStatus.PENDING,
            rideDate = LocalDate.of(2026, 9, 22),
            message = "Hola Mateo, ¿puedo unirme a tu viaje?",
            contributionCents = 1000,
        ),
        RideRequest(
            id = "request-accepted-sofia",
            senderTripId = "trip-mateo-driver",
            targetTripId = "trip-sofia-passenger",
            type = RequestType.DRIVER_INVITATION,
            status = RequestStatus.ACCEPTED,
            rideDate = LocalDate.of(2026, 9, 22),
            message = "Nos vemos mañana.",
            contributionCents = 1000,
        ),
        RideRequest(
            id = "request-accepted-carlos",
            senderTripId = "trip-mateo-driver",
            targetTripId = "trip-carlos-passenger",
            type = RequestType.DRIVER_INVITATION,
            status = RequestStatus.ACCEPTED,
            rideDate = LocalDate.of(2026, 9, 22),
            message = null,
            contributionCents = 1000,
        ),
        RideRequest(
            id = "request-rejected",
            senderTripId = "trip-mateo-passenger",
            targetTripId = "trip-diego-driver",
            type = RequestType.JOIN_REQUEST,
            status = RequestStatus.REJECTED,
            rideDate = LocalDate.of(2026, 9, 21),
            message = "¿Aún tienes espacio?",
            contributionCents = 1000,
        ),
        RideRequest(
            id = "request-cancelled",
            senderTripId = "trip-mateo-passenger",
            targetTripId = "trip-andrea-driver",
            type = RequestType.JOIN_REQUEST,
            status = RequestStatus.CANCELLED,
            rideDate = LocalDate.of(2026, 9, 21),
            message = null,
            contributionCents = 1000,
        ),
        RideRequest(
            id = "request-expired",
            senderTripId = "trip-mateo-passenger",
            targetTripId = "trip-andrea-driver-full",
            type = RequestType.JOIN_REQUEST,
            status = RequestStatus.EXPIRED,
            rideDate = LocalDate.of(2026, 9, 24),
            message = "¿Podemos coordinar este viaje?",
            contributionCents = 1000,
        ),
    )

    val coordinations = listOf(
        Coordination(
            id = "coord-mateo-sofia",
            requestId = "request-accepted-sofia",
            driverTripId = "trip-mateo-driver",
            passengerTripId = "trip-sofia-passenger",
            rideDate = LocalDate.of(2026, 9, 22),
            contributionCents = 1000,
        ),
        Coordination(
            id = "coord-mateo-carlos",
            requestId = "request-accepted-carlos",
            driverTripId = "trip-mateo-driver",
            passengerTripId = "trip-carlos-passenger",
            rideDate = LocalDate.of(2026, 9, 22),
            contributionCents = 1000,
        ),
    )
}

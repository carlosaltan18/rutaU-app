package uvg.edu.rutau.core.data.mock

import java.time.LocalDate
import java.time.LocalTime
import uvg.edu.rutau.core.model.RequestStatus
import uvg.edu.rutau.core.model.RequestType
import uvg.edu.rutau.core.model.RideRequest
import uvg.edu.rutau.core.model.Student
import uvg.edu.rutau.core.model.Trip
import uvg.edu.rutau.core.model.TripRole
import uvg.edu.rutau.core.model.UserAccount

/** Centralized sample data shared by all fake repositories during the frontend phase. */
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
    )
}

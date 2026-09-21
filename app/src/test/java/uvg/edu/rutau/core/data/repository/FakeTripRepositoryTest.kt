package uvg.edu.rutau.core.data.repository

import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uvg.edu.rutau.core.data.mock.MockRutaUStore
import uvg.edu.rutau.core.model.RequestStatus
import uvg.edu.rutau.core.model.RequestType
import uvg.edu.rutau.core.model.TripInput
import uvg.edu.rutau.core.model.TripRole
import uvg.edu.rutau.core.model.Trip
import uvg.edu.rutau.core.model.Student

class FakeTripRepositoryTest {
    @Test
    fun `passenger receives only compatible drivers with seats`() = runBlocking {
        val repository = FakeTripRepository(MockRutaUStore())

        val matches = repository.observeMatches("trip-mateo-passenger").first()

        assertEquals(listOf("Diego Pérez", "Andrea López"), matches.map { it.student.fullName })
        assertTrue(matches.all { it.trip.role == TripRole.DRIVER && it.trip.availableSeats > 0 })
        assertTrue(matches.all { it.timeDifferenceMinutes <= 30 })
    }

    @Test
    fun `driver receives compatible passengers`() = runBlocking {
        val repository = FakeTripRepository(MockRutaUStore())

        val matches = repository.observeMatches("trip-mateo-driver").first()

        assertEquals(listOf("Carlos Méndez", "Sofía Ramírez"), matches.map { it.student.fullName })
        assertTrue(matches.all { it.trip.role == TripRole.PASSENGER })
    }

    @Test
    fun `matches exclude same role late candidates and full drivers`() = runBlocking {
        val store = MockRutaUStore()
        val repository = FakeTripRepository(store)
        store.students.value += listOf(
            Student("user-late", "Tarde", "Universidad San Carlos", "Campus Central", null),
            Student("user-passenger", "Pasajero", "Universidad San Carlos", "Campus Central", null),
            Student("user-full", "Lleno", "Universidad San Carlos", "Campus Central", null),
        )
        store.trips.value += listOf(
            Trip(
                "trip-late-driver", "user-late", "Zona 11", "Campus Central", "Lunes",
                LocalTime.of(7, 1), TripRole.DRIVER, 2, 0, true,
            ),
            Trip(
                "trip-same-role", "user-passenger", "Zona 11", "Campus Central", "Lunes",
                LocalTime.of(6, 30), TripRole.PASSENGER, 0, 0, true,
            ),
            Trip(
                "trip-full-driver", "user-full", "Zona 11", "Campus Central", "Lunes",
                LocalTime.of(6, 30), TripRole.DRIVER, 1, 1, true,
            ),
        )

        val matches = repository.observeMatches("trip-mateo-passenger").first()

        assertFalse(matches.any { it.trip.id in setOf("trip-late-driver", "trip-same-role", "trip-full-driver") })
    }

    @Test
    fun `creates updates deactivates and deletes a trip`() = runBlocking {
        val repository = FakeTripRepository(MockRutaUStore())
        val createdId = repository.createTrip(
            TripInput("Zona 7", "Campus Central", "Viernes", LocalTime.of(8, 0), TripRole.DRIVER, 2),
        )
        assertTrue(repository.observeTrips().first().any { it.id == createdId && it.offeredSeats == 2 })

        repository.updateTrip(
            createdId,
            TripInput("Zona 10", "Campus Central", "Viernes", LocalTime.of(8, 30), TripRole.PASSENGER),
        )
        assertEquals(TripRole.PASSENGER, repository.observeTrip(createdId).first()?.role)
        assertEquals(0, repository.observeTrip(createdId).first()?.offeredSeats)

        repository.deactivateTrip(createdId)
        assertFalse(repository.observeTrip(createdId).first()?.active ?: true)

        repository.deleteTrip(createdId)
        assertEquals(null, repository.observeTrip(createdId).first())
    }

    @Test
    fun `driver capacity supports one two and three seats`() = runBlocking {
        val repository = FakeTripRepository(MockRutaUStore())

        (1..3).forEach { seats ->
            val id = repository.createTrip(
                TripInput("Zona 7", "Campus Central", "Viernes", LocalTime.of(8, 0), TripRole.DRIVER, seats),
            )
            assertEquals(seats, repository.observeTrip(id).first()?.offeredSeats)
        }
    }

    @Test
    fun `editing cannot reduce capacity below confirmed passengers`() = runBlocking {
        val repository = FakeTripRepository(MockRutaUStore())
        val original = repository.observeTrip("trip-mateo-driver").first()!!

        val failure = try {
            repository.updateTrip(
                original.id,
                TripInput(
                    originZone = original.originZone,
                    destinationCampus = original.destinationCampus,
                    dayOfWeek = original.dayOfWeek,
                    departureTime = original.departureTime,
                    role = TripRole.DRIVER,
                    offeredSeats = 1,
                ),
            )
            null
        } catch (error: Throwable) {
            error
        }

        assertTrue(failure is IllegalArgumentException)
        val unchanged = repository.observeTrip(original.id).first()!!
        assertEquals(3, unchanged.offeredSeats)
        assertEquals(2, unchanged.occupiedSeats)
    }

    @Test
    fun `pending request does not reduce available seats`() = runBlocking {
        val store = MockRutaUStore()
        val trips = FakeTripRepository(store)
        val coordination = FakeCoordinationRepository(store)
        val seatsBefore = trips.observeTrip("trip-andrea-driver").first()!!.availableSeats

        val requestId = coordination.createPendingCoordination(
            tripId = "trip-mateo-passenger",
            candidateTripId = "trip-andrea-driver",
            type = RequestType.JOIN_REQUEST,
            message = "¿Coordinamos?",
            contributionCents = 1000,
        )

        assertEquals(seatsBefore, trips.observeTrip("trip-andrea-driver").first()!!.availableSeats)
        assertEquals(RequestStatus.PENDING, store.requests.value.first { it.id == requestId }.status)
    }

    @Test
    fun `passenger request and driver invitation are both created as pending`() = runBlocking {
        val store = MockRutaUStore()
        val coordination = FakeCoordinationRepository(store)

        val joinRequestId = coordination.createPendingCoordination(
            tripId = "trip-mateo-passenger",
            candidateTripId = "trip-andrea-driver",
            type = RequestType.JOIN_REQUEST,
            message = "¿Podemos coordinar?",
            contributionCents = 1_000,
        )
        val invitationId = coordination.createPendingCoordination(
            tripId = "trip-mateo-driver",
            candidateTripId = "trip-sofia-passenger",
            type = RequestType.DRIVER_INVITATION,
            message = "Te invito a mi trayecto.",
            contributionCents = 1_000,
        )

        assertEquals(RequestType.JOIN_REQUEST, store.requests.value.first { it.id == joinRequestId }.type)
        assertEquals(RequestType.DRIVER_INVITATION, store.requests.value.first { it.id == invitationId }.type)
        assertTrue(store.requests.value.filter { it.id in setOf(joinRequestId, invitationId) }.all {
            it.status == RequestStatus.PENDING
        })
    }
}

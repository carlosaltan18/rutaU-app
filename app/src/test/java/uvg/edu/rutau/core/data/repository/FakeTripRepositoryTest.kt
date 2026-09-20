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
}

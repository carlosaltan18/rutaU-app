package uvg.edu.rutau.core.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uvg.edu.rutau.core.data.mock.MockRutaUStore
import uvg.edu.rutau.core.model.RequestStatus
import uvg.edu.rutau.core.model.RequestType

/** Comprueba los cambios de estado de una solicitud. */
class FakeRideRequestRepositoryTest {
    @Test
    fun `mock data includes every request state and both pending directions`() = runBlocking {
        val store = MockRutaUStore()
        val requests = FakeRideRequestRepository(store).observeRequestDetails().first()

        assertTrue(requests.any { it.request.id == "request-pending" })
        assertTrue(requests.any { it.request.id == "request-invitation-pending" })
        assertTrue(requests.any { it.request.id == "request-received-join" })
        assertTrue(requests.any { it.request.id == "request-received-invitation" })
        assertTrue(RequestStatus.entries.all { status -> requests.any { it.request.status == status } })
        assertTrue(requests.all { it.request.createdAt.year == 2026 })
    }

    @Test
    fun `accepting a request creates a coordination and occupies one driver seat`() = runBlocking {
        val store = MockRutaUStore()
        val requests = FakeRideRequestRepository(store)
        val coordinations = FakeCoordinationRepository(store)
        val requestId = "request-pending"
        val seatsBefore = store.trips.value.first { it.id == "trip-andrea-driver" }.availableSeats

        requests.accept(requestId)

        assertEquals(RequestStatus.ACCEPTED, requests.observeRequest(requestId).first()?.status)
        assertEquals(
            seatsBefore - 1,
            store.trips.value.first { it.id == "trip-andrea-driver" }.availableSeats,
        )
        val coordination = coordinations.observeCoordination(requestId).first()
        assertEquals("trip-andrea-driver", coordination?.driverTripId)
        assertEquals("trip-mateo-passenger", coordination?.passengerTripId)
    }

    @Test
    fun `reject cancel and expire leave driver capacity unchanged`() = runBlocking {
        val store = MockRutaUStore()
        val requests = FakeRideRequestRepository(store)
        val creator = FakeCoordinationRepository(store)
        val seatsBefore = store.trips.value.first { it.id == "trip-andrea-driver" }.availableSeats

        val rejectedId = creator.createPendingCoordination(
            "trip-mateo-passenger",
            "trip-andrea-driver",
            RequestType.JOIN_REQUEST,
            null,
            1_000,
        )
        val cancelledId = creator.createPendingCoordination(
            "trip-mateo-passenger",
            "trip-andrea-driver",
            RequestType.JOIN_REQUEST,
            null,
            1_000,
        )
        val expiredId = creator.createPendingCoordination(
            "trip-mateo-passenger",
            "trip-andrea-driver",
            RequestType.JOIN_REQUEST,
            null,
            1_000,
        )

        requests.reject(rejectedId)
        requests.cancel(cancelledId)
        requests.expire(expiredId)

        assertEquals(RequestStatus.REJECTED, requests.observeRequest(rejectedId).first()?.status)
        assertEquals(RequestStatus.CANCELLED, requests.observeRequest(cancelledId).first()?.status)
        assertEquals(RequestStatus.EXPIRED, requests.observeRequest(expiredId).first()?.status)
        assertEquals(seatsBefore, store.trips.value.first { it.id == "trip-andrea-driver" }.availableSeats)
    }

    @Test
    fun `cancelling an accepted participation releases the occupied seat`() = runBlocking {
        val store = MockRutaUStore()
        val requests = FakeRideRequestRepository(store)
        val coordinations = FakeCoordinationRepository(store)
        val requestId = "request-pending"
        val seatsBefore = store.trips.value.first { it.id == "trip-andrea-driver" }.availableSeats

        requests.accept(requestId)
        val coordination = coordinations.observeCoordination(requestId).first()!!
        coordinations.cancelParticipation(coordination.id)

        assertEquals(RequestStatus.CANCELLED, requests.observeRequest(requestId).first()?.status)
        assertEquals(seatsBefore, store.trips.value.first { it.id == "trip-andrea-driver" }.availableSeats)
        assertNull(coordinations.observeCoordination(requestId).first())
    }

    @Test
    fun `cancelling a driver ride frees every confirmed seat for that ride`() = runBlocking {
        val store = MockRutaUStore()
        val coordinations = FakeCoordinationRepository(store)

        coordinations.cancelRide("coord-mateo-sofia")

        val driver = store.trips.value.first { it.id == "trip-mateo-driver" }
        assertEquals(0, driver.occupiedSeats)
        assertEquals(RequestStatus.CANCELLED, store.requests.value.first { it.id == "request-accepted-sofia" }.status)
        assertEquals(RequestStatus.CANCELLED, store.requests.value.first { it.id == "request-accepted-carlos" }.status)
        assertFalse(store.coordinations.value.any { it.driverTripId == "trip-mateo-driver" })
    }

    @Test
    fun `full ride keeps three confirmed passengers and rejects new acceptances`() = runBlocking {
        val store = MockRutaUStore()
        val requests = FakeRideRequestRepository(store)
        val creator = FakeCoordinationRepository(store)
        val fullDriver = store.trips.value.first { it.id == "trip-andrea-driver-full" }

        assertEquals(3, fullDriver.occupiedSeats)
        assertEquals(0, fullDriver.availableSeats)
        assertEquals(3, store.coordinations.value.count { it.driverTripId == fullDriver.id })

        val requestId = creator.createPendingCoordination(
            "trip-lucia-passenger",
            fullDriver.id,
            RequestType.JOIN_REQUEST,
            null,
            1_000,
        )
        val failure = runCatching { requests.accept(requestId) }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        assertEquals(RequestStatus.PENDING, requests.observeRequest(requestId).first()?.status)
    }

    @Test
    fun `accepting a request without available seats is rejected`() = runBlocking {
        val store = MockRutaUStore()
        val requests = FakeRideRequestRepository(store)
        val creator = FakeCoordinationRepository(store)
        val firstRequestId = creator.createPendingCoordination(
            "trip-mateo-driver",
            "trip-sofia-passenger",
            RequestType.DRIVER_INVITATION,
            null,
            1_000,
        )
        val secondRequestId = creator.createPendingCoordination(
            "trip-mateo-driver",
            "trip-carlos-passenger",
            RequestType.DRIVER_INVITATION,
            null,
            1_000,
        )

        requests.accept(firstRequestId)
        val failure = try {
            requests.accept(secondRequestId)
            null
        } catch (error: Throwable) {
            error
        }

        assertTrue(failure is IllegalStateException)
        assertEquals(RequestStatus.PENDING, requests.observeRequest(secondRequestId).first()?.status)
    }
}

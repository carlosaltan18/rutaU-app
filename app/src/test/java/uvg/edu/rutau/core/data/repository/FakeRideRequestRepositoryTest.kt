package uvg.edu.rutau.core.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uvg.edu.rutau.core.data.mock.MockRutaUStore
import uvg.edu.rutau.core.model.RequestStatus
import uvg.edu.rutau.core.model.RequestType

/** Comprueba los cambios de estado de una solicitud. */
class FakeRideRequestRepositoryTest {
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

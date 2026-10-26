package uvg.edu.rutau.core.network

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** Comprueba que los nombres enviados coincidan con el contrato OpenAPI. */
class BackendNetworkModelsTest {
    @Test
    fun serializesTheDeviceRegistrationFields() {
        val json = Gson().toJson(RegisterDeviceRequestDto(token = "fcm-token", platform = "ANDROID"))

        assertEquals("fcm-token", Gson().fromJson(json, Map::class.java)["token"])
        assertEquals("ANDROID", Gson().fromJson(json, Map::class.java)["platform"])
    }

    @Test
    fun serializesTheRideRequestMessageWithTheOpenApiName() {
        val json = Gson().toJson(
            CreateRideRequestRequestDto(
                senderTripId = "trip-a",
                targetTripId = "trip-b",
                type = "JOIN_REQUEST",
                travelDate = "2026-10-24",
                offeredContributionMinor = 1000,
                currency = "GTQ",
                message = "Hola",
            ),
        )

        assertEquals("Hola", Gson().fromJson(json, Map::class.java)["message"])
    }

    @Test
    fun keepsTheSameIdempotencyKeyUntilTheOperationCompletes() {
        val keys = IdempotencyKeyStore()

        val first = keys.keyFor("trip:one")
        val retry = keys.keyFor("trip:one")
        keys.complete("trip:one")
        val newOperation = keys.keyFor("trip:one")

        assertEquals(first, retry)
        assertNotEquals(first, newOperation)
    }

    @Test
    fun mapsTheBackendTripFieldsWithoutChangingTheRole() {
        val trip = BackendTripDto(
            id = "trip-1",
            originZoneId = "zone-1",
            destinationCampusId = "campus-1",
            dayOfWeek = 1,
            departureTime = "06:30",
            role = "DRIVER",
            seatCapacity = 3,
            status = "ACTIVE",
            version = 2,
        ).toTrip()

        assertEquals("trip-1", trip.id)
        assertEquals(3, trip.offeredSeats)
        assertEquals("Lunes", trip.dayOfWeek)
    }
}

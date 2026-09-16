package uvg.edu.rutau.core.model

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class TripTest {
    @Test
    fun `available seats subtract occupied seats from offered seats`() {
        val trip = Trip(
            id = "trip-driver",
            ownerId = "user-driver",
            originZone = "Zona 11",
            destinationCampus = "Campus Central",
            dayOfWeek = "Lunes",
            departureTime = LocalTime.of(6, 30),
            role = TripRole.DRIVER,
            offeredSeats = 3,
            occupiedSeats = 2,
            active = true,
        )

        assertEquals(1, trip.availableSeats)
    }
}

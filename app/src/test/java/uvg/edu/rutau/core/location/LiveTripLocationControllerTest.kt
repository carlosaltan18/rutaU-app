package uvg.edu.rutau.core.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Comprueba que la ubicación se limite a viajes confirmados. */
class LiveTripLocationControllerTest {
    @Test
    fun startsOnlyForAConfirmedTripWithPermission() {
        val service = FakeLocationSharingService()
        val controller = LiveTripLocationController(service)

        val didStart = controller.start(LocationSharingSession("occurrence-1", true, true))

        assertTrue(didStart)
        assertEquals("occurrence-1", service.startedOccurrenceId)
    }

    @Test
    fun doesNotStartWithoutConfirmationOrPermission() {
        val service = FakeLocationSharingService()
        val controller = LiveTripLocationController(service)

        val didStart = controller.start(LocationSharingSession("occurrence-1", false, true))

        assertFalse(didStart)
        assertEquals(null, service.startedOccurrenceId)
    }

    @Test
    fun stopsTheSharingService() {
        val service = FakeLocationSharingService()
        val controller = LiveTripLocationController(service)

        controller.stop()

        assertTrue(service.wasStopped)
    }
}

/** Guarda las llamadas al servicio para las pruebas. */
private class FakeLocationSharingService : LocationSharingService {
    var startedOccurrenceId: String? = null
    var wasStopped = false

    override fun start(tripOccurrenceId: String) {
        startedOccurrenceId = tripOccurrenceId
    }

    override fun stop() {
        wasStopped = true
    }
}

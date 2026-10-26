package uvg.edu.rutau.core.notifications

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Comprueba que un push use identificadores para abrir el recurso correcto. */
@RunWith(AndroidJUnit4::class)
class PushNavigationTest {
    @Test
    fun readsTheRequestTargetFromTheIntent() {
        val target = PushNavigationTarget(
            type = RutaUPushType.REQUEST_RECEIVED,
            destination = "RIDE_REQUEST_DETAIL",
            resourceId = "request-1",
        )

        val restored = Intent().putPushNavigationTarget(target).toPushNavigationTarget()

        assertEquals(target, restored)
    }
}

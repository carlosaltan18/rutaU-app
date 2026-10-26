package uvg.edu.rutau.core.preferences

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Comprueba que DataStore solo recuerde mensajes y preferencias locales. */
@RunWith(AndroidJUnit4::class)
class PopupMessageRepositoryTest {
    @Test
    fun remembersSeenMessagesAndThePopupPreference() = runBlocking {
        val repository = DataStorePopupMessageRepository(ApplicationProvider.getApplicationContext())
        val messageId = "popup-test-${System.nanoTime()}"

        repository.markAsSeen(messageId)
        repository.setPopupsEnabled(false)
        val state = repository.observeState().first()

        assertTrue(messageId in state.seenMessageIds)
        assertTrue(state.lastViewedAtMillis != null)
        assertFalse(state.popupsEnabled)
    }
}

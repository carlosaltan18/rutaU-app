package uvg.edu.rutau.core.data.repository

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uvg.edu.rutau.core.data.mock.MockRutaUStore
import uvg.edu.rutau.core.model.SignUpInput

/** Comprueba las acciones de inicio y cierre de sesión. */
class FakeSessionRepositoryTest {
    @Test
    fun `login signs in with the seeded credentials`() = runBlocking {
        val store = MockRutaUStore()
        val repository = FakeSessionRepository(store)

        val didLogin = repository.login("mateo@ejemplo.com", "RutaU123")

        assertTrue(didLogin)
        assertEquals(SessionState.SignedIn("user-mateo"), repository.sessionState.value)
    }

    @Test
    fun `login rejects an invalid password`() = runBlocking {
        val repository = FakeSessionRepository(MockRutaUStore())

        val didLogin = repository.login("mateo@ejemplo.com", "incorrect-password")

        assertFalse(didLogin)
        assertEquals(SessionState.SignedOut, repository.sessionState.value)
    }

    @Test
    fun `registration creates a session and updates the shared user`() = runBlocking {
        val store = MockRutaUStore()
        val repository = FakeSessionRepository(store)

        val didRegister = repository.register(
            SignUpInput(
                fullName = "Ana García",
                university = "Universidad San Carlos",
                campus = "Campus Central",
                email = "ana@ejemplo.com",
                password = "RutaU123",
            ),
        )

        assertTrue(didRegister)
        assertEquals("ana@ejemplo.com", store.currentUser.value?.email)
        assertTrue(repository.sessionState.value is SessionState.SignedIn)
    }
}

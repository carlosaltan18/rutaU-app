package uvg.edu.rutau.core.data.repository

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uvg.edu.rutau.core.data.mock.MockRutaUStore
import uvg.edu.rutau.core.model.UpdateEmailInput
import uvg.edu.rutau.core.model.UpdatePasswordInput
import uvg.edu.rutau.core.model.UpdateProfileInput

/** Comprueba los cambios que se pueden hacer en una cuenta. */
class FakeUserRepositoryTest {
    @Test
    fun `profile updates are reflected in the shared store`() = runBlocking {
        val store = MockRutaUStore()
        val repository = FakeUserRepository(store)

        repository.updateProfile(
            UpdateProfileInput(
                fullName = "Mateo M. Silva",
                university = "Universidad San Carlos",
                campus = "Campus Sur",
                photoUrl = null,
            ),
        )

        assertEquals("Mateo M. Silva", store.currentUser.value?.fullName)
        assertEquals("Campus Sur", store.currentUser.value?.campus)
    }

    @Test
    fun `email update requires the current password`() = runBlocking {
        val store = MockRutaUStore()
        val repository = FakeUserRepository(store)

        val didReject = repository.updateEmail(
            UpdateEmailInput("nuevo@ejemplo.com", "invalid-password"),
        )
        val didUpdate = repository.updateEmail(
            UpdateEmailInput("nuevo@ejemplo.com", "RutaU123"),
        )

        assertFalse(didReject)
        assertTrue(didUpdate)
        assertEquals("nuevo@ejemplo.com", store.currentUser.value?.email)
    }

    @Test
    fun `password update validates the current password`() = runBlocking {
        val store = MockRutaUStore()
        val repository = FakeUserRepository(store)

        val didUpdate = repository.updatePassword(
            UpdatePasswordInput("RutaU123", "NuevaRutaU123"),
        )

        assertTrue(didUpdate)
        assertTrue(store.passwordMatches("NuevaRutaU123"))
    }

    @Test
    fun `password and email updates reject invalid new values`() = runBlocking {
        val store = MockRutaUStore()
        val repository = FakeUserRepository(store)

        val passwordUpdated = repository.updatePassword(
            UpdatePasswordInput("RutaU123", "solo1234"),
        )
        val emailUpdated = repository.updateEmail(
            UpdateEmailInput("correo-invalido", "RutaU123"),
        )

        assertFalse(passwordUpdated)
        assertFalse(emailUpdated)
        assertTrue(store.passwordMatches("RutaU123"))
    }

    @Test
    fun `deleting the account also clears coordinated rides`() = runBlocking {
        val store = MockRutaUStore()
        val repository = FakeUserRepository(store)

        repository.deleteAccount()

        assertTrue(store.coordinations.value.isEmpty())
        assertTrue(store.requests.value.isEmpty())
        assertTrue(store.trips.value.isEmpty())
    }
}

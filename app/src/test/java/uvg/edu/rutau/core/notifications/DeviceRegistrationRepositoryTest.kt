package uvg.edu.rutau.core.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uvg.edu.rutau.core.data.repository.SessionRepository
import uvg.edu.rutau.core.data.repository.SessionState
import uvg.edu.rutau.core.model.SignUpInput
import uvg.edu.rutau.core.network.RegisterDeviceRequestDto

/** Comprueba el registro y la renovación del token de notificaciones. */
class DeviceRegistrationRepositoryTest {
    @Test
    fun registersTheCurrentAndRenewedTokensForASignedInPerson() = runBlocking {
        val registrations = mutableListOf<RegisterDeviceRequestDto>()
        val repository = BackendDeviceRegistrationRepository(
            tokenProvider = FakeFcmTokenProvider("first-token"),
            sessionRepository = TestSessionRepository(SessionState.SignedIn("firebase-user-1")),
            store = MemoryDeviceRegistrationStore(),
            registerDeviceCall = { idempotencyKey, body ->
                assertTrue(idempotencyKey.isNotBlank())
                registrations += body
                "device-${registrations.size}"
            },
            deleteDeviceCall = {},
        )

        repository.registerCurrentDevice()
        repository.registerToken("renewed-token")

        assertEquals(listOf("first-token", "renewed-token"), registrations.map(RegisterDeviceRequestDto::token))
        assertTrue(registrations.all { it.platform == "ANDROID" })
    }

    @Test
    fun ignoresTokensWhenThereIsNoSession() = runBlocking {
        var registrationCount = 0
        val repository = BackendDeviceRegistrationRepository(
            tokenProvider = FakeFcmTokenProvider("token"),
            sessionRepository = TestSessionRepository(SessionState.SignedOut),
            store = MemoryDeviceRegistrationStore(),
            registerDeviceCall = { _, _ -> registrationCount += 1; "device" },
            deleteDeviceCall = {},
        )

        repository.registerCurrentDevice()

        assertEquals(0, registrationCount)
    }

    @Test
    fun removesTheSavedDeviceWhenTheSessionCloses() = runBlocking {
        val store = MemoryDeviceRegistrationStore()
        var removedDeviceId: String? = null
        val repository = BackendDeviceRegistrationRepository(
            tokenProvider = FakeFcmTokenProvider("token"),
            sessionRepository = TestSessionRepository(SessionState.SignedIn("firebase-user-1")),
            store = store,
            registerDeviceCall = { _, _ -> "device-123" },
            deleteDeviceCall = { removedDeviceId = it },
        )

        repository.registerCurrentDevice()
        repository.unregisterCurrentDevice()

        assertEquals("device-123", removedDeviceId)
        assertEquals(null, store.deviceId())
    }
}

/** Entrega un token controlado para las pruebas. */
private class FakeFcmTokenProvider(
    private val token: String,
) : FcmTokenProvider {
    override suspend fun getToken(): String = token
}

/** Mantiene el estado de sesión mínimo que necesitan estas pruebas. */
private class TestSessionRepository(
    initialState: SessionState,
) : SessionRepository {
    private val mutableState = MutableStateFlow(initialState)
    override val sessionState: StateFlow<SessionState> = mutableState

    override suspend fun login(email: String, password: String): Boolean = false
    override suspend fun loginWithGoogle(idToken: String): Boolean = false
    override suspend fun register(input: SignUpInput): Boolean = false
    override suspend fun registerWithGoogle(idToken: String, input: SignUpInput): Boolean = false
    override suspend fun requestPasswordRecovery(email: String) = Unit
    override suspend fun resetPassword(newPassword: String) = Unit
    override suspend fun logout() {
        mutableState.value = SessionState.SignedOut
    }
}

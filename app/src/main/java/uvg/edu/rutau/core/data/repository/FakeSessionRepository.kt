package uvg.edu.rutau.core.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import uvg.edu.rutau.core.data.mock.MockRutaUStore
import uvg.edu.rutau.core.model.SignUpInput
import uvg.edu.rutau.core.model.UserAccount

/** Maneja la sesión de ejemplo mientras la aplicación está abierta. */
class FakeSessionRepository(
    private val store: MockRutaUStore,
) : SessionRepository {
    private val mutableSessionState = MutableStateFlow<SessionState>(SessionState.SignedOut)
    override val sessionState: StateFlow<SessionState> = mutableSessionState

    override suspend fun login(email: String, password: String): Boolean {
        if (!store.credentialsMatch(email, password)) return false
        val userId = store.currentUser.value?.id ?: return false
        mutableSessionState.value = SessionState.SignedIn(userId)
        return true
    }

    override suspend fun register(input: SignUpInput): Boolean {
        if (input.email.isBlank() || input.password.isBlank()) return false
        val account = UserAccount(
            id = "user-${input.email.trim().lowercase().hashCode()}",
            fullName = input.fullName.trim(),
            university = input.university.trim(),
            campus = input.campus.trim(),
            email = input.email.trim(),
            photoUrl = input.photoUrl,
        )
        store.currentUser.value = account
        store.updatePassword(input.password)
        mutableSessionState.value = SessionState.SignedIn(account.id)
        return true
    }

    override suspend fun requestPasswordRecovery(email: String) = Unit

    override suspend fun resetPassword(newPassword: String) {
        store.updatePassword(newPassword)
        mutableSessionState.value = SessionState.SignedOut
    }

    override suspend fun logout() {
        mutableSessionState.value = SessionState.SignedOut
    }
}

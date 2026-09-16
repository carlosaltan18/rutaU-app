package uvg.edu.rutau.core.data.repository

import kotlinx.coroutines.flow.StateFlow
import uvg.edu.rutau.core.model.SignUpInput

sealed interface SessionState {
    data object SignedOut : SessionState
    data class SignedIn(val userId: String) : SessionState
}

interface SessionRepository {
    val sessionState: StateFlow<SessionState>

    suspend fun login(email: String, password: String): Boolean
    suspend fun register(input: SignUpInput): Boolean
    suspend fun requestPasswordRecovery(email: String)
    suspend fun resetPassword(newPassword: String)
    suspend fun logout()
}

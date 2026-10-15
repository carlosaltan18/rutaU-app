package uvg.edu.rutau.core.data.repository

import kotlinx.coroutines.flow.StateFlow
import uvg.edu.rutau.core.model.SignUpInput

/** Indica si una persona tiene una sesión iniciada. */
sealed interface SessionState {
    /** Indica que no hay una sesión iniciada. */
    data object SignedOut : SessionState
    /** Guarda la persona que tiene la sesión iniciada. */
    data class SignedIn(val userId: String) : SessionState
}

/** Indica las acciones disponibles para iniciar o cerrar sesión. */
interface SessionRepository {
    val sessionState: StateFlow<SessionState>

    suspend fun login(email: String, password: String): Boolean
    suspend fun register(input: SignUpInput): Boolean
    suspend fun requestPasswordRecovery(email: String)
    suspend fun resetPassword(newPassword: String)
    suspend fun logout()
}

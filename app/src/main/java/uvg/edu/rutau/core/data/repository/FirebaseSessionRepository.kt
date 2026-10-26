package uvg.edu.rutau.core.data.repository

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import uvg.edu.rutau.core.model.PasswordRules
import uvg.edu.rutau.core.model.SignUpInput
import uvg.edu.rutau.core.network.BackendApi
import uvg.edu.rutau.core.network.BackendApiException
import uvg.edu.rutau.core.network.BackendNetworkException
import uvg.edu.rutau.core.network.SessionRequestDto
import uvg.edu.rutau.core.network.requireData

/** Maneja la sesión de Firebase y la enlaza con la sesión del servidor. */
class FirebaseSessionRepository(
    context: Context,
    private val firebaseAuth: FirebaseAuth,
    private val backendApi: BackendApi,
    private val onBackendSessionReady: () -> Unit,
    private val onBeforeLogout: suspend () -> Unit = {},
) : SessionRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val credentialManager = CredentialManager.create(context.applicationContext)
    private val mutableSessionState = MutableStateFlow(firebaseUidToSessionState(firebaseAuth.currentUser?.uid))
    override val sessionState: StateFlow<SessionState> = mutableSessionState

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        mutableSessionState.value = firebaseUidToSessionState(auth.currentUser?.uid)
    }

    init {
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    override suspend fun login(email: String, password: String): Boolean = runCatching {
        firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await()
        createBackendSession(SessionRequestDto())
    }.getOrElse { error ->
        signOutFirebase()
        // Re-lanzar errores de red para que el ViewModel pueda mostrar mensaje apropiado
        if (error is java.io.IOException) throw error
        false
    }

    override suspend fun loginWithGoogle(idToken: String): Boolean = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = firebaseAuth.signInWithCredential(credential).await()
        val user = authResult.user ?: firebaseAuth.currentUser
        createBackendSession(
            SessionRequestDto(
                displayName = user?.displayName?.trim()?.takeIf { it.isNotBlank() },
                institutionalEmail = user?.email?.trim()?.takeIf { it.isNotBlank() },
                photoUrl = user?.photoUrl?.toString()?.trim()?.takeIf { it.isNotBlank() },
            ),
        )
    }.getOrElse { error ->
        signOutFirebase()
        // Re-lanzar errores de red para que el ViewModel pueda mostrar mensaje apropiado
        if (error is java.io.IOException) throw error
        false
    }

    override suspend fun register(input: SignUpInput): Boolean {
        if (input.email.isBlank() || PasswordRules.validationError(input.password) != null) return false
        return runCatching {
            firebaseAuth.createUserWithEmailAndPassword(input.email.trim(), input.password).await()
            firebaseAuth.currentUser?.updateProfile(
                UserProfileChangeRequest.Builder().setDisplayName(input.fullName.trim()).build(),
            )?.await()
            createBackendSession(
                SessionRequestDto(
                    displayName = input.fullName.trim(),
                    institutionalEmail = input.email.trim(),
                    universityId = input.universityId,
                    campusId = input.campusId,
                    photoUrl = input.photoUrl?.trim()?.takeIf { it.isNotBlank() },
                ),
            )
        }.getOrElse { error ->
            runCatching { firebaseAuth.currentUser?.delete()?.await() }
            signOutFirebase()
            // Re-lanzar errores de red y errores de API para que el ViewModel muestre mensajes descriptivos
            if (error is java.io.IOException) throw error
            false
        }
    }

    override suspend fun registerWithGoogle(idToken: String, input: SignUpInput): Boolean {
        if (idToken.isBlank()) return false
        return runCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user ?: firebaseAuth.currentUser
            val displayName = input.fullName.trim().ifBlank { user?.displayName.orEmpty() }
            if (displayName.isNotBlank()) {
                user?.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(displayName).build(),
                )?.await()
            }
            createBackendSession(
                SessionRequestDto(
                    displayName = displayName.takeIf { it.isNotBlank() },
                    institutionalEmail = (input.email.trim().ifBlank { user?.email.orEmpty() }).takeIf { it.isNotBlank() },
                    universityId = input.universityId.trim().takeIf { it.isNotBlank() },
                    campusId = input.campusId.trim().takeIf { it.isNotBlank() },
                    photoUrl = (input.photoUrl?.trim()?.takeIf { it.isNotBlank() } ?: user?.photoUrl?.toString()),
                ),
            )
        }.getOrElse { error ->
            signOutFirebase()
            // Re-lanzar errores de red y errores de API para que el ViewModel muestre mensajes descriptivos
            if (error is java.io.IOException) throw error
            false
        }
    }

    override suspend fun requestPasswordRecovery(email: String) {
        runCatching { firebaseAuth.sendPasswordResetEmail(email.trim()).await() }
    }

    override suspend fun resetPassword(newPassword: String) {
        if (PasswordRules.validationError(newPassword) != null) return
        firebaseAuth.currentUser?.updatePassword(newPassword)?.await()
        logout()
    }

    override suspend fun logout() {
        runCatching { onBeforeLogout() }
        signOutFirebase()
        runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
    }

    /** Cierra la sesión si el servidor rechaza un token renovado. */
    fun onBackendSessionInvalid() {
        scope.launch { logout() }
    }

    /** Recupera la sesión del servidor cuando Firebase conserva una sesión previa. */
    suspend fun restoreBackendSession() {
        val user = firebaseAuth.currentUser ?: return
        runCatching {
            createBackendSession(
                SessionRequestDto(
                    displayName = user.displayName?.trim()?.takeIf { it.isNotBlank() },
                    institutionalEmail = user.email?.trim()?.takeIf { it.isNotBlank() },
                    photoUrl = user.photoUrl?.toString()?.trim()?.takeIf { it.isNotBlank() },
                ),
            )
        }.onFailure { error ->
            // Solo cerrar sesión si el servidor rechaza explícitamente el token (auth inválida)
            // No cerrar sesión por errores de red (cold start de Render, sin internet, etc.)
            if (error is BackendApiException && (error.code == "UNAUTHENTICATED" || error.code == "FORBIDDEN")) {
                signOutFirebase()
            }
            // Los errores de red (BackendNetworkException, IOException) se ignoran:
            // el usuario queda con sesión Firebase válida y el backend sincronizará en la siguiente llamada
        }
    }

    private suspend fun createBackendSession(body: SessionRequestDto): Boolean {
        check(backendApi.createSession(body).requireData().authenticated) {
            "El servidor no confirmó la sesión."
        }
        onBackendSessionReady()
        return true
    }

    private fun signOutFirebase() {
        firebaseAuth.signOut()
        mutableSessionState.value = SessionState.SignedOut
    }
}

/** Convierte el identificador de Firebase al estado que usa la aplicación. */
internal fun firebaseUidToSessionState(firebaseUid: String?): SessionState =
    firebaseUid?.takeIf(String::isNotBlank)?.let(SessionState::SignedIn) ?: SessionState.SignedOut

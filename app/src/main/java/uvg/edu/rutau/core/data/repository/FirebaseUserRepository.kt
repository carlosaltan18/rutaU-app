package uvg.edu.rutau.core.data.repository

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import uvg.edu.rutau.core.model.UpdateEmailInput
import uvg.edu.rutau.core.model.UpdatePasswordInput
import uvg.edu.rutau.core.model.UpdateProfileInput
import uvg.edu.rutau.core.model.UserAccount
import uvg.edu.rutau.core.network.BackendApi
import uvg.edu.rutau.core.network.UpdateMeRequestDto
import uvg.edu.rutau.core.network.requireData
import uvg.edu.rutau.core.network.toUserAccount

/** Consulta y cambia la cuenta usando Firebase y el perfil del servidor. */
class FirebaseUserRepository(
    private val firebaseAuth: FirebaseAuth,
    private val sessionRepository: SessionRepository,
    private val backendApi: BackendApi,
) : UserRepository {
    override fun observeCurrentUser(): Flow<UserAccount?> = sessionRepository.sessionState.map { state ->
        if (state !is SessionState.SignedIn) return@map null
        backendApi.getMe().requireData().toUserAccount()
    }

    override suspend fun updateProfile(input: UpdateProfileInput) {
        backendApi.updateMe(
            UpdateMeRequestDto(
                displayName = input.fullName,
                photoUrl = input.photoUrl,
                campusId = input.campus,
            ),
        ).requireData()
        firebaseAuth.currentUser?.updateProfile(
            UserProfileChangeRequest.Builder().setDisplayName(input.fullName).build(),
        )?.await()
    }

    override suspend fun updateEmail(input: UpdateEmailInput): Boolean = runCatching {
        val user = firebaseAuth.currentUser ?: return false
        user.reauthenticate(EmailAuthProvider.getCredential(user.email.orEmpty(), input.currentPassword)).await()
        user.verifyBeforeUpdateEmail(input.email.trim()).await()
        true
    }.getOrDefault(false)

    override suspend fun updatePassword(input: UpdatePasswordInput): Boolean = runCatching {
        val user = firebaseAuth.currentUser ?: return false
        user.reauthenticate(EmailAuthProvider.getCredential(user.email.orEmpty(), input.currentPassword)).await()
        user.updatePassword(input.newPassword).await()
        true
    }.getOrDefault(false)

    override suspend fun deleteAccount() {
        backendApi.deleteMe().requireData()
    }
}

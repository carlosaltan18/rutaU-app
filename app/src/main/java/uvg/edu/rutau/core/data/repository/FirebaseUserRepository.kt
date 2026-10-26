package uvg.edu.rutau.core.data.repository

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import uvg.edu.rutau.core.data.catalog.CatalogRepository
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
    private val catalogRepository: CatalogRepository,
) : UserRepository {
    override fun observeCurrentUser(): Flow<UserAccount?> = sessionRepository.sessionState.map { state ->
        if (state !is SessionState.SignedIn) return@map null
        val profile = backendApi.getMe().requireData()
        val universityName = runCatching {
            catalogRepository.universities().firstOrNull { it.id == profile.universityId }?.name
        }.getOrNull() ?: profile.universityId
        val campusName = runCatching {
            catalogRepository.campuses(profile.universityId).firstOrNull { it.id == profile.campusId }?.name
        }.getOrNull() ?: profile.campusId
        profile.toUserAccount(universityName, campusName)
    }

    override suspend fun updateProfile(input: UpdateProfileInput) {
        backendApi.updateMe(
            UpdateMeRequestDto(
                displayName = input.fullName,
                photoUrl = input.photoUrl,
                universityId = input.universityId,
                campusId = input.campusId,
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

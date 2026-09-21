package uvg.edu.rutau.core.data.repository

import kotlinx.coroutines.flow.Flow
import uvg.edu.rutau.core.data.mock.MockRutaUStore
import uvg.edu.rutau.core.model.UpdateEmailInput
import uvg.edu.rutau.core.model.UpdatePasswordInput
import uvg.edu.rutau.core.model.UpdateProfileInput
import uvg.edu.rutau.core.model.UserAccount
import uvg.edu.rutau.core.model.PasswordRules

/** Maneja la cuenta de ejemplo mientras la aplicación está abierta. */
class FakeUserRepository(
    private val store: MockRutaUStore,
) : UserRepository {
    override fun observeCurrentUser(): Flow<UserAccount?> = store.currentUser

    override suspend fun updateProfile(input: UpdateProfileInput) {
        if (input.fullName.isBlank() || input.university.isBlank() || input.campus.isBlank()) return
        val currentUser = store.currentUser.value ?: return
        store.currentUser.value = currentUser.copy(
            fullName = input.fullName.trim(),
            university = input.university.trim(),
            campus = input.campus.trim(),
            photoUrl = input.photoUrl,
        )
    }

    override suspend fun updateEmail(input: UpdateEmailInput): Boolean {
        if (input.email.isBlank() || !input.email.contains('@')) return false
        if (!store.passwordMatches(input.currentPassword)) return false
        val currentUser = store.currentUser.value ?: return false
        store.currentUser.value = currentUser.copy(email = input.email.trim())
        return true
    }

    override suspend fun updatePassword(input: UpdatePasswordInput): Boolean {
        if (PasswordRules.validationError(input.newPassword) != null) return false
        if (!store.passwordMatches(input.currentPassword)) return false
        store.updatePassword(input.newPassword)
        return true
    }

    override suspend fun deleteAccount() {
        store.currentUser.value = null
        store.trips.value = emptyList()
        store.requests.value = emptyList()
        store.coordinations.value = emptyList()
    }
}

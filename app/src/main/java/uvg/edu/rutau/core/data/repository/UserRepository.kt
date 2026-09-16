package uvg.edu.rutau.core.data.repository

import kotlinx.coroutines.flow.Flow
import uvg.edu.rutau.core.model.UpdateEmailInput
import uvg.edu.rutau.core.model.UpdatePasswordInput
import uvg.edu.rutau.core.model.UpdateProfileInput
import uvg.edu.rutau.core.model.UserAccount

interface UserRepository {
    fun observeCurrentUser(): Flow<UserAccount?>

    suspend fun updateProfile(input: UpdateProfileInput)
    suspend fun updateEmail(input: UpdateEmailInput): Boolean
    suspend fun updatePassword(input: UpdatePasswordInput): Boolean
    suspend fun deleteAccount()
}

package uvg.edu.rutau.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uvg.edu.rutau.core.data.repository.SessionRepository
import uvg.edu.rutau.core.data.repository.UserRepository
import uvg.edu.rutau.core.model.UpdateEmailInput
import uvg.edu.rutau.core.model.UpdatePasswordInput
import uvg.edu.rutau.core.model.UpdateProfileInput

sealed interface AccountEvent {
    data object SignedOut : AccountEvent
}

class AccountViewModel(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<AccountUiState?>(null)
    val uiState = mutableUiState.asStateFlow()

    private val mutableEvents = MutableSharedFlow<AccountEvent>()
    val events: SharedFlow<AccountEvent> = mutableEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            userRepository.observeCurrentUser().collectLatest { user ->
                mutableUiState.value = user?.let(::AccountUiState)
            }
        }
    }

    fun onAction(action: AccountAction) {
        when (action) {
            is AccountAction.FullNameChanged -> updateState { copy(fullName = action.value) }
            is AccountAction.UniversityChanged -> updateState { copy(university = action.value) }
            is AccountAction.CampusChanged -> updateState { copy(campus = action.value) }
            is AccountAction.EmailChanged -> updateState { copy(email = action.value) }
            is AccountAction.CurrentEmailPasswordChanged -> updateState { copy(currentEmailPassword = action.value) }
            is AccountAction.CurrentPasswordChanged -> updateState { copy(currentPassword = action.value) }
            is AccountAction.NewPasswordChanged -> updateState { copy(newPassword = action.value) }
            is AccountAction.ConfirmPasswordChanged -> updateState { copy(confirmPassword = action.value) }
            is AccountAction.NotificationsChanged -> updateState { copy(notificationsEnabled = action.enabled) }
            AccountAction.SaveProfile -> saveProfile()
            AccountAction.UpdateEmail -> updateEmail()
            AccountAction.UpdatePassword -> updatePassword()
            AccountAction.Logout -> logout()
            AccountAction.DeleteAccountRequested -> updateState { copy(showDeleteConfirmation = true) }
            AccountAction.DeleteAccountDismissed -> updateState { copy(showDeleteConfirmation = false) }
            AccountAction.DeleteAccountConfirmed -> deleteAccount()
        }
    }

    private fun saveProfile() {
        val state = uiState.value ?: return
        viewModelScope.launch {
            updateState { copy(isSaving = true, message = null) }
            userRepository.updateProfile(
                UpdateProfileInput(state.fullName, state.university, state.campus, state.user.photoUrl),
            )
            updateState { copy(isSaving = false, message = "Tus datos fueron actualizados.") }
        }
    }

    private fun updateEmail() {
        val state = uiState.value ?: return
        viewModelScope.launch {
            val didUpdate = userRepository.updateEmail(
                UpdateEmailInput(state.email, state.currentEmailPassword),
            )
            updateState {
                copy(
                    currentEmailPassword = "",
                    message = if (didUpdate) "Tu correo fue actualizado." else "La contraseña actual no es correcta.",
                )
            }
        }
    }

    private fun updatePassword() {
        val state = uiState.value ?: return
        if (state.newPassword.length < 8 || state.newPassword != state.confirmPassword) {
            updateState { copy(message = "Verifica la nueva contraseña y su confirmación.") }
            return
        }
        viewModelScope.launch {
            val didUpdate = userRepository.updatePassword(
                UpdatePasswordInput(state.currentPassword, state.newPassword),
            )
            updateState {
                copy(
                    currentPassword = "",
                    newPassword = "",
                    confirmPassword = "",
                    message = if (didUpdate) "Tu contraseña fue actualizada." else "La contraseña actual no es correcta.",
                )
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            sessionRepository.logout()
            mutableEvents.emit(AccountEvent.SignedOut)
        }
    }

    private fun deleteAccount() {
        viewModelScope.launch {
            userRepository.deleteAccount()
            sessionRepository.logout()
            mutableEvents.emit(AccountEvent.SignedOut)
        }
    }

    private fun updateState(transform: AccountUiState.() -> AccountUiState) {
        mutableUiState.update { state -> state?.transform() }
    }

    companion object {
        fun factory(
            userRepository: UserRepository,
            sessionRepository: SessionRepository,
        ) = viewModelFactory {
            initializer { AccountViewModel(userRepository, sessionRepository) }
        }
    }
}

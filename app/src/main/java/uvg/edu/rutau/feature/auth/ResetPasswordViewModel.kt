package uvg.edu.rutau.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uvg.edu.rutau.core.data.repository.SessionRepository

/** Guarda lo que se muestra al cambiar una contraseña. */
data class ResetPasswordUiState(
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

/** Indica los resultados posibles al cambiar una contraseña. */
sealed interface ResetPasswordEvent {
    data object PasswordReset : ResetPasswordEvent
}

/** Maneja el cambio de contraseña desde la recuperación. */
class ResetPasswordViewModel(
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ResetPasswordUiState())
    val uiState = mutableUiState.asStateFlow()

    private val mutableEvents = MutableSharedFlow<ResetPasswordEvent>()
    val events: SharedFlow<ResetPasswordEvent> = mutableEvents.asSharedFlow()

    fun onNewPasswordChange(value: String) {
        mutableUiState.update { it.copy(newPassword = value, errorMessage = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        mutableUiState.update { it.copy(confirmPassword = value, errorMessage = null) }
    }

    fun resetPassword() {
        val state = uiState.value
        val validationError = when {
            state.newPassword.length < 8 -> "La contraseña debe tener al menos 8 caracteres."
            state.newPassword != state.confirmPassword -> "Las contraseñas no coinciden."
            else -> null
        }
        if (validationError != null) {
            mutableUiState.update { it.copy(errorMessage = validationError) }
            return
        }
        viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, errorMessage = null) }
            sessionRepository.resetPassword(state.newPassword)
            mutableUiState.update { it.copy(isLoading = false) }
            mutableEvents.emit(ResetPasswordEvent.PasswordReset)
        }
    }

    companion object {
        fun factory(sessionRepository: SessionRepository) = viewModelFactory {
            initializer { ResetPasswordViewModel(sessionRepository) }
        }
    }
}

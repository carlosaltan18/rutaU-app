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

/** Guarda lo que se muestra al iniciar sesión. */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
)

/** Indica los resultados posibles al iniciar sesión. */
sealed interface LoginEvent {
    data object SignedIn : LoginEvent
}

/** Maneja el inicio de sesión. */
class LoginViewModel(
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(LoginUiState())
    val uiState = mutableUiState.asStateFlow()

    private val mutableEvents = MutableSharedFlow<LoginEvent>()
    val events: SharedFlow<LoginEvent> = mutableEvents.asSharedFlow()

    fun onEmailChange(value: String) {
        mutableUiState.update { it.copy(email = value, emailError = null) }
    }

    fun onPasswordChange(value: String) {
        mutableUiState.update { it.copy(password = value, passwordError = null) }
    }

    fun login() {
        val state = uiState.value
        val emailError = when {
            state.email.isBlank() -> "Ingresa tu correo electrónico."
            !state.email.contains('@') -> "Ingresa un correo electrónico válido."
            else -> null
        }
        val passwordError = if (state.password.isBlank()) "Ingresa tu contraseña." else null
        if (emailError != null || passwordError != null) {
            mutableUiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }
        viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, emailError = null, passwordError = null) }
            val didLogin = sessionRepository.login(state.email, state.password)
            mutableUiState.update { it.copy(isLoading = false) }
            if (didLogin) {
                mutableEvents.emit(LoginEvent.SignedIn)
            } else {
                mutableUiState.update {
                    it.copy(passwordError = "El correo o la contraseña no coinciden.")
                }
            }
        }
    }

    companion object {
        fun factory(sessionRepository: SessionRepository) = viewModelFactory {
            initializer { LoginViewModel(sessionRepository) }
        }
    }
}

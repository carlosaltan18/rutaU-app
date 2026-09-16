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

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface LoginEvent {
    data object SignedIn : LoginEvent
}

class LoginViewModel(
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(LoginUiState())
    val uiState = mutableUiState.asStateFlow()

    private val mutableEvents = MutableSharedFlow<LoginEvent>()
    val events: SharedFlow<LoginEvent> = mutableEvents.asSharedFlow()

    fun onEmailChange(value: String) {
        mutableUiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        mutableUiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun login() {
        val state = uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            mutableUiState.update { it.copy(errorMessage = "Ingresa tu correo y contraseña.") }
            return
        }
        viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, errorMessage = null) }
            val didLogin = sessionRepository.login(state.email, state.password)
            mutableUiState.update { it.copy(isLoading = false) }
            if (didLogin) {
                mutableEvents.emit(LoginEvent.SignedIn)
            } else {
                mutableUiState.update { it.copy(errorMessage = "Correo o contraseña incorrectos.") }
            }
        }
    }

    companion object {
        fun factory(sessionRepository: SessionRepository) = viewModelFactory {
            initializer { LoginViewModel(sessionRepository) }
        }
    }
}

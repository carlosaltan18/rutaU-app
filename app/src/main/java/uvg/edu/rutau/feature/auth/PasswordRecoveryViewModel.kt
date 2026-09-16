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

data class PasswordRecoveryUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface PasswordRecoveryEvent {
    data object InstructionsSent : PasswordRecoveryEvent
}

class PasswordRecoveryViewModel(
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(PasswordRecoveryUiState())
    val uiState = mutableUiState.asStateFlow()

    private val mutableEvents = MutableSharedFlow<PasswordRecoveryEvent>()
    val events: SharedFlow<PasswordRecoveryEvent> = mutableEvents.asSharedFlow()

    fun onEmailChange(value: String) {
        mutableUiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun sendInstructions() {
        val email = uiState.value.email
        if (email.isBlank() || !email.contains('@')) {
            mutableUiState.update { it.copy(errorMessage = "Ingresa un correo válido.") }
            return
        }
        viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, errorMessage = null) }
            sessionRepository.requestPasswordRecovery(email)
            mutableUiState.update { it.copy(isLoading = false) }
            mutableEvents.emit(PasswordRecoveryEvent.InstructionsSent)
        }
    }

    companion object {
        fun factory(sessionRepository: SessionRepository) = viewModelFactory {
            initializer { PasswordRecoveryViewModel(sessionRepository) }
        }
    }
}

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
import uvg.edu.rutau.core.model.SignUpInput

data class SignUpUiState(
    val fullName: String = "",
    val university: String = "",
    val campus: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val photoUrl: String? = null,
    val termsAccepted: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface SignUpEvent {
    data object AccountCreated : SignUpEvent
}

class SignUpViewModel(
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SignUpUiState())
    val uiState = mutableUiState.asStateFlow()

    private val mutableEvents = MutableSharedFlow<SignUpEvent>()
    val events: SharedFlow<SignUpEvent> = mutableEvents.asSharedFlow()

    fun onFullNameChange(value: String) = updateState { copy(fullName = value, errorMessage = null) }
    fun onUniversityChange(value: String) = updateState {
        copy(university = value, campus = "", errorMessage = null)
    }
    fun onCampusChange(value: String) = updateState { copy(campus = value, errorMessage = null) }
    fun onPhotoChange(value: String?) = updateState { copy(photoUrl = value, errorMessage = null) }
    fun onEmailChange(value: String) = updateState { copy(email = value, errorMessage = null) }
    fun onPasswordChange(value: String) = updateState { copy(password = value, errorMessage = null) }
    fun onConfirmPasswordChange(value: String) = updateState { copy(confirmPassword = value, errorMessage = null) }
    fun onTermsAcceptedChange(value: Boolean) = updateState { copy(termsAccepted = value, errorMessage = null) }

    fun createAccount() {
        val state = uiState.value
        val validationError = when {
            state.fullName.isBlank() || state.university.isBlank() || state.campus.isBlank() ->
                "Completa tus datos personales y académicos."
            state.email.isBlank() || !state.email.contains('@') -> "Ingresa un correo válido."
            state.password.length < 8 -> "La contraseña debe tener al menos 8 caracteres."
            state.password != state.confirmPassword -> "Las contraseñas no coinciden."
            !state.termsAccepted -> "Debes aceptar los términos y condiciones."
            else -> null
        }
        if (validationError != null) {
            updateState { copy(errorMessage = validationError) }
            return
        }
        viewModelScope.launch {
            updateState { copy(isLoading = true, errorMessage = null) }
            val didRegister = sessionRepository.register(
                SignUpInput(
                    fullName = state.fullName,
                    university = state.university,
                    campus = state.campus,
                    email = state.email,
                    password = state.password,
                    photoUrl = state.photoUrl,
                ),
            )
            updateState { copy(isLoading = false) }
            if (didRegister) {
                mutableEvents.emit(SignUpEvent.AccountCreated)
            } else {
                updateState { copy(errorMessage = "No pudimos crear tu cuenta. Intenta de nuevo.") }
            }
        }
    }

    private fun updateState(transform: SignUpUiState.() -> SignUpUiState) {
        mutableUiState.update(transform)
    }

    companion object {
        fun factory(sessionRepository: SessionRepository) = viewModelFactory {
            initializer { SignUpViewModel(sessionRepository) }
        }
    }
}

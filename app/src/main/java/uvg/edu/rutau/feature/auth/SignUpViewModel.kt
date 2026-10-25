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
import uvg.edu.rutau.core.model.PasswordRules
import uvg.edu.rutau.core.model.SignUpInput

/** Guarda lo que se muestra al crear una cuenta. */
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
    val fullNameError: String? = null,
    val universityError: String? = null,
    val campusError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val termsError: String? = null,
)

/** Indica los resultados posibles al crear una cuenta. */
sealed interface SignUpEvent {
    data object AccountCreated : SignUpEvent
}

/** Maneja la creación de cuentas nuevas. */
class SignUpViewModel(
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SignUpUiState())
    val uiState = mutableUiState.asStateFlow()

    private val mutableEvents = MutableSharedFlow<SignUpEvent>()
    val events: SharedFlow<SignUpEvent> = mutableEvents.asSharedFlow()

    fun onFullNameChange(value: String) = updateState { copy(fullName = value, fullNameError = null) }
    fun onUniversityChange(value: String) = updateState {
        copy(university = value, campus = "", universityError = null, campusError = null)
    }
    fun onCampusChange(value: String) = updateState { copy(campus = value, campusError = null) }
    fun onPhotoChange(value: String?) = updateState { copy(photoUrl = value) }
    fun onEmailChange(value: String) = updateState { copy(email = value, emailError = null) }
    fun onPasswordChange(value: String) = updateState {
        copy(password = value, passwordError = null, confirmPasswordError = null)
    }
    fun onConfirmPasswordChange(value: String) = updateState {
        copy(confirmPassword = value, confirmPasswordError = null)
    }
    fun onTermsAcceptedChange(value: Boolean) = updateState { copy(termsAccepted = value, termsError = null) }

    fun createAccount() {
        val state = uiState.value
        val passwordError = PasswordRules.validationError(state.password)
        val emailError = if (state.email.isBlank() || !state.email.contains('@')) {
            "Ingresa un correo electrónico válido."
        } else {
            null
        }
        val confirmPasswordError = when {
            state.confirmPassword.isBlank() -> "Confirma tu contraseña."
            state.password != state.confirmPassword -> "Las contraseñas no coinciden."
            else -> null
        }
        val fullNameError = if (state.fullName.isBlank()) "Ingresa tu nombre completo." else null
        val universityError = if (state.university.isBlank()) "Selecciona tu universidad." else null
        val campusError = if (state.campus.isBlank()) "Selecciona tu campus o sede habitual." else null
        val termsError = if (!state.termsAccepted) "Debes aceptar los términos y condiciones." else null
        if (
            fullNameError != null || universityError != null || campusError != null ||
            emailError != null || passwordError != null || confirmPasswordError != null || termsError != null
        ) {
            updateState {
                copy(
                    fullNameError = fullNameError,
                    universityError = universityError,
                    campusError = campusError,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError,
                    termsError = termsError,
                )
            }
            return
        }
        viewModelScope.launch {
            updateState {
                copy(
                    isLoading = true,
                    fullNameError = null,
                    universityError = null,
                    campusError = null,
                    emailError = null,
                    passwordError = null,
                    confirmPasswordError = null,
                    termsError = null,
                )
            }
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
                updateState { copy(emailError = "No pudimos validar este correo. Intenta de nuevo.") }
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

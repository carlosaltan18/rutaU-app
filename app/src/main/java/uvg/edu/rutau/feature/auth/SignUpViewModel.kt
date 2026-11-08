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
import uvg.edu.rutau.core.data.catalog.CatalogItem
import uvg.edu.rutau.core.data.catalog.CatalogRepository
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
    val universities: List<CatalogItem> = emptyList(),
    val campuses: List<CatalogItem> = emptyList(),
    val catalogError: String? = null,
    val termsAccepted: Boolean = false,
    val isLoading: Boolean = false,
    val fullNameError: String? = null,
    val universityError: String? = null,
    val campusError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val termsError: String? = null,
    val isFormValid: Boolean = false,
    val isGoogleFormValid: Boolean = false,
)

/** Indica los resultados posibles al crear una cuenta. */
sealed interface SignUpEvent {
    data object AccountCreated : SignUpEvent
}

/** Maneja la creación de cuentas nuevas. */
class SignUpViewModel(
    private val sessionRepository: SessionRepository,
    private val catalogRepository: CatalogRepository? = null,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SignUpUiState())
    val uiState = mutableUiState.asStateFlow()

    private val mutableEvents = MutableSharedFlow<SignUpEvent>()
    val events: SharedFlow<SignUpEvent> = mutableEvents.asSharedFlow()

    init {
        if (catalogRepository != null) loadUniversities()
    }

    fun onFullNameChange(value: String) = updateValidatedState { copy(fullName = value) }
    fun onUniversityChange(value: String) = updateValidatedState {
        copy(university = value, campus = "")
    }.also { loadCampuses(value) }
    fun onCampusChange(value: String) = updateValidatedState { copy(campus = value) }
    fun onPhotoChange(value: String?) = updateState { copy(photoUrl = value) }
    fun onEmailChange(value: String) = updateValidatedState { copy(email = value) }
    fun onPasswordChange(value: String) = updateValidatedState {
        copy(password = value)
    }
    fun onConfirmPasswordChange(value: String) = updateValidatedState {
        copy(confirmPassword = value)
    }
    fun onTermsAcceptedChange(value: Boolean) = updateValidatedState { copy(termsAccepted = value) }

    fun createAccount() {
        val state = uiState.value.withValidation()
        mutableUiState.value = state
        if (!state.isFormValid) {
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
            val result = runCatching {
                sessionRepository.register(
                    SignUpInput(
                        fullName = state.fullName,
                        university = state.university,
                        campus = state.campus,
                        email = state.email,
                        password = state.password,
                        photoUrl = state.photoUrl,
                        universityId = state.university,
                        campusId = state.campus,
                    ),
                )
            }
            updateState { copy(isLoading = false) }
            result.fold(
                onSuccess = { didRegister ->
                    if (didRegister) {
                        mutableEvents.emit(SignUpEvent.AccountCreated)
                    } else {
                        updateState { copy(emailError = "No fue posible registrar la cuenta. Verifica tus datos.") }
                    }
                },
                onFailure = { error ->
                    val message = when {
                        error is uvg.edu.rutau.core.network.BackendApiException -> error.message
                        error is java.io.IOException -> "No fue posible conectar con el servidor. Intenta de nuevo en unos segundos."
                        else -> error.localizedMessage ?: "No pudimos crear la cuenta. Intenta de nuevo."
                    }
                    updateState { copy(emailError = message) }
                },
            )
        }
    }

    /** Crea una cuenta de Google después de validar los datos académicos. */
    fun createAccountWithGoogle(idToken: String?) {
        val state = uiState.value.withValidation()
        mutableUiState.value = state
        if (idToken.isNullOrBlank() || !state.isGoogleFormValid) {
            updateState {
                copy(
                    emailError = if (idToken.isNullOrBlank()) "No fue posible continuar con Google." else null,
                )
            }
            return
        }
        viewModelScope.launch {
            updateState { copy(isLoading = true, emailError = null) }
            val result = runCatching {
                sessionRepository.registerWithGoogle(
                    idToken,
                    SignUpInput(
                        fullName = state.fullName,
                        university = state.university,
                        campus = state.campus,
                        email = state.email,
                        password = "",
                        photoUrl = state.photoUrl,
                        universityId = state.university,
                        campusId = state.campus,
                    ),
                )
            }
            updateState { copy(isLoading = false) }
            result.fold(
                onSuccess = { didRegister ->
                    if (didRegister) mutableEvents.emit(SignUpEvent.AccountCreated)
                    else updateState { copy(emailError = "No fue posible registrar la cuenta con Google.") }
                },
                onFailure = { error ->
                    val message = when {
                        error is uvg.edu.rutau.core.network.BackendApiException -> error.message
                        error is java.io.IOException -> "No fue posible conectar con el servidor. Intenta de nuevo en unos segundos."
                        else -> error.localizedMessage ?: "No pudimos crear la cuenta con Google. Intenta de nuevo."
                    }
                    updateState { copy(emailError = message) }
                },
            )
        }
    }

    private fun updateState(transform: SignUpUiState.() -> SignUpUiState) {
        mutableUiState.update(transform)
    }

    /** Actualiza el formulario y revisa sus errores en el mismo momento. */
    private fun updateValidatedState(transform: SignUpUiState.() -> SignUpUiState) {
        mutableUiState.update { current -> current.transform().withValidation() }
    }

    private fun loadUniversities() {
        val repository = catalogRepository ?: return
        viewModelScope.launch {
            runCatching { repository.universities() }
                .onSuccess { items -> updateState { copy(universities = items, catalogError = null) } }
                .onFailure { updateState { copy(catalogError = "No fue posible cargar las universidades.") } }
        }
    }

    private fun loadCampuses(universityId: String) {
        val repository = catalogRepository ?: return
        viewModelScope.launch {
            runCatching { repository.campuses(universityId) }
                .onSuccess { items -> updateState { copy(campuses = items, catalogError = null) } }
                .onFailure { updateState { copy(campuses = emptyList(), catalogError = "No fue posible cargar los campus.") } }
        }
    }

    companion object {
        fun factory(
            sessionRepository: SessionRepository,
            catalogRepository: CatalogRepository? = null,
        ) = viewModelFactory {
            initializer { SignUpViewModel(sessionRepository, catalogRepository) }
        }
    }
}

/** Revisa los campos visibles para habilitar cada forma de registro. */
private fun SignUpUiState.withValidation(): SignUpUiState {
    val fullNameError = if (fullName.isBlank()) "Ingresa tu nombre completo." else null
    val universityError = if (university.isBlank()) "Selecciona tu universidad." else null
    val campusError = if (campus.isBlank()) "Selecciona tu campus o sede habitual." else null
    val emailError = if (email.isBlank() || !email.contains('@')) {
        "Ingresa un correo electrónico válido."
    } else {
        null
    }
    val passwordError = PasswordRules.validationError(password)
    val confirmPasswordError = when {
        confirmPassword.isBlank() -> "Confirma tu contraseña."
        password != confirmPassword -> "Las contraseñas no coinciden."
        else -> null
    }
    val termsError = if (!termsAccepted) "Debes aceptar los términos y condiciones." else null
    val isGoogleFormValid = listOf(fullNameError, universityError, campusError, termsError).all { it == null }
    val isFormValid = isGoogleFormValid && listOf(emailError, passwordError, confirmPasswordError).all { it == null }
    return copy(
        fullNameError = fullNameError,
        universityError = universityError,
        campusError = campusError,
        emailError = emailError,
        passwordError = passwordError,
        confirmPasswordError = confirmPasswordError,
        termsError = termsError,
        isFormValid = isFormValid,
        isGoogleFormValid = isGoogleFormValid,
    )
}

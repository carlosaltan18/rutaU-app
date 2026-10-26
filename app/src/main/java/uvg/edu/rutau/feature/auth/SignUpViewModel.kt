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

    fun onFullNameChange(value: String) = updateState { copy(fullName = value, fullNameError = null) }
    fun onUniversityChange(value: String) = updateState {
        copy(university = value, campus = "", universityError = null, campusError = null)
    }.also { loadCampuses(value) }
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
                    universityId = state.university,
                    campusId = state.campus,
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

    /** Crea una cuenta de Google después de validar los datos académicos. */
    fun createAccountWithGoogle(idToken: String?) {
        val state = uiState.value
        val emailError = if (state.email.isBlank() || !state.email.contains('@')) {
            "Ingresa el correo institucional que usarás en RutaU."
        } else {
            null
        }
        val fullNameError = if (state.fullName.isBlank()) "Ingresa tu nombre completo." else null
        val universityError = if (state.university.isBlank()) "Selecciona tu universidad." else null
        val campusError = if (state.campus.isBlank()) "Selecciona tu campus o sede habitual." else null
        val termsError = if (!state.termsAccepted) "Debes aceptar los términos y condiciones." else null
        if (idToken.isNullOrBlank() || fullNameError != null || universityError != null || campusError != null || emailError != null || termsError != null) {
            updateState {
                copy(
                    fullNameError = fullNameError,
                    universityError = universityError,
                    campusError = campusError,
                    emailError = emailError ?: "No fue posible continuar con Google.",
                    termsError = termsError,
                )
            }
            return
        }
        viewModelScope.launch {
            updateState { copy(isLoading = true, emailError = null) }
            val didRegister = sessionRepository.registerWithGoogle(
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
            updateState { copy(isLoading = false) }
            if (didRegister) mutableEvents.emit(SignUpEvent.AccountCreated)
            else updateState { copy(emailError = "No pudimos crear la cuenta con Google. Intenta de nuevo.") }
        }
    }

    private fun updateState(transform: SignUpUiState.() -> SignUpUiState) {
        mutableUiState.update(transform)
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

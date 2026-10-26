package uvg.edu.rutau.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uvg.edu.rutau.core.data.repository.SessionRepository
import uvg.edu.rutau.core.data.repository.UserRepository
import uvg.edu.rutau.core.model.UpdateEmailInput
import uvg.edu.rutau.core.model.UpdatePasswordInput
import uvg.edu.rutau.core.model.UpdateProfileInput
import uvg.edu.rutau.core.model.PasswordRules
import uvg.edu.rutau.core.preferences.PopupMessageRepository
import uvg.edu.rutau.core.preferences.ThemeMode
import uvg.edu.rutau.core.preferences.ThemePreferenceRepository
import uvg.edu.rutau.core.data.catalog.CatalogRepository
import uvg.edu.rutau.core.data.catalog.CatalogItem

/** Indica los resultados posibles de las acciones de cuenta. */
sealed interface AccountEvent {
    data object SignedOut : AccountEvent
}

/** Maneja los cambios de la cuenta de la persona actual. */
class AccountViewModel(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
    private val popupMessageRepository: PopupMessageRepository,
    private val themePreferenceRepository: ThemePreferenceRepository,
    private val catalogRepository: CatalogRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<AccountUiState?>(null)
    val uiState = mutableUiState.asStateFlow()

    private val mutableEvents = MutableSharedFlow<AccountEvent>()
    val events: SharedFlow<AccountEvent> = mutableEvents.asSharedFlow()
    private val universityOptions = MutableStateFlow<List<CatalogItem>>(emptyList())
    private val campusOptions = MutableStateFlow<List<CatalogItem>>(emptyList())

    init {
        viewModelScope.launch {
            universityOptions.value = runCatching { catalogRepository.universities() }.getOrDefault(emptyList())
        }
        viewModelScope.launch {
            userRepository.observeCurrentUser().collectLatest { account ->
                campusOptions.value = account?.let { user ->
                    runCatching { catalogRepository.campuses(user.universityId) }.getOrDefault(emptyList())
                }.orEmpty()
            }
        }
        viewModelScope.launch {
            combine(
                userRepository.observeCurrentUser(),
                popupMessageRepository.observeState(),
                themePreferenceRepository.observeThemeMode(),
                universityOptions,
                campusOptions,
            ) { user, popupState, themeMode, universities, campuses ->
                AccountData(user, popupState, themeMode, universities, campuses)
            }.collectLatest { data ->
                val user = data.user
                val popupState = data.popupState
                mutableUiState.value = user?.let { account ->
                    AccountUiState(
                        user = account.copy(photoUrl = savedStateHandle[PhotoUrlKey] ?: account.photoUrl),
                        fullName = savedStateHandle[FullNameKey] ?: account.fullName,
                        university = savedStateHandle[UniversityKey] ?: account.university,
                        universityId = savedStateHandle[UniversityIdKey] ?: account.universityId,
                        universityOptions = data.universities,
                        campus = savedStateHandle[CampusKey] ?: account.campus,
                        campusId = savedStateHandle[CampusIdKey] ?: account.campusId,
                        campusOptions = data.campuses,
                        email = savedStateHandle[EmailKey] ?: account.email,
                        notificationsEnabled = savedStateHandle[NotificationsKey] ?: popupState.popupsEnabled,
                        themeMode = data.themeMode,
                    )
                }
            }
        }
    }

    fun onAction(action: AccountAction) {
        when (action) {
            is AccountAction.FullNameChanged -> updateState { copy(fullName = action.value) }
            is AccountAction.UniversityChanged -> selectUniversity(action.value)
            is AccountAction.CampusChanged -> updateState {
                campusOptions.firstOrNull { it.id == action.value }?.let { campus ->
                    copy(campus = campus.name, campusId = campus.id)
                } ?: this
            }
            is AccountAction.EmailChanged -> updateState { copy(email = action.value) }
            is AccountAction.CurrentEmailPasswordChanged -> updateState { copy(currentEmailPassword = action.value) }
            is AccountAction.CurrentPasswordChanged -> updateState { copy(currentPassword = action.value) }
            is AccountAction.NewPasswordChanged -> updateState { copy(newPassword = action.value) }
            is AccountAction.ConfirmPasswordChanged -> updateState { copy(confirmPassword = action.value) }
            is AccountAction.PhotoChanged -> updateState {
                copy(user = user.copy(photoUrl = action.value))
            }
            is AccountAction.NotificationsChanged -> {
                updateState { copy(notificationsEnabled = action.enabled) }
                viewModelScope.launch { popupMessageRepository.setPopupsEnabled(action.enabled) }
            }
            is AccountAction.ThemeModeChanged -> viewModelScope.launch {
                themePreferenceRepository.setThemeMode(action.value)
            }
            AccountAction.SaveProfile -> saveProfile()
            AccountAction.UpdateEmail -> updateEmail()
            AccountAction.UpdatePassword -> updatePassword()
            AccountAction.LogoutRequested -> updateState { copy(showLogoutConfirmation = true) }
            AccountAction.LogoutDismissed -> updateState { copy(showLogoutConfirmation = false) }
            AccountAction.LogoutConfirmed -> logout()
            is AccountAction.LegalDocumentRequested -> updateState { copy(legalDocument = action.document) }
            AccountAction.LegalDocumentDismissed -> updateState { copy(legalDocument = null) }
            AccountAction.DeleteAccountRequested -> updateState { copy(showDeleteConfirmation = true) }
            AccountAction.DeleteAccountDismissed -> updateState { copy(showDeleteConfirmation = false) }
            AccountAction.DeleteAccountConfirmed -> deleteAccount()
        }
    }

    /** Cambia la universidad y obliga a elegir uno de sus campus. */
    private fun selectUniversity(universityId: String) {
        val selected = universityOptions.value.firstOrNull { it.id == universityId } ?: return
        updateState {
            copy(
                university = selected.name,
                universityId = selected.id,
                campus = "",
                campusId = "",
                campusOptions = emptyList(),
            )
        }
        viewModelScope.launch {
            campusOptions.value = runCatching {
                catalogRepository.campuses(selected.id)
            }.getOrDefault(emptyList())
        }
    }

    private fun saveProfile() {
        val state = uiState.value ?: return
        if (state.fullName.isBlank() || state.university.isBlank() || state.campus.isBlank()) {
            updateState { copy(message = "Completa tu nombre, universidad y campus.") }
            return
        }
        viewModelScope.launch {
            updateState { copy(isSaving = true, message = null) }
            try {
                userRepository.updateProfile(
                    UpdateProfileInput(
                        state.fullName,
                        state.university,
                        state.campus,
                        state.user.photoUrl,
                        universityId = state.universityId,
                        campusId = state.campusId,
                    ),
                )
                updateState { copy(isSaving = false, message = "Tus datos fueron actualizados.") }
            } catch (_: Throwable) {
                updateState {
                    copy(
                        isSaving = false,
                        message = "No fue posible guardar tus datos. Revisa tu conexión e inténtalo de nuevo.",
                    )
                }
            }
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
        val passwordError = PasswordRules.validationError(state.newPassword)
        if (passwordError != null || state.newPassword != state.confirmPassword) {
            updateState {
                copy(message = passwordError ?: "Las contraseñas no coinciden.")
            }
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
            updateState { copy(showLogoutConfirmation = false) }
            sessionRepository.logout()
            mutableEvents.emit(AccountEvent.SignedOut)
        }
    }

    private fun deleteAccount() {
        viewModelScope.launch {
            try {
                userRepository.deleteAccount()
                sessionRepository.logout()
                mutableEvents.emit(AccountEvent.SignedOut)
            } catch (_: Throwable) {
                updateState { copy(showDeleteConfirmation = false, message = "No fue posible eliminar la cuenta. Inténtalo de nuevo.") }
            }
        }
    }

    private fun updateState(transform: AccountUiState.() -> AccountUiState) {
        mutableUiState.update { state ->
            state?.transform()?.also(::persistRestorableFields)
        }
    }

    private fun persistRestorableFields(state: AccountUiState) {
        savedStateHandle[FullNameKey] = state.fullName
        savedStateHandle[UniversityKey] = state.university
        savedStateHandle[UniversityIdKey] = state.universityId
        savedStateHandle[CampusKey] = state.campus
        savedStateHandle[CampusIdKey] = state.campusId
        savedStateHandle[EmailKey] = state.email
        savedStateHandle[PhotoUrlKey] = state.user.photoUrl
        savedStateHandle[NotificationsKey] = state.notificationsEnabled
    }

    companion object {
        fun factory(
            userRepository: UserRepository,
            sessionRepository: SessionRepository,
            popupMessageRepository: PopupMessageRepository,
            themePreferenceRepository: ThemePreferenceRepository,
            catalogRepository: CatalogRepository,
        ) = viewModelFactory {
            initializer {
                AccountViewModel(
                    userRepository = userRepository,
                    sessionRepository = sessionRepository,
                    popupMessageRepository = popupMessageRepository,
                    themePreferenceRepository = themePreferenceRepository,
                    catalogRepository = catalogRepository,
                    savedStateHandle = createSavedStateHandle(),
                )
            }
        }

        private const val FullNameKey = "account_full_name"
        private const val UniversityKey = "account_university"
        private const val UniversityIdKey = "account_university_id"
        private const val CampusKey = "account_campus"
        private const val CampusIdKey = "account_campus_id"
        private const val EmailKey = "account_email"
        private const val PhotoUrlKey = "account_photo_url"
        private const val NotificationsKey = "account_notifications_enabled"
    }
}

/** Reúne los datos locales que necesita la pantalla de cuenta. */
private data class AccountData(
    val user: uvg.edu.rutau.core.model.UserAccount?,
    val popupState: uvg.edu.rutau.core.preferences.PopupMessageState,
    val themeMode: ThemeMode,
    val universities: List<CatalogItem>,
    val campuses: List<CatalogItem>,
)

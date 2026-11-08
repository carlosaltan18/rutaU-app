package uvg.edu.rutau.core.preferences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Guarda el anuncio general que puede mostrarse sobre la aplicación. */
data class PopupMessageUiState(
    val message: AppPopupMessage? = null,
)

/** Lleva la acción elegida en un anuncio hacia la navegación de la aplicación. */
data class PopupMessageNavigation(
    val action: PopupMessageAction,
    val actionValue: String?,
)

/** Maneja los anuncios Firestore sin dejar que la interfaz acceda a repositorios. */
class PopupMessageViewModel(
    feedRepository: PopupMessageFeedRepository,
    private val localRepository: PopupMessageRepository,
    private val eventRepository: PopupMessageEventRepository,
) : ViewModel() {
    private val mutableNavigation = MutableSharedFlow<PopupMessageNavigation>()
    val navigation: SharedFlow<PopupMessageNavigation> = mutableNavigation.asSharedFlow()

    val uiState: StateFlow<PopupMessageUiState> = feedRepository.observeAvailableMessages()
        .map { messages -> PopupMessageUiState(messages.firstOrNull()) }
        .catch { emit(PopupMessageUiState()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PopupMessageUiState(),
        )

    /** Registra que se mostró un anuncio, sin bloquear a la persona usuaria. */
    fun onMessageShown(messageId: String) {
        recordEvent(messageId, PopupMessageEventType.SHOWN)
    }

    /** Oculta el anuncio y conserva esa decisión en el dispositivo. */
    fun dismiss() {
        val message = uiState.value.message ?: return
        viewModelScope.launch { localRepository.markAsSeen(message.id) }
        recordEvent(message.id, PopupMessageEventType.DISMISSED)
    }

    /** Guarda la interacción y abre la sección solicitada por el anuncio. */
    fun confirm() {
        val message = uiState.value.message ?: return
        viewModelScope.launch {
            localRepository.markAsSeen(message.id)
            mutableNavigation.emit(PopupMessageNavigation(message.action, message.actionValue))
        }
        recordEvent(message.id, message.action.confirmEventType())
    }

    private fun recordEvent(messageId: String, eventType: PopupMessageEventType) {
        viewModelScope.launch { eventRepository.record(messageId, eventType) }
    }

    companion object {
        /** Crea este modelo con las fuentes de anuncios de la aplicación. */
        fun factory(
            feedRepository: PopupMessageFeedRepository,
            localRepository: PopupMessageRepository,
            eventRepository: PopupMessageEventRepository,
        ): ViewModelProvider.Factory = popupMessageViewModelFactory {
            PopupMessageViewModel(feedRepository, localRepository, eventRepository)
        }
    }
}

/** Crea el modelo de anuncios sin depender de un contenedor de inyección. */
private inline fun <reified T : ViewModel> popupMessageViewModelFactory(
    crossinline create: () -> T,
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = create() as VM
}

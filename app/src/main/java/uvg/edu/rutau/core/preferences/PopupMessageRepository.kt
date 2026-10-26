package uvg.edu.rutau.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

private val Context.popupMessageDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "rutau_popup_messages",
)

/** Guarda el estado local de los mensajes que ya vio una persona. */
data class PopupMessageState(
    val seenMessageIds: Set<String> = emptySet(),
    val lastViewedAtMillis: Long? = null,
    val popupsEnabled: Boolean = true,
)

/** Maneja únicamente preferencias locales de mensajes emergentes. */
interface PopupMessageRepository {
    fun observeState(): Flow<PopupMessageState>
    suspend fun markAsSeen(messageId: String)
    suspend fun setPopupsEnabled(enabled: Boolean)
}

/** Describe un mensaje remoto sin guardar información privada en el equipo. */
data class AppPopupMessage(
    val id: String,
    val title: String,
    val body: String,
)

/** Indica cómo leer mensajes remotos que la aplicación puede mostrar. */
interface PopupMessageFeedRepository {
    fun observeAvailableMessages(): Flow<List<AppPopupMessage>>
}

/** Lee mensajes de Firestore y excluye los que ya fueron descartados localmente. */
class FirestorePopupMessageFeedRepository(
    private val firestore: FirebaseFirestore,
    private val localRepository: PopupMessageRepository,
) : PopupMessageFeedRepository {
    override fun observeAvailableMessages(): Flow<List<AppPopupMessage>> = callbackFlow {
        val registration = firestore.collection("appMessages").addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(
                snapshot?.documents.orEmpty().mapNotNull { document ->
                    val title = document.getString("title")?.trim().orEmpty()
                    val body = document.getString("body")?.trim().orEmpty()
                    if (title.isBlank() || body.isBlank()) null else AppPopupMessage(document.id, title, body)
                },
            )
        }
        awaitClose { registration.remove() }
    }.combine(localRepository.observeState()) { messages, local ->
        if (!local.popupsEnabled) emptyList() else messages.filterNot { it.id in local.seenMessageIds }
    }
}

/** Guarda las preferencias de mensajes sin almacenar tokens ni datos privados. */
class DataStorePopupMessageRepository(
    context: Context,
) : PopupMessageRepository {
    private val dataStore = context.applicationContext.popupMessageDataStore

    override fun observeState(): Flow<PopupMessageState> = dataStore.data.map { preferences ->
        PopupMessageState(
            seenMessageIds = preferences[SeenMessageIds].orEmpty(),
            lastViewedAtMillis = preferences[LastViewedAtMillis],
            popupsEnabled = preferences[PopupsEnabled] ?: true,
        )
    }

    override suspend fun markAsSeen(messageId: String) {
        if (messageId.isBlank()) return
        dataStore.edit { preferences ->
            preferences[SeenMessageIds] = preferences[SeenMessageIds].orEmpty() + messageId
            preferences[LastViewedAtMillis] = System.currentTimeMillis()
        }
    }

    override suspend fun setPopupsEnabled(enabled: Boolean) {
        dataStore.edit { it[PopupsEnabled] = enabled }
    }

    private companion object {
        val SeenMessageIds = stringSetPreferencesKey("seen_message_ids")
        val LastViewedAtMillis = longPreferencesKey("last_viewed_at_millis")
        val PopupsEnabled = booleanPreferencesKey("popups_enabled")
    }
}

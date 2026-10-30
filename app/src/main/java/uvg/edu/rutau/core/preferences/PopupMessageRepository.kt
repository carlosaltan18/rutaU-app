package uvg.edu.rutau.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Locale
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.tasks.await
import uvg.edu.rutau.core.data.repository.UserRepository
import uvg.edu.rutau.core.model.UserAccount

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
    val audience: String,
    val priority: PopupMessagePriority,
    val action: PopupMessageAction,
    val actionValue: String? = null,
    val startsAtMillis: Long? = null,
    val endsAtMillis: Long? = null,
    val minAppVersionCode: Int = 1,
    val createdAtMillis: Long = 0L,
)

/** Indica la importancia visual de un anuncio remoto. */
enum class PopupMessagePriority {
    LOW,
    NORMAL,
    HIGH,
}

/** Indica la pantalla que puede abrir un anuncio al confirmarlo. */
enum class PopupMessageAction {
    NONE,
    OPEN_PROFILE,
    OPEN_TRIPS,
    OPEN_REQUESTS,
}

/** Indica cómo leer mensajes remotos que la aplicación puede mostrar. */
interface PopupMessageFeedRepository {
    fun observeAvailableMessages(): Flow<List<AppPopupMessage>>
}

/** Registra en Firestore las interacciones con anuncios generales. */
interface PopupMessageEventRepository {
    suspend fun record(messageId: String, eventType: PopupMessageEventType): AppPopupMessageEvent?
}

/** Evento de interacción confirmado por Firestore y seguro para el dominio. */
data class AppPopupMessageEvent(
    val id: String,
    val userId: String,
    val messageId: String,
    val eventType: PopupMessageEventType,
    val appVersionCode: Int,
    val createdAtMillis: Long,
)

/** Indica si un anuncio se mostró, se cerró o abrió una sección. */
enum class PopupMessageEventType {
    SHOWN,
    DISMISSED,
    OPENED,
}

/** Envía eventos simples a Firestore sin afectar la lectura local del anuncio. */
class FirestorePopupMessageEventRepository(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val appVersionCode: Int,
) : PopupMessageEventRepository {
    override suspend fun record(
        messageId: String,
        eventType: PopupMessageEventType,
    ): AppPopupMessageEvent? {
        val userId = firebaseAuth.currentUser?.uid?.takeIf(String::isNotBlank) ?: return null
        if (messageId.isBlank()) return null
        return runCatching {
            val document = firestore.collection(AppMessageEventsCollection).document()
            document.set(
                mapOf(
                    "userId" to userId,
                    "messageId" to messageId,
                    "eventType" to eventType.name,
                    "appVersionCode" to appVersionCode,
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            document.get().await().toAppPopupMessageEvent(userId)
        }.getOrNull()
    }

    private companion object {
        const val AppMessageEventsCollection = "appMessageEvents"
    }
}

/** Mapea únicamente eventos propios y completos antes de exponerlos al dominio. */
private fun DocumentSnapshot.toAppPopupMessageEvent(expectedUserId: String): AppPopupMessageEvent? {
    val userId = getString("userId")?.takeIf { it == expectedUserId } ?: return null
    val messageId = getString("messageId")?.takeIf(String::isNotBlank) ?: return null
    val eventType = getString("eventType")
        ?.let { value -> PopupMessageEventType.entries.firstOrNull { it.name == value } }
        ?: return null
    val appVersionCode = getLong("appVersionCode")?.toInt()?.takeIf { it > 0 } ?: return null
    val createdAtMillis = getTimestamp("createdAt")?.toDate()?.time ?: return null
    return AppPopupMessageEvent(
        id = id,
        userId = userId,
        messageId = messageId,
        eventType = eventType,
        appVersionCode = appVersionCode,
        createdAtMillis = createdAtMillis,
    )
}

/** Lee mensajes de Firestore y excluye los que ya fueron descartados localmente. */
class FirestorePopupMessageFeedRepository(
    private val firestore: FirebaseFirestore,
    private val localRepository: PopupMessageRepository,
    private val userRepository: UserRepository,
    private val appVersionCode: Int,
) : PopupMessageFeedRepository {
    override fun observeAvailableMessages(): Flow<List<AppPopupMessage>> = combine(
        observeRemoteMessages(),
        localRepository.observeState(),
        userRepository.observeCurrentUser(),
        clock(),
    ) { messages, local, user, now ->
        if (!local.popupsEnabled) {
            emptyList()
        } else {
            messages
                .asSequence()
                .filterNot { it.id in local.seenMessageIds }
                .filter { it.isVisibleTo(user, appVersionCode, now) }
                .sortedWith(
                    compareByDescending<AppPopupMessage> { it.priority.rank }
                        .thenByDescending { it.createdAtMillis }
                        .thenBy { it.id },
                )
                .toList()
        }
    }

    /** Lee documentos remotos sin guardar mensajes ni datos personales localmente. */
    private fun observeRemoteMessages(): Flow<List<AppPopupMessage>> = callbackFlow {
        val registration = firestore.collection("appMessages").addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.documents.orEmpty().mapNotNull(DocumentSnapshot::toAppPopupMessage))
        }
        awaitClose { registration.remove() }
    }.catch {
        // Al cerrar sesión o perder permisos, la app solo oculta los anuncios.
        emit(emptyList())
    }

    /** Revisa cada minuto si un mensaje alcanzó su fecha de inicio o vencimiento. */
    private fun clock(): Flow<Long> = flow {
        while (currentCoroutineContext().isActive) {
            emit(System.currentTimeMillis())
            delay(ClockIntervalMillis)
        }
    }

    private companion object {
        const val ClockIntervalMillis = 60_000L
    }
}

/** Convierte un documento válido de Firestore en un mensaje que la app puede mostrar. */
private fun DocumentSnapshot.toAppPopupMessage(): AppPopupMessage? {
    val title = getString("title")?.trim().orEmpty()
    val body = getString("body")?.trim().orEmpty()
    val isActive = get("active") as? Boolean ?: false
    if (title.isBlank() || body.isBlank() || !isActive) return null
    val priority = getString("priority")
        ?.trim()
        ?.uppercase(Locale.ROOT)
        ?.let { value -> PopupMessagePriority.entries.firstOrNull { it.name == value } }
        ?: PopupMessagePriority.NORMAL
    val action = getString("actionType")
        ?.trim()
        ?.uppercase(Locale.ROOT)
        ?.let { value -> PopupMessageAction.entries.firstOrNull { it.name == value } }
        ?: PopupMessageAction.NONE
    return AppPopupMessage(
        id = id,
        title = title,
        body = body,
        audience = getString("audience")?.trim().orEmpty(),
        priority = priority,
        action = action,
        actionValue = getString("actionValue")?.trim()?.takeIf(String::isNotEmpty),
        startsAtMillis = getTimestamp("startsAt")?.toDate()?.time,
        endsAtMillis = getTimestamp("endsAt")?.toDate()?.time,
        minAppVersionCode = getLong("minAppVersionCode")?.toInt() ?: 1,
        createdAtMillis = getTimestamp("createdAt")?.toDate()?.time ?: 0L,
    )
}

/** Decide si el aviso corresponde a la cuenta y versión que está usando la persona. */
internal fun AppPopupMessage.isVisibleTo(
    user: UserAccount?,
    appVersionCode: Int,
    nowMillis: Long,
): Boolean {
    if (minAppVersionCode > appVersionCode) return false
    if (startsAtMillis != null && nowMillis < startsAtMillis) return false
    if (endsAtMillis != null && nowMillis > endsAtMillis) return false
    return when {
        audience == "ALL" -> true
        audience.startsWith("UNIVERSITY:") -> user?.universityId == audience.substringAfter(':')
        audience.startsWith("CAMPUS:") -> user?.campusId == audience.substringAfter(':')
        else -> false
    }
}

/** Devuelve el orden de atención que usa la lista de anuncios. */
private val PopupMessagePriority.rank: Int
    get() = when (this) {
        PopupMessagePriority.HIGH -> 2
        PopupMessagePriority.NORMAL -> 1
        PopupMessagePriority.LOW -> 0
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

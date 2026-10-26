package uvg.edu.rutau.core.notifications

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import uvg.edu.rutau.core.data.repository.SessionRepository
import uvg.edu.rutau.core.data.repository.SessionState
import uvg.edu.rutau.core.network.BackendApi
import uvg.edu.rutau.core.network.RegisterDeviceRequestDto
import uvg.edu.rutau.core.network.requireData
import java.util.UUID

/** Registra el dispositivo actual para recibir avisos de RutaU. */
interface DeviceRegistrationRepository {
    suspend fun registerCurrentDevice()
    suspend fun registerToken(token: String)
    suspend fun unregisterCurrentDevice()
}

/** Obtiene el token que Firebase asignó a este dispositivo. */
interface FcmTokenProvider {
    suspend fun getToken(): String
}

/** Obtiene el token actual usando Firebase Messaging. */
class FirebaseFcmTokenProvider(
    private val firebaseMessaging: FirebaseMessaging,
) : FcmTokenProvider {
    @Suppress("DEPRECATION")
    override suspend fun getToken(): String = firebaseMessaging.getToken().await()
}

/** Guarda únicamente el identificador del dispositivo y una clave de reintento. */
interface DeviceRegistrationStore {
    suspend fun deviceId(): String?
    suspend fun pendingKey(): String?
    suspend fun save(deviceId: String, pendingKey: String?)
    suspend fun clear()
}

/** Conserva el registro local del dispositivo usando DataStore. */
class DataStoreDeviceRegistrationStore(
    private val context: Context,
) : DeviceRegistrationStore {
    override suspend fun deviceId(): String? = context.deviceDataStore.data.first()[DeviceIdKey]

    override suspend fun pendingKey(): String? = context.deviceDataStore.data.first()[PendingKey]

    override suspend fun save(deviceId: String, pendingKey: String?) {
        context.deviceDataStore.edit { preferences ->
            preferences[DeviceIdKey] = deviceId
            if (pendingKey == null) preferences.remove(PendingKey) else preferences[PendingKey] = pendingKey
        }
    }

    override suspend fun clear() {
        context.deviceDataStore.edit { preferences ->
            preferences.remove(DeviceIdKey)
            preferences.remove(PendingKey)
        }
    }

    private companion object {
        val DeviceIdKey: Preferences.Key<String> = stringPreferencesKey("registered_device_id")
        val PendingKey: Preferences.Key<String> = stringPreferencesKey("pending_device_registration_key")
    }
}

/** Mantiene valores solo durante pruebas unitarias. */
class MemoryDeviceRegistrationStore : DeviceRegistrationStore {
    private var savedDeviceId: String? = null
    private var savedPendingKey: String? = null

    override suspend fun deviceId(): String? = savedDeviceId
    override suspend fun pendingKey(): String? = savedPendingKey
    override suspend fun save(deviceId: String, pendingKey: String?) {
        savedDeviceId = deviceId
        savedPendingKey = pendingKey
    }
    override suspend fun clear() {
        savedDeviceId = null
        savedPendingKey = null
    }
}

/** Envía el token FCM y conserva solo el identificador devuelto por el servidor. */
class BackendDeviceRegistrationRepository(
    private val tokenProvider: FcmTokenProvider,
    private val sessionRepository: SessionRepository,
    private val store: DeviceRegistrationStore,
    private val registerDeviceCall: suspend (String, RegisterDeviceRequestDto) -> String,
    private val deleteDeviceCall: suspend (String) -> Unit,
) : DeviceRegistrationRepository {
    constructor(
        context: Context,
        firebaseMessaging: FirebaseMessaging,
        sessionRepository: SessionRepository,
        backendApi: BackendApi,
    ) : this(
        tokenProvider = FirebaseFcmTokenProvider(firebaseMessaging),
        sessionRepository = sessionRepository,
        store = DataStoreDeviceRegistrationStore(context.applicationContext),
        registerDeviceCall = { idempotencyKey, body ->
            backendApi.registerDevice(idempotencyKey, body).requireData().id
        },
        deleteDeviceCall = { deviceId -> backendApi.deleteDevice(deviceId).requireData() },
    )

    override suspend fun registerCurrentDevice() {
        registerToken(tokenProvider.getToken())
    }

    override suspend fun registerToken(token: String) {
        if (token.isBlank() || sessionRepository.sessionState.value !is SessionState.SignedIn) return
        val retryKey = store.pendingKey() ?: UUID.randomUUID().toString()
        store.save(store.deviceId().orEmpty(), retryKey)
        val deviceId = registerDeviceCall(
            retryKey,
            RegisterDeviceRequestDto(token = token, platform = AndroidPlatform),
        )
        store.save(deviceId, pendingKey = null)
    }

    override suspend fun unregisterCurrentDevice() {
        val deviceId = store.deviceId()
        if (!deviceId.isNullOrBlank()) deleteDeviceCall(deviceId)
        store.clear()
    }

    private companion object {
        const val AndroidPlatform = "ANDROID"
    }
}

private val Context.deviceDataStore by preferencesDataStore(name = "rutau_device")

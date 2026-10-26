package uvg.edu.rutau.app

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import uvg.edu.rutau.BuildConfig
import uvg.edu.rutau.core.auth.GoogleSignInCoordinator
import uvg.edu.rutau.core.data.mock.MockRutaUStore
import uvg.edu.rutau.core.data.catalog.CatalogRepository
import uvg.edu.rutau.core.data.catalog.RemoteCatalogRepository
import uvg.edu.rutau.core.data.repository.CoordinationRepository
import uvg.edu.rutau.core.data.repository.FakeCoordinationRepository
import uvg.edu.rutau.core.data.repository.FakeRideRequestRepository
import uvg.edu.rutau.core.data.repository.FakeSessionRepository
import uvg.edu.rutau.core.data.repository.FakeTripRepository
import uvg.edu.rutau.core.data.repository.FakeUserRepository
import uvg.edu.rutau.core.data.repository.FirebaseSessionRepository
import uvg.edu.rutau.core.data.repository.FirebaseUserRepository
import uvg.edu.rutau.core.data.repository.RemoteCoordinationRepository
import uvg.edu.rutau.core.data.repository.RemoteRideRequestRepository
import uvg.edu.rutau.core.data.repository.RemoteTripRepository
import uvg.edu.rutau.core.data.repository.RideRequestRepository
import uvg.edu.rutau.core.data.repository.SessionRepository
import uvg.edu.rutau.core.data.repository.TripRepository
import uvg.edu.rutau.core.data.repository.UserRepository
import uvg.edu.rutau.core.diagnostics.SafeCrashReporter
import uvg.edu.rutau.core.location.FirebaseLiveTripLocationRepository
import uvg.edu.rutau.core.location.LiveTripLocationRepository
import uvg.edu.rutau.core.location.UnconfiguredLiveTripLocationRepository
import uvg.edu.rutau.core.model.UserAccount
import uvg.edu.rutau.core.network.FirebaseIdTokenProvider
import uvg.edu.rutau.core.network.RutaUNetworkClient
import uvg.edu.rutau.core.notifications.BackendDeviceRegistrationRepository
import uvg.edu.rutau.core.notifications.DeviceRegistrationRepository
import uvg.edu.rutau.core.preferences.DataStorePopupMessageRepository
import uvg.edu.rutau.core.preferences.PopupMessageRepository
import uvg.edu.rutau.core.preferences.PopupMessageFeedRepository
import uvg.edu.rutau.core.preferences.FirestorePopupMessageFeedRepository
import uvg.edu.rutau.core.preferences.DataStoreThemePreferenceRepository
import uvg.edu.rutau.core.preferences.ThemePreferenceRepository

/** Reúne los servicios reales de la aplicación y los datos de prueba. */
object RutaUAppDependencies {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var store = MockRutaUStore()
    private var initialized = false

    var sessionRepository: SessionRepository = FakeSessionRepository(store)
        private set
    var userRepository: UserRepository = FakeUserRepository(store)
        private set
    var tripRepository: TripRepository = FakeTripRepository(store)
        private set
    var coordinationRepository: CoordinationRepository = FakeCoordinationRepository(store)
        private set
    var rideRequestRepository: RideRequestRepository = FakeRideRequestRepository(store)
        private set
    lateinit var googleSignInCoordinator: GoogleSignInCoordinator
        private set
    lateinit var popupMessageRepository: PopupMessageRepository
        private set
    lateinit var popupMessageFeedRepository: PopupMessageFeedRepository
        private set
    lateinit var catalogRepository: CatalogRepository
        private set
    lateinit var deviceRegistrationRepository: DeviceRegistrationRepository
        private set
    lateinit var liveTripLocationRepository: LiveTripLocationRepository
        private set
    lateinit var themePreferenceRepository: ThemePreferenceRepository
        private set

    /** Conecta la aplicación normal con Firebase y el servidor de RutaU. */
    fun initialize(context: Context) {
        if (initialized) return
        initialized = true
        val appContext = context.applicationContext
        val firebaseAuth = FirebaseAuth.getInstance()
        val tokenProvider = FirebaseIdTokenProvider(firebaseAuth)
        lateinit var firebaseSessionRepository: FirebaseSessionRepository
        val networkClient = RutaUNetworkClient(
            baseUrl = BuildConfig.API_BASE_URL,
            tokenProvider = tokenProvider,
            onSessionInvalid = { firebaseSessionRepository.onBackendSessionInvalid() },
        )
        firebaseSessionRepository = FirebaseSessionRepository(
            context = appContext,
            firebaseAuth = firebaseAuth,
            backendApi = networkClient.api,
            onBackendSessionReady = {
                appScope.launch {
                    runCatching { deviceRegistrationRepository.registerCurrentDevice() }
                        .onFailure { recordTechnicalError("fcm_device_registration") }
                }
            },
            onBeforeLogout = {
                uvg.edu.rutau.core.location.LiveTripLocationService.stop(appContext)
                if (::deviceRegistrationRepository.isInitialized) {
                    runCatching { deviceRegistrationRepository.unregisterCurrentDevice() }
                        .onFailure { recordTechnicalError("fcm_device_removal") }
                }
            },
        )
        sessionRepository = firebaseSessionRepository
        catalogRepository = RemoteCatalogRepository(networkClient.api)
        userRepository = FirebaseUserRepository(
            firebaseAuth,
            sessionRepository,
            networkClient.api,
            catalogRepository,
        )
        tripRepository = RemoteTripRepository(networkClient.api)
        coordinationRepository = RemoteCoordinationRepository(networkClient.api)
        rideRequestRepository = RemoteRideRequestRepository(networkClient.api)
        googleSignInCoordinator = GoogleSignInCoordinator(appContext)
        popupMessageRepository = DataStorePopupMessageRepository(appContext)
        themePreferenceRepository = DataStoreThemePreferenceRepository(appContext)
        popupMessageFeedRepository = FirestorePopupMessageFeedRepository(
            FirebaseFirestore.getInstance(),
            popupMessageRepository,
        )
        deviceRegistrationRepository = BackendDeviceRegistrationRepository(
            context = appContext,
            firebaseMessaging = FirebaseMessaging.getInstance(),
            sessionRepository = sessionRepository,
            backendApi = networkClient.api,
        )
        liveTripLocationRepository = if (BuildConfig.REALTIME_DATABASE_URL.isBlank()) {
            UnconfiguredLiveTripLocationRepository()
        } else {
            FirebaseLiveTripLocationRepository(BuildConfig.REALTIME_DATABASE_URL)
        }
        if (firebaseAuth.currentUser != null) {
            appScope.launch { firebaseSessionRepository.restoreBackendSession() }
        }
    }

    /** Envía al servidor un token de FCM nuevo cuando existe una sesión. */
    fun registerFcmToken(token: String) {
        if (!initialized) return
        appScope.launch {
            runCatching { deviceRegistrationRepository.registerToken(token) }
                .onFailure { recordTechnicalError("fcm_token_refresh") }
        }
    }

    /** Registra un error técnico sin adjuntar información de una persona. */
    fun recordTechnicalError(category: String) {
        if (initialized) SafeCrashReporter(FirebaseCrashlytics.getInstance()).record(category)
    }

    /** Reinicia los datos de ejemplo para las pruebas de interfaz. */
    internal fun resetForTesting() {
        store = MockRutaUStore()
        sessionRepository = FakeSessionRepository(store)
        userRepository = FakeUserRepository(store)
        tripRepository = FakeTripRepository(store)
        coordinationRepository = FakeCoordinationRepository(store)
        rideRequestRepository = FakeRideRequestRepository(store)
        liveTripLocationRepository = UnconfiguredLiveTripLocationRepository()
    }

    /** Devuelve la cuenta de ejemplo que verifican las pruebas. */
    internal fun currentUserForTesting(): UserAccount? = store.currentUser.value
}

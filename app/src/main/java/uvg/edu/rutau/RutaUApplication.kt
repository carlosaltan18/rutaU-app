package uvg.edu.rutau

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
import uvg.edu.rutau.BuildConfig
import uvg.edu.rutau.app.RutaUAppDependencies
import uvg.edu.rutau.core.firebase.FirebaseAppCheckSetup
import uvg.edu.rutau.core.notifications.RutaUNotificationManager
import org.osmdroid.config.Configuration

/** Prepara los servicios compartidos antes de abrir las pantallas. */
class RutaUApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseCrashlytics.getInstance()
            .setCrashlyticsCollectionEnabled(BuildConfig.CRASHLYTICS_COLLECTION_ENABLED)
        Configuration.getInstance().userAgentValue = "RutaU/1.0 (com.apprutau.rutau)"
        FirebaseAppCheckSetup.configure()
        RutaUNotificationManager(this).ensureChannel()
        RutaUAppDependencies.initialize(this)
    }
}

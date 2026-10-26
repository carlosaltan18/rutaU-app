package uvg.edu.rutau

import android.app.Application
import uvg.edu.rutau.app.RutaUAppDependencies
import uvg.edu.rutau.core.firebase.FirebaseAppCheckSetup
import uvg.edu.rutau.core.notifications.RutaUNotificationManager

/** Prepara los servicios compartidos antes de abrir las pantallas. */
class RutaUApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseAppCheckSetup.configure()
        RutaUNotificationManager(this).ensureChannel()
        RutaUAppDependencies.initialize(this)
    }
}

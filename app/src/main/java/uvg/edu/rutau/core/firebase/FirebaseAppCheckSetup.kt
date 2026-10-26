package uvg.edu.rutau.core.firebase

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import uvg.edu.rutau.BuildConfig

/** Configura App Check sin activar todavía el bloqueo de los servicios. */
object FirebaseAppCheckSetup {
    fun configure() {
        val factory = if (BuildConfig.DEBUG) {
            DebugAppCheckProviderFactory.getInstance()
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(factory)
    }
}

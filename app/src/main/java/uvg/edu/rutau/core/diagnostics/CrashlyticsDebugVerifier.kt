package uvg.edu.rutau.core.diagnostics

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.firebase.crashlytics.FirebaseCrashlytics
import uvg.edu.rutau.BuildConfig

/**
 * Genera una única evidencia no fatal desde una compilación de depuración.
 *
 * No se ejecuta en release ni incluye datos de la cuenta, la sesión o el dispositivo.
 */
object CrashlyticsDebugVerifier {
    private const val PreferencesName = "crashlytics_debug_verification"
    private const val VerificationReportedKey = "verification_reported"
    private const val VerificationCategory = "debug_nonfatal_verification"

    fun reportOnce(context: Context) {
        if (!BuildConfig.DEBUG) return

        val appContext = context.applicationContext
        Handler(Looper.getMainLooper()).postDelayed({
            val preferences = appContext.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            if (preferences.getBoolean(VerificationReportedKey, false)) return@postDelayed

            SafeCrashReporter(FirebaseCrashlytics.getInstance()).record(VerificationCategory)
            preferences.edit().putBoolean(VerificationReportedKey, true).commit()
        }, InitializationDelayMillis)
    }

    private const val InitializationDelayMillis = 3_000L
}

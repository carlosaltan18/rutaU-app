package uvg.edu.rutau.core.diagnostics

import com.google.firebase.crashlytics.FirebaseCrashlytics

/** Envía fallos técnicos sin incluir datos personales ni secretos. */
class SafeCrashReporter(
    private val crashlytics: FirebaseCrashlytics,
) {
    fun record(category: String) {
        crashlytics.setCustomKey("error_category", category.take(80))
        crashlytics.recordException(IllegalStateException("Error técnico de RutaU."))
    }
}

package uvg.edu.rutau.core.network

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth

/** Entrega un token de Firebase para las llamadas protegidas. */
interface FirebaseTokenProvider {
    fun getToken(forceRefresh: Boolean): String?
}

/** Obtiene el token de la persona autenticada sin guardarlo localmente. */
class FirebaseIdTokenProvider(
    private val firebaseAuth: FirebaseAuth,
) : FirebaseTokenProvider {
    override fun getToken(forceRefresh: Boolean): String? {
        val user = firebaseAuth.currentUser ?: return null
        return Tasks.await(user.getIdToken(forceRefresh)).token
    }
}

package uvg.edu.rutau.core.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import uvg.edu.rutau.R

/** Abre el selector de Google y devuelve solo el token necesario para Firebase. */
class GoogleSignInCoordinator(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val credentialManager = CredentialManager.create(appContext)

    suspend fun requestIdToken(activity: Activity): String? {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setServerClientId(appContext.getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build(),
            )
            .build()
        return try {
            val credential = credentialManager.getCredential(activity, request).credential
            if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) return null
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } catch (_: NoCredentialException) {
            null
        } catch (_: GetCredentialException) {
            null
        } catch (_: GoogleIdTokenParsingException) {
            null
        }
    }
}

/** Busca la actividad que puede mostrar el selector de Google. */
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

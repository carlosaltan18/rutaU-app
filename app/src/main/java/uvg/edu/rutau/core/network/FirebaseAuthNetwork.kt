package uvg.edu.rutau.core.network

import java.io.IOException
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/** Agrega el token de Firebase a cada llamada que lo necesite. */
class FirebaseAuthInterceptor(
    private val tokenProvider: FirebaseTokenProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (original.header(AuthorizationHeader) != null) return chain.proceed(original)
        val token = runCatching { tokenProvider.getToken(forceRefresh = false) }.getOrNull()
        val request = token?.let { original.withBearerToken(it) } ?: original
        return chain.proceed(request)
    }

    private companion object {
        const val AuthorizationHeader = "Authorization"
    }
}

/** Intenta renovar el token una vez cuando el servidor responde 401. */
class FirebaseTokenAuthenticator(
    private val tokenProvider: FirebaseTokenProvider,
    private val onSessionInvalid: () -> Unit,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= MaximumAttempts) {
            onSessionInvalid()
            return null
        }
        val tokenResult = runCatching { tokenProvider.getToken(forceRefresh = true) }
        // No cerrar la sesión cuando Firebase no pudo renovar por falta de red.
        if (tokenResult.isFailure) return null
        val token = tokenResult.getOrNull()
        if (token.isNullOrBlank()) {
            onSessionInvalid()
            return null
        }
        return response.request.withBearerToken(token)
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var priorResponse = response.priorResponse
        while (priorResponse != null) {
            count += 1
            priorResponse = priorResponse.priorResponse
        }
        return count
    }

    private companion object {
        const val MaximumAttempts = 2
    }
}

/** Crea una solicitud con el encabezado de autorización de Firebase. */
private fun Request.withBearerToken(token: String): Request = newBuilder()
    .header("Authorization", "Bearer $token")
    .build()

/** Representa un problema de red sin incluir datos privados. */
class BackendNetworkException : IOException("No fue posible comunicarse con RutaU.")

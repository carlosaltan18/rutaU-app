package uvg.edu.rutau.core.network

import com.google.gson.GsonBuilder
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Crea el cliente que se comunica con la API de RutaU. */
class RutaUNetworkClient(
    baseUrl: String,
    tokenProvider: FirebaseTokenProvider,
    onSessionInvalid: () -> Unit,
) {
    val api: BackendApi

    init {
        require(baseUrl.isNotBlank()) {
            "Configura RUTAU_RELEASE_API_BASE_URL antes de crear una versión release."
        }
        require(baseUrl.endsWith('/')) { "La URL de la API debe terminar con /." }
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(FirebaseAuthInterceptor(tokenProvider))
            .authenticator(FirebaseTokenAuthenticator(tokenProvider, onSessionInvalid))
            .build()
        api = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().serializeNulls().create()))
            .build()
            .create(BackendApi::class.java)
    }
}

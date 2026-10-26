package uvg.edu.rutau.core.network

import com.google.gson.Gson
import java.io.IOException
import retrofit2.Response

/** Indica que falta información del contrato para completar una acción. */
class MissingBackendContractException(message: String) : IllegalStateException(message)

/** Explica un error que el servidor de RutaU devolvió de forma segura. */
class BackendApiException(
    val code: String,
    message: String,
) : IOException(message)

/** Devuelve los datos de una respuesta correcta o lanza un error útil. */
fun <T> Response<ApiEnvelopeDto<T>>.requireData(): T {
    return requireEnvelope().data
}

/** Devuelve la respuesta completa cuando también se necesita el cursor. */
fun <T> Response<ApiEnvelopeDto<T>>.requireEnvelope(): ApiEnvelopeDto<T> {
    if (isSuccessful) return body() ?: throw BackendNetworkException()
    val error = runCatching {
        errorBody()?.charStream()?.use { reader ->
            Gson().fromJson(reader, ApiErrorEnvelopeDto::class.java).error
        }
    }.getOrNull()
    if (error != null) throw BackendApiException(error.code, error.message)
    throw BackendNetworkException()
}

/** Comprueba una llamada cuando no se necesita leer su cuerpo. */
fun Response<*>.requireSuccessfulCall() {
    if (!isSuccessful) throw BackendNetworkException()
}

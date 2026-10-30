package uvg.edu.rutau.core.network

import java.util.concurrent.atomic.AtomicBoolean
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Comprueba que los reintentos de Firebase usen un token renovado. */
class FirebaseAuthNetworkTest {
    @Test
    fun addsTheFirebaseTokenToProtectedCalls() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(200))
            val client = OkHttpClient.Builder()
                .addInterceptor(FirebaseAuthInterceptor(FakeFirebaseTokenProvider("current-token")))
                .build()

            client.newCall(Request.Builder().url(server.url("health")).build()).execute().close()

            assertEquals("Bearer current-token", server.takeRequest().getHeader("Authorization"))
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun refreshesTheTokenAfterTheFirstUnauthorizedResponse() {
        val provider = FakeFirebaseTokenProvider("renewed-token")
        val authenticator = FirebaseTokenAuthenticator(provider) { }

        val retry = authenticator.authenticate(null, unauthorizedResponse())

        assertEquals("Bearer renewed-token", retry?.header("Authorization"))
        assertEquals(listOf(true), provider.refreshRequests)
    }

    @Test
    fun closesTheSessionAfterTheSecondUnauthorizedResponse() {
        val sessionClosed = AtomicBoolean(false)
        val firstResponse = unauthorizedResponse()
        val secondResponse = unauthorizedResponse().newBuilder().priorResponse(firstResponse).build()
        val authenticator = FirebaseTokenAuthenticator(FakeFirebaseTokenProvider("renewed-token")) {
            sessionClosed.set(true)
        }

        val retry = authenticator.authenticate(null, secondResponse)

        assertEquals(null, retry)
        assertTrue(sessionClosed.get())
    }

    @Test
    fun keepsTheSessionWhenTokenRefreshFailsWithoutNetwork() {
        val sessionClosed = AtomicBoolean(false)
        val provider = object : FirebaseTokenProvider {
            override fun getToken(forceRefresh: Boolean): String? {
                throw java.io.IOException("offline")
            }
        }
        val authenticator = FirebaseTokenAuthenticator(provider) { sessionClosed.set(true) }

        val retry = authenticator.authenticate(null, unauthorizedResponse())

        assertEquals(null, retry)
        assertFalse(sessionClosed.get())
    }

    private fun unauthorizedResponse(): Response = Response.Builder()
        .request(Request.Builder().url("https://example.test/").build())
        .protocol(Protocol.HTTP_1_1)
        .code(401)
        .message("Unauthorized")
        .build()
}

/** Entrega un token controlado para las pruebas de red. */
private class FakeFirebaseTokenProvider(
    private val token: String,
) : FirebaseTokenProvider {
    val refreshRequests = mutableListOf<Boolean>()

    override fun getToken(forceRefresh: Boolean): String {
        refreshRequests += forceRefresh
        return token
    }
}

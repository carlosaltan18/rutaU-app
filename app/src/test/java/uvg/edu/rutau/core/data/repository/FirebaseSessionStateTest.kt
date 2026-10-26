package uvg.edu.rutau.core.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

/** Comprueba el estado local que representa la sesión de Firebase. */
class FirebaseSessionStateTest {
    @Test
    fun keepsTheFirebaseUidWhenThereIsASignedInPerson() {
        assertEquals(SessionState.SignedIn("firebase-user-1"), firebaseUidToSessionState("firebase-user-1"))
    }

    @Test
    fun returnsSignedOutWhenFirebaseDoesNotHaveAPerson() {
        assertEquals(SessionState.SignedOut, firebaseUidToSessionState(null))
        assertEquals(SessionState.SignedOut, firebaseUidToSessionState("   "))
    }
}

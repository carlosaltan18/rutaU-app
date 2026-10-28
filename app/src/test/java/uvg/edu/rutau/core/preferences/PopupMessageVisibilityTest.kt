package uvg.edu.rutau.core.preferences

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uvg.edu.rutau.core.model.UserAccount

/** Verifica que los anuncios de Firestore solo aparezcan a la audiencia correcta. */
class PopupMessageVisibilityTest {
    private val user = UserAccount(
        id = "student-1",
        fullName = "Ana Pérez",
        university = "Universidad RutaU",
        campus = "Campus Central",
        email = "ana@example.com",
        photoUrl = null,
        universityId = "university-1",
        campusId = "campus-1",
    )

    @Test
    fun `muestra un mensaje general activo dentro de su vigencia`() {
        val message = message(audience = "ALL", startsAtMillis = 100L, endsAtMillis = 300L)

        assertTrue(message.isVisibleTo(user, appVersionCode = 1, nowMillis = 200L))
    }

    @Test
    fun `oculta un mensaje fuera de vigencia o de una versión futura`() {
        val expired = message(audience = "ALL", endsAtMillis = 100L)
        val futureVersion = message(audience = "ALL", minAppVersionCode = 2)

        assertFalse(expired.isVisibleTo(user, appVersionCode = 1, nowMillis = 101L))
        assertFalse(futureVersion.isVisibleTo(user, appVersionCode = 1, nowMillis = 100L))
    }

    @Test
    fun `filtra universidad y campus de la cuenta actual`() {
        val universityMessage = message(audience = "UNIVERSITY:university-1")
        val campusMessage = message(audience = "CAMPUS:campus-1")
        val otherCampusMessage = message(audience = "CAMPUS:campus-2")

        assertTrue(universityMessage.isVisibleTo(user, appVersionCode = 1, nowMillis = 100L))
        assertTrue(campusMessage.isVisibleTo(user, appVersionCode = 1, nowMillis = 100L))
        assertFalse(otherCampusMessage.isVisibleTo(user, appVersionCode = 1, nowMillis = 100L))
    }

    private fun message(
        audience: String,
        startsAtMillis: Long? = null,
        endsAtMillis: Long? = null,
        minAppVersionCode: Int = 1,
    ) = AppPopupMessage(
        id = "welcome",
        title = "Bienvenida",
        body = "Completa tu perfil.",
        audience = audience,
        priority = PopupMessagePriority.NORMAL,
        action = PopupMessageAction.NONE,
        startsAtMillis = startsAtMillis,
        endsAtMillis = endsAtMillis,
        minAppVersionCode = minAppVersionCode,
    )
}

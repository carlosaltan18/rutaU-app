package uvg.edu.rutau.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Comprueba las reglas que se muestran al crear o cambiar una contraseña. */
class PasswordRulesTest {
    @Test
    fun `rejects a password shorter than eight characters`() {
        assertEquals(
            "La contraseña debe tener al menos 8 caracteres.",
            PasswordRules.validationError("Ru1ta"),
        )
    }

    @Test
    fun `rejects a password without uppercase letter or number`() {
        assertEquals(
            "La contraseña debe incluir una letra mayúscula.",
            PasswordRules.validationError("rutau123"),
        )
        assertEquals(
            "La contraseña debe incluir un número.",
            PasswordRules.validationError("RutaUNueva"),
        )
    }

    @Test
    fun `accepts the mock password`() {
        assertNull(PasswordRules.validationError("RutaU123"))
    }
}

package uvg.edu.rutau.core.model

/** Reúne las reglas básicas que deben cumplir las contraseñas. */
object PasswordRules {
    private const val MinimumLength = 8

    fun validationError(password: String): String? = when {
        password.length < MinimumLength -> "La contraseña debe tener al menos 8 caracteres."
        password.none(Char::isUpperCase) -> "La contraseña debe incluir una letra mayúscula."
        password.none(Char::isDigit) -> "La contraseña debe incluir un número."
        else -> null
    }
}

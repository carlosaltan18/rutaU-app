package uvg.edu.rutau.core.navigation

import kotlinx.serialization.Serializable

/** Reúne las rutas disponibles de la aplicación. */
sealed interface AppDestination

/** Lleva a la pantalla para iniciar sesión. */
@Serializable
data object LoginRoute : AppDestination

/** Lleva a la pantalla para crear una cuenta. */
@Serializable
data object SignUpRoute : AppDestination

/** Lleva a la pantalla para recuperar el acceso. */
@Serializable
data object RecoverAccessRoute : AppDestination

/** Lleva a la pantalla que confirma el envío de un correo. */
@Serializable
data object EmailSentRoute : AppDestination

/** Lleva a la pantalla para cambiar la contraseña. */
@Serializable
data object ResetPasswordRoute : AppDestination

/** Lleva a la lista de trayectos. */
@Serializable
data object TripsRoute : AppDestination

/** Lleva al formulario de un trayecto. */
@Serializable
data class TripEditorRoute(val tripId: String? = null) : AppDestination

/** Lleva a las personas compatibles de un trayecto. */
@Serializable
data class MatchesRoute(val tripId: String) : AppDestination

/** Lleva al perfil de una persona compatible. */
@Serializable
data class CandidateProfileRoute(
    val tripId: String,
    val candidateTripId: String,
) : AppDestination

/** Lleva a la confirmación de una solicitud o invitación. */
@Serializable
data class ConfirmCoordinationRoute(
    val tripId: String,
    val candidateTripId: String,
) : AppDestination

/** Lleva a la lista de solicitudes. */
@Serializable
data object RequestsRoute : AppDestination

/** Lleva al detalle de una solicitud. */
@Serializable
data class RequestDetailRoute(val requestId: String) : AppDestination

/** Lleva a un viaje que ya fue aceptado. */
@Serializable
data class CoordinatedRideRoute(val requestId: String) : AppDestination

/** Lleva a la pantalla de cuenta. */
@Serializable
data object AccountRoute : AppDestination

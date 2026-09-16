package uvg.edu.rutau.core.navigation

import kotlinx.serialization.Serializable

/** Contrato de rutas tipadas. Los destinos solo transportan identificadores, nunca objetos completos. */
sealed interface AppDestination

@Serializable
data object LoginRoute : AppDestination

@Serializable
data object SignUpRoute : AppDestination

@Serializable
data object RecoverAccessRoute : AppDestination

@Serializable
data object EmailSentRoute : AppDestination

@Serializable
data object ResetPasswordRoute : AppDestination

@Serializable
data object TripsRoute : AppDestination

@Serializable
data class TripEditorRoute(val tripId: String? = null) : AppDestination

@Serializable
data class MatchesRoute(val tripId: String) : AppDestination

@Serializable
data class CandidateProfileRoute(
    val tripId: String,
    val candidateTripId: String,
) : AppDestination

@Serializable
data class ConfirmCoordinationRoute(
    val tripId: String,
    val candidateTripId: String,
) : AppDestination

@Serializable
data object RequestsRoute : AppDestination

@Serializable
data class RequestDetailRoute(val requestId: String) : AppDestination

@Serializable
data class CoordinatedRideRoute(val requestId: String) : AppDestination

@Serializable
data object AccountRoute : AppDestination

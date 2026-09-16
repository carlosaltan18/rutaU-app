package uvg.edu.rutau.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import uvg.edu.rutau.core.designsystem.component.RutaUScreenContainer
import uvg.edu.rutau.core.designsystem.component.RutaUTopAppBar
import uvg.edu.rutau.core.navigation.AccountRoute
import uvg.edu.rutau.core.navigation.CandidateProfileRoute
import uvg.edu.rutau.core.navigation.ConfirmCoordinationRoute
import uvg.edu.rutau.core.navigation.CoordinatedRideRoute
import uvg.edu.rutau.core.navigation.EmailSentRoute
import uvg.edu.rutau.core.navigation.LoginRoute
import uvg.edu.rutau.core.navigation.MatchesRoute
import uvg.edu.rutau.core.navigation.RecoverAccessRoute
import uvg.edu.rutau.core.navigation.RequestDetailRoute
import uvg.edu.rutau.core.navigation.RequestsRoute
import uvg.edu.rutau.core.navigation.ResetPasswordRoute
import uvg.edu.rutau.core.navigation.SignUpRoute
import uvg.edu.rutau.core.navigation.TripEditorRoute
import uvg.edu.rutau.core.navigation.TripsRoute

/**
 * Host central de navegación. Cada módulo reemplazará su destino temporal por su Route real.
 */
@Composable
fun RutaUNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: Any = LoginRoute,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable<LoginRoute> { TemporaryDestination("Iniciar sesión") }
        composable<SignUpRoute> { TemporaryDestination("Crear cuenta") }
        composable<RecoverAccessRoute> { TemporaryDestination("Recuperar acceso") }
        composable<EmailSentRoute> { TemporaryDestination("Revisa tu correo") }
        composable<ResetPasswordRoute> { TemporaryDestination("Restablecer contraseña") }
        composable<TripsRoute> { TemporaryDestination("Mis trayectos") }
        composable<TripEditorRoute> { TemporaryDestination("Crear o editar trayecto") }
        composable<MatchesRoute> { TemporaryDestination("Compañeros compatibles") }
        composable<CandidateProfileRoute> { TemporaryDestination("Perfil compatible") }
        composable<ConfirmCoordinationRoute> { TemporaryDestination("Confirmar coordinación") }
        composable<RequestsRoute> { TemporaryDestination("Solicitudes") }
        composable<RequestDetailRoute> { TemporaryDestination("Detalle de solicitud") }
        composable<CoordinatedRideRoute> { TemporaryDestination("Viaje coordinado") }
        composable<AccountRoute> { TemporaryDestination("Cuenta y configuración") }
    }
}

@Composable
private fun TemporaryDestination(title: String) {
    RutaUScreenContainer(
        topBar = { RutaUTopAppBar(title = title) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

package uvg.edu.rutau.feature.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uvg.edu.rutau.core.designsystem.component.RutaUInfoCard
import uvg.edu.rutau.core.designsystem.component.RutaUPrimaryButton
import uvg.edu.rutau.core.designsystem.component.RutaUScreenContainer
import uvg.edu.rutau.core.designsystem.component.RutaUTopAppBar
import uvg.edu.rutau.core.model.Trip
import uvg.edu.rutau.core.model.TripRole

@Composable
fun TripsRoute(
    onCreateTrip: () -> Unit,
    onEditTrip: (String) -> Unit,
    onOpenMatches: (String) -> Unit,
    viewModel: TripsViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingRemoval by remember { mutableStateOf<Trip?>(null) }
    RutaUScreenContainer(
        topBar = { RutaUTopAppBar("Mis trayectos") },
        bottomBar = {
            Column(Modifier.padding(16.dp)) {
                FloatingActionButton(onClick = onCreateTrip, modifier = Modifier.align(Alignment.End)) {
                    Icon(Icons.Default.Add, "Crear trayecto")
                }
            }
        },
    ) { padding ->
        if (state.trips.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Aún no tienes trayectos", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.size(8.dp))
                Text("Publica tu horario habitual para encontrar estudiantes compatibles.")
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { Text("Publica como pasajero o conductor. Cada trayecto mantiene un único rol.") }
                items(state.trips, key = Trip::id) { trip ->
                    TripCard(
                        trip,
                        onEdit = { onEditTrip(trip.id) },
                        onMatches = { onOpenMatches(trip.id) },
                        onRemove = { pendingRemoval = trip },
                    )
                }
                item { Spacer(Modifier.size(64.dp)) }
            }
        }
    }
    pendingRemoval?.let { trip ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("Eliminar trayecto") },
            text = { Text("Esta acción quitará el trayecto de tu lista.") },
            confirmButton = {
                TextButton(onClick = { viewModel.remove(trip.id); pendingRemoval = null }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { pendingRemoval = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
fun TripEditorRoute(onBack: () -> Unit, viewModel: TripEditorViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RutaUScreenContainer(
        topBar = {
            RutaUTopAppBar(
                if (state.isEditing) "Editar trayecto" else "Crear trayecto",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationIconContentDescription = "Volver",
                onNavigationClick = onBack,
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                TripEditorForm(
                    state,
                    viewModel::setOrigin,
                    viewModel::setCampus,
                    viewModel::setDay,
                    viewModel::setTime,
                    viewModel::setRole,
                    viewModel::setSeats,
                )
            }
            item {
                RutaUPrimaryButton(
                    if (state.isEditing) "Guardar cambios" else "Publicar trayecto",
                    onClick = { viewModel.save { onBack() } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving,
                )
            }
        }
    }
}

@Composable
fun MatchesRoute(onBack: () -> Unit, onOpenCandidate: (String) -> Unit, viewModel: MatchesViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showRules by remember { mutableStateOf(false) }
    val title = if (state.sourceTrip?.role == TripRole.PASSENGER) "Conductores compatibles" else "Pasajeros compatibles"
    RutaUScreenContainer(
        topBar = {
            RutaUTopAppBar(title, navigationIcon = Icons.AutoMirrored.Filled.ArrowBack, navigationIconContentDescription = "Volver", onNavigationClick = onBack)
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            state.sourceTrip?.let { trip -> item { TripSummaryCard(trip) } }
            item { CompatibilitySummaryCard(state.matches.size) }
            item {
                TextButton(onClick = { showRules = !showRules }) {
                    Text(if (showRules) "Ocultar cómo calculamos" else "Cómo calculamos la compatibilidad")
                }
            }
            if (showRules) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CompatibilityRuleItem("Misma zona de origen")
                        CompatibilityRuleItem("Mismo campus y día")
                        CompatibilityRuleItem("Hasta 30 minutos de diferencia")
                        CompatibilityRuleItem("Roles complementarios")
                        CompatibilityRuleItem("Conductor con plazas disponibles")
                    }
                }
            }
            if (state.matches.isEmpty()) {
                item { RutaUInfoCard("Sin resultados por ahora", "Prueba otro horario o vuelve más tarde cuando haya nuevos trayectos.") }
            } else {
                items(state.matches, key = { it.trip.id }) { match ->
                    MatchCard(match, onOpen = { onOpenCandidate(match.trip.id) })
                }
            }
        }
    }
}

@Composable
fun CandidateProfileRoute(onBack: () -> Unit, onCoordinate: () -> Unit, viewModel: CandidateProfileViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val title = if (state.candidateTrip?.role == TripRole.DRIVER) "Perfil del conductor" else "Perfil del pasajero"
    RutaUScreenContainer(
        topBar = {
            RutaUTopAppBar(title, navigationIcon = Icons.AutoMirrored.Filled.ArrowBack, navigationIconContentDescription = "Volver", onNavigationClick = onBack)
        },
    ) { padding ->
        val student = state.student
        val trip = state.candidateTrip
        if (student == null || trip == null) {
            Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item { CandidateProfileHeader(student) }
                item { CandidateTripCard(trip) }
                item { PrivacyNoticeCard() }
                item {
                    RutaUPrimaryButton(
                        if (state.sourceTrip?.role == TripRole.PASSENGER) "Solicitar unirme" else "Invitar a mi trayecto",
                        onCoordinate,
                        Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
fun ConfirmCoordinationRoute(onBack: () -> Unit, onConfirmed: (String) -> Unit, viewModel: ConfirmCoordinationViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RutaUScreenContainer(
        topBar = {
            RutaUTopAppBar("Confirmar coordinación", navigationIcon = Icons.AutoMirrored.Filled.ArrowBack, navigationIconContentDescription = "Volver", onNavigationClick = onBack)
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            ConfirmCoordinationSheet(
                state,
                viewModel::setMessage,
                viewModel::setContribution,
                onConfirm = { viewModel.confirm(onConfirmed) },
            )
        }
    }
}

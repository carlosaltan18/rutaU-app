package uvg.edu.rutau.feature.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
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
import uvg.edu.rutau.core.designsystem.component.RutaUConfirmationDialog
import uvg.edu.rutau.core.designsystem.component.RutaUDestructiveButton
import uvg.edu.rutau.core.designsystem.component.RutaUPrimaryButton
import uvg.edu.rutau.core.designsystem.component.RutaUSecondaryButton
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
    TripsScreen(
        state = state,
        onCreateTrip = onCreateTrip,
        onEditTrip = onEditTrip,
        onOpenMatches = onOpenMatches,
        onDeactivateTrip = { viewModel.deactivate(it) },
        onRemoveTrip = { viewModel.remove(it) },
    )
}

@Composable
fun TripsScreen(
    state: TripsUiState,
    onCreateTrip: () -> Unit,
    onEditTrip: (String) -> Unit,
    onOpenMatches: (String) -> Unit,
    onDeactivateTrip: (String) -> Unit,
    onRemoveTrip: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingRemoval by remember { mutableStateOf<Trip?>(null) }
    RutaUScreenContainer(
        modifier = modifier,
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
                        onDeactivate = { onDeactivateTrip(trip.id) },
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
                TextButton(onClick = { onRemoveTrip(trip.id); pendingRemoval = null }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { pendingRemoval = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
fun TripEditorRoute(onBack: () -> Unit, viewModel: TripEditorViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TripEditorScreen(
        state = state,
        onOriginChange = viewModel::setOrigin,
        onCampusChange = viewModel::setCampus,
        onDayChange = viewModel::setDay,
        onTimeChange = viewModel::setTime,
        onRoleChange = viewModel::setRole,
        onSeatsChange = viewModel::setSeats,
        onSave = { viewModel.save { onBack() } },
        onDelete = { viewModel.delete(onBack) },
        onBack = onBack,
    )
}

@Composable
fun TripEditorScreen(
    state: TripEditorUiState,
    onOriginChange: (String) -> Unit,
    onCampusChange: (String) -> Unit,
    onDayChange: (String) -> Unit,
    onTimeChange: (java.time.LocalTime) -> Unit,
    onRoleChange: (TripRole) -> Unit,
    onSeatsChange: (Int) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    RutaUScreenContainer(
        modifier = modifier,
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
                    onOriginChange,
                    onCampusChange,
                    onDayChange,
                    onTimeChange,
                    onRoleChange,
                    onSeatsChange,
                )
            }
            item {
                RutaUPrimaryButton(
                    if (state.isEditing) "Guardar cambios" else "Publicar trayecto",
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving,
                )
            }
            item {
                RutaUSecondaryButton(
                    text = "Cancelar",
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (state.isEditing) {
                item {
                    RutaUDestructiveButton(
                        text = "Eliminar este trayecto",
                        onClick = { showDeleteConfirmation = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
    if (showDeleteConfirmation) {
        RutaUConfirmationDialog(
            title = "¿Eliminar este trayecto?",
            message = "Se eliminará de tu lista y dejará de aparecer en resultados compatibles.",
            confirmLabel = "Eliminar trayecto",
            onConfirm = onDelete,
            onDismiss = { showDeleteConfirmation = false },
            isDestructive = true,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesRoute(onBack: () -> Unit, onOpenCandidate: (String) -> Unit, viewModel: MatchesViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MatchesScreen(state = state, onBack = onBack, onOpenCandidate = onOpenCandidate)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesScreen(
    state: MatchesUiState,
    onBack: () -> Unit,
    onOpenCandidate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showRules by remember { mutableStateOf(false) }
    val title = if (state.sourceTrip?.role == TripRole.PASSENGER) "Conductores compatibles" else "Pasajeros compatibles"
    RutaUScreenContainer(
        modifier = modifier,
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
                TextButton(onClick = { showRules = true }) {
                    Text("Cómo calculamos la compatibilidad")
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
    if (showRules) {
        ModalBottomSheet(onDismissRequest = { showRules = false }) {
            CompatibilityInfoSheet(onDismiss = { showRules = false })
        }
    }
}

@Composable
fun CandidateProfileRoute(onBack: () -> Unit, onCoordinate: () -> Unit, viewModel: CandidateProfileViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CandidateProfileScreen(
        state = state,
        onBack = onBack,
        onCoordinate = onCoordinate,
        onContributionChange = viewModel::setContribution,
    )
}

@Composable
fun CandidateProfileScreen(
    state: CandidateProfileUiState,
    onBack: () -> Unit,
    onCoordinate: () -> Unit,
    onContributionChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = if (state.candidateTrip?.role == TripRole.DRIVER) "Perfil del conductor" else "Perfil del pasajero"
    RutaUScreenContainer(
        modifier = modifier,
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
                state.sourceTrip?.let { source ->
                    item { CompatibilityReasonsCard(source, trip) }
                }
                item {
                    ContributionEditor(
                        value = state.contributionQuetzales,
                        onValueChange = onContributionChange,
                    )
                }
                item { PrivacyNoticeCard() }
                item {
                    RutaUPrimaryButton(
                        if (state.sourceTrip?.role == TripRole.PASSENGER) "Solicitar unirme" else "Invitar a mi trayecto",
                        onCoordinate,
                        Modifier.fillMaxWidth(),
                    )
                }
                item {
                    RutaUSecondaryButton(
                        text = "Volver a resultados",
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
fun ConfirmCoordinationRoute(onBack: () -> Unit, onConfirmed: (String) -> Unit, viewModel: ConfirmCoordinationViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ConfirmCoordinationScreen(
        state = state,
        onMessageChange = viewModel::setMessage,
        onContributionChange = viewModel::setContribution,
        onConfirm = { viewModel.confirm(onConfirmed) },
        onBack = onBack,
    )
}

@Composable
fun ConfirmCoordinationScreen(
    state: ConfirmCoordinationUiState,
    onMessageChange: (String) -> Unit,
    onContributionChange: (Int) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RutaUScreenContainer(
        modifier = modifier,
        topBar = {
            RutaUTopAppBar("Confirmar coordinación", navigationIcon = Icons.AutoMirrored.Filled.ArrowBack, navigationIconContentDescription = "Volver", onNavigationClick = onBack)
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ConfirmCoordinationSheet(
                state,
                onMessageChange,
                onContributionChange,
                onConfirm,
            )
            RutaUSecondaryButton(
                text = "Cancelar",
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

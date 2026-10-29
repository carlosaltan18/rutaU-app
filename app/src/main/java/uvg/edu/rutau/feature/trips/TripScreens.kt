package uvg.edu.rutau.feature.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import uvg.edu.rutau.core.model.CoordinationStatus

/** Separa las rutas habituales de los viajes actuales y de los ya terminados. */
private enum class TripSection {
    ACTIVE,
    BASE,
    SAVED,
    HISTORY,
}

@Composable
fun TripsRoute(
    onCreateTrip: () -> Unit,
    onEditTrip: (String) -> Unit,
    onOpenMatches: (String) -> Unit,
    onOpenCoordinatedRide: (String) -> Unit,
    viewModel: TripsViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TripsScreen(
        state = state,
        onCreateTrip = onCreateTrip,
        onEditTrip = onEditTrip,
        onOpenMatches = onOpenMatches,
        onOpenCoordinatedRide = onOpenCoordinatedRide,
        onDeactivateTrip = { viewModel.deactivate(it) },
        onRemoveTrip = { viewModel.remove(it) },
        onRemoveFavorite = viewModel::removeFavorite,
        onRetry = viewModel::retry,
    )
}

@Composable
fun TripsScreen(
    state: TripsUiState,
    onCreateTrip: () -> Unit,
    onEditTrip: (String) -> Unit,
    onOpenMatches: (String) -> Unit,
    onOpenCoordinatedRide: (String) -> Unit,
    onDeactivateTrip: (String) -> Unit,
    onRemoveTrip: (String) -> Unit,
    onRemoveFavorite: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingRemoval by remember { mutableStateOf<Trip?>(null) }
    val activeCoordinations = state.coordinatedTrips.filter {
        it.status == CoordinationStatus.CONFIRMED
    }
    val tripHistory = state.coordinatedTrips.filter {
        it.status != CoordinationStatus.CONFIRMED
    }
    var savedSection by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedSection = savedSection
        ?.let { saved -> TripSection.entries.firstOrNull { it.name == saved } }
        ?: TripSection.BASE
    RutaUScreenContainer(
        modifier = modifier,
        topBar = { RutaUTopAppBar("Trayectos") },
        floatingActionButton = {
            if (selectedSection == TripSection.BASE) {
                ExtendedFloatingActionButton(
                    onClick = onCreateTrip,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Crear ruta habitual") },
                )
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                top = 12.dp,
                end = 16.dp,
                bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                TripSectionSelector(
                    selected = selectedSection,
                    onSelected = { section -> savedSection = section.name },
                )
            }
            when (state.contentState) {
                TripListContentState.LOADING -> item { LoadingTripsCard() }
                TripListContentState.ERROR -> item {
                    TripsErrorCard(state.error.orEmpty(), onRetry)
                }
                TripListContentState.CONTENT,
                TripListContentState.EMPTY,
                -> {
                    state.error?.let { message ->
                        item { Text(message, color = MaterialTheme.colorScheme.error) }
                    }
                    when (selectedSection) {
                TripSection.ACTIVE -> {
                    item {
                        RutaUInfoCard(
                            title = "Viajes coordinados",
                            message = "Aquí aparecen los viajes que ya aceptaron ambas personas. Abre uno para ver pasajeros, punto de encuentro y acciones.",
                        )
                    }
                    if (activeCoordinations.isEmpty()) {
                        item {
                            EmptyTripSection("Aún no tienes viajes coordinados. Primero crea una ruta habitual y envía una solicitud a una persona compatible.")
                        }
                    } else {
                        items(activeCoordinations, key = { it.requestId }) { detail ->
                            CoordinatedTripCard(detail, onOpen = { onOpenCoordinatedRide(detail.requestId) })
                        }
                    }
                }
                TripSection.BASE -> {
                    item {
                        RutaUInfoCard(
                            title = "Rutas habituales",
                            message = "Paso 1: registra aquí la ruta que sueles recorrer. Después podrás buscar personas compatibles; esto todavía no confirma un viaje.",
                        )
                    }
                    if (state.trips.isEmpty()) {
                        item {
                            EmptyTripSection("Aún no tienes rutas habituales. Usa “Crear ruta habitual” para registrar la primera.")
                        }
                    } else {
                        items(state.trips, key = Trip::id) { trip ->
                            TripCard(
                                trip,
                                onEdit = { onEditTrip(trip.id) },
                                onMatches = { onOpenMatches(trip.id) },
                                onDeactivate = { onDeactivateTrip(trip.id) },
                                onRemove = { pendingRemoval = trip },
                            )
                        }
                    }
                }
                TripSection.SAVED -> {
                    item {
                        RutaUInfoCard(
                            title = "Perfiles guardados",
                            message = "Guarda perfiles compatibles para consultarlos aunque cierres la aplicación.",
                        )
                    }
                    if (state.favorites.isEmpty()) {
                        item { EmptyTripSection("Aún no has guardado perfiles compatibles.") }
                    } else {
                        items(state.favorites, key = { it.tripId }) { favorite ->
                            FavoriteTripCard(favorite, onRemove = { onRemoveFavorite(favorite.tripId) })
                        }
                    }
                }
                TripSection.HISTORY -> {
                    item {
                        RutaUInfoCard(
                            title = "Historial de viajes coordinados",
                            message = "Consulta aquí los viajes que finalizaste o cancelaste.",
                        )
                    }
                    if (tripHistory.isEmpty()) {
                        item {
                            EmptyTripSection("Aún no tienes viajes finalizados ni cancelados.")
                        }
                    } else {
                        items(tripHistory, key = { it.requestId }) { detail ->
                            CoordinatedTripCard(detail, onOpen = { onOpenCoordinatedRide(detail.requestId) })
                        }
                    }
                }
                    }
                }
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

/** Muestra una espera clara mientras llegan los trayectos de la cuenta. */
@Composable
private fun LoadingTripsCard() {
    RutaUInfoCard(
        title = "Cargando trayectos",
        message = "Estamos consultando tus rutas y viajes coordinados.",
    )
}

/** Explica que la lista falló y ofrece volver a consultar sin cerrar la app. */
@Composable
private fun TripsErrorCard(
    message: String,
    onRetry: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RutaUInfoCard(
            title = "No pudimos cargar los trayectos",
            message = message.ifBlank { "Revisa tu conexión e inténtalo de nuevo." },
        )
        RutaUPrimaryButton(
            text = "Reintentar",
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Muestra los botones que cambian la sección principal de Trayectos. */
@Composable
private fun TripSectionSelector(
    selected: TripSection,
    onSelected: (TripSection) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TripSection.entries.forEach { section ->
            val label = when (section) {
                TripSection.ACTIVE -> "Viajes"
                TripSection.BASE -> "Rutas"
                TripSection.SAVED -> "Guardados"
                TripSection.HISTORY -> "Historial"
            }
            FilterChip(
                selected = selected == section,
                onClick = { onSelected(section) },
                label = { Text(label) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Explica con claridad cuando la sección elegida todavía no tiene elementos. */
@Composable
private fun EmptyTripSection(message: String) {
    RutaUInfoCard(title = "Sin resultados", message = message)
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
fun CandidateProfileRoute(onBack: () -> Unit, onCoordinate: (String?, Int) -> Unit, viewModel: CandidateProfileViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CandidateProfileScreen(
        state = state,
        onBack = onBack,
        onCoordinate = onCoordinate,
        onContributionChange = viewModel::setContribution,
        onToggleFavorite = viewModel::toggleFavorite,
    )
}

@Composable
fun CandidateProfileScreen(
    state: CandidateProfileUiState,
    onBack: () -> Unit,
    onCoordinate: (String?, Int) -> Unit,
    onContributionChange: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
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
                    RutaUSecondaryButton(
                        text = if (state.isFavorite) "Quitar de guardados" else "Guardar perfil",
                        onClick = onToggleFavorite,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    RutaUPrimaryButton(
                        when (state.existingRequestStatus) {
                            uvg.edu.rutau.core.model.RequestStatus.PENDING -> "Solicitud enviada · Ver detalle"
                            uvg.edu.rutau.core.model.RequestStatus.ACCEPTED -> "Solicitud aceptada · Ver detalle"
                            else -> if (state.sourceTrip?.role == TripRole.PASSENGER) "Solicitar unirme" else "Invitar a mi trayecto"
                        },
                        { onCoordinate(state.existingRequestId, state.contributionQuetzales) },
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

package uvg.edu.rutau.feature.requests

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uvg.edu.rutau.core.designsystem.component.RutaUConfirmationDialog
import uvg.edu.rutau.core.designsystem.component.RutaUDestructiveButton
import uvg.edu.rutau.core.designsystem.component.RutaUInfoCard
import uvg.edu.rutau.core.designsystem.component.RutaUPrimaryButton
import uvg.edu.rutau.core.designsystem.component.RutaUSecondaryButton
import uvg.edu.rutau.core.designsystem.component.RutaUScreenContainer
import uvg.edu.rutau.core.designsystem.component.RutaUTopAppBar
import uvg.edu.rutau.core.model.RequestStatus
import uvg.edu.rutau.core.model.RideRequestDetails
import uvg.edu.rutau.core.model.TripRole

/** Conecta la lista de solicitudes con los datos compartidos. */
@Composable
fun RequestsRoute(
    onOpenRequest: (String) -> Unit,
    viewModel: RequestsViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RequestsScreen(
        state = state,
        onTabSelected = viewModel::selectTab,
        onFilterSelected = viewModel::selectFilter,
        onOpenRequest = onOpenRequest,
    )
}

/** Muestra las solicitudes recibidas y enviadas de la persona actual. */
@Composable
fun RequestsScreen(
    state: RequestsUiState,
    onTabSelected: (RequestTab) -> Unit,
    onFilterSelected: (RequestFilter) -> Unit,
    onOpenRequest: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    RutaUScreenContainer(
        modifier = modifier,
        topBar = { RutaUTopAppBar("Solicitudes") },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                RequestTabSelector(state.selectedTab, onTabSelected)
            }
            item {
                RequestFilterSelector(state.selectedFilter, onFilterSelected)
            }
            if (state.requests.isEmpty()) {
                item { EmptyRequestsCard() }
            } else {
                items(state.requests, key = { it.request.id }) { detail ->
                    RequestCard(
                        detail = detail,
                        isReceived = state.selectedTab == RequestTab.RECEIVED,
                        onOpen = { onOpenRequest(detail.request.id) },
                    )
                }
            }
            item { Spacer(Modifier.size(64.dp)) }
        }
    }
}

/** Conecta el detalle de una solicitud con los datos compartidos. */
@Composable
fun RequestDetailRoute(
    onBack: () -> Unit,
    onOpenCoordinatedRide: () -> Unit,
    viewModel: RequestDetailViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RequestDetailScreen(
        state = state,
        onBack = onBack,
        onAccept = { viewModel.accept(onOpenCoordinatedRide) },
        onReject = viewModel::reject,
        onCancel = viewModel::cancel,
        onOpenCoordinatedRide = onOpenCoordinatedRide,
    )
}

/** Muestra el detalle y las acciones de una solicitud. */
@Composable
fun RequestDetailScreen(
    state: RequestDetailUiState,
    onBack: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onCancel: () -> Unit,
    onOpenCoordinatedRide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmation by remember { mutableStateOf<RequestAction?>(null) }
    RutaUScreenContainer(
        modifier = modifier,
        topBar = {
            RutaUTopAppBar(
                title = "Detalle de solicitud",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationIconContentDescription = "Volver",
                onNavigationClick = onBack,
            )
        },
    ) { padding ->
        val detail = state.detail
        if (detail == null) {
            LoadingRequestScreen(Modifier.padding(padding))
        } else {
            val isReceived = detail.targetTrip.ownerId == state.currentUserId
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { RequestStatusChip(detail.request.status) }
                item { RequestPersonCard(detail, isReceived) }
                item { RequestTripSummary(detail) }
                detail.request.message?.let { message ->
                    item {
                        RutaUInfoCard(
                            title = "Mensaje",
                            message = message,
                        )
                    }
                }
                item {
                    Text(
                        "Contribución sugerida: ${detail.request.contributionCents.asQuetzales()}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                state.error?.let { error ->
                    item { Text(error, color = MaterialTheme.colorScheme.error) }
                }
                item {
                    RequestActions(
                        detail = detail,
                        isReceived = isReceived,
                        hasCoordination = state.coordination != null,
                        isSaving = state.isSaving,
                        onAccept = { confirmation = RequestAction.ACCEPT },
                        onReject = { confirmation = RequestAction.REJECT },
                        onCancel = { confirmation = RequestAction.CANCEL },
                        onOpenCoordinatedRide = onOpenCoordinatedRide,
                        onBack = onBack,
                    )
                }
            }
        }
    }
    confirmation?.let { action ->
        RutaUConfirmationDialog(
            title = action.title,
            message = action.message,
            confirmLabel = action.confirmLabel,
            onConfirm = {
                confirmation = null
                when (action) {
                    RequestAction.ACCEPT -> onAccept()
                    RequestAction.REJECT -> onReject()
                    RequestAction.CANCEL -> onCancel()
                }
            },
            onDismiss = { confirmation = null },
            isDestructive = action != RequestAction.ACCEPT,
        )
    }
}

/** Conecta un viaje aceptado con los datos compartidos. */
@Composable
fun CoordinatedRideRoute(
    onBack: () -> Unit,
    viewModel: CoordinatedRideViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CoordinatedRideScreen(
        state = state,
        onBack = onBack,
        onCancelRide = { viewModel.cancelRide(onBack) },
        onCancelParticipation = { viewModel.cancelParticipation(onBack) },
    )
}

/** Muestra la información de un viaje que ya fue aceptado. */
@Composable
fun CoordinatedRideScreen(
    state: CoordinatedRideUiState,
    onBack: () -> Unit,
    onCancelRide: () -> Unit,
    onCancelParticipation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmCancellation by remember { mutableStateOf(false) }
    RutaUScreenContainer(
        modifier = modifier,
        topBar = {
            RutaUTopAppBar(
                title = "Viaje coordinado",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationIconContentDescription = "Volver",
                onNavigationClick = onBack,
            )
        },
    ) { padding ->
        val detail = state.detail
        if (detail == null || state.coordination == null) {
            LoadingRequestScreen(Modifier.padding(padding))
        } else {
            val driver = listOf(detail.senderTrip, detail.targetTrip).first { it.role == TripRole.DRIVER }
            val currentUserIsDriver = driver.ownerId == state.currentUserId
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { RequestStatusChip(RequestStatus.ACCEPTED) }
                item { RequestTripSummary(detail) }
                item { ContactNoticeCard() }
                item {
                    RutaUInfoCard(
                        title = "Punto de encuentro",
                        message = "Acuerda el punto de encuentro directamente con la otra persona.",
                    )
                }
                state.error?.let { error ->
                    item { Text(error, color = MaterialTheme.colorScheme.error) }
                }
                item {
                    RutaUDestructiveButton(
                        text = if (currentUserIsDriver) "Cancelar viaje" else "Cancelar participación",
                        onClick = { confirmCancellation = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isSaving,
                    )
                }
            }
            if (confirmCancellation) {
                RutaUConfirmationDialog(
                    title = if (currentUserIsDriver) "Cancelar viaje" else "Cancelar participación",
                    message = "Esta acción liberará la plaza ocupada.",
                    confirmLabel = "Confirmar cancelación",
                    onConfirm = {
                        confirmCancellation = false
                        if (currentUserIsDriver) onCancelRide() else onCancelParticipation()
                    },
                    onDismiss = { confirmCancellation = false },
                    isDestructive = true,
                )
            }
        }
    }
}

/** Muestra la persona con quien se quiere coordinar. */
@Composable
private fun RequestPersonCard(detail: RideRequestDetails, isReceived: Boolean) {
    val person = if (isReceived) detail.sender else detail.target
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(if (isReceived) "Solicitud de" else "Invitación para", style = MaterialTheme.typography.labelLarge)
        Text(person.fullName, style = MaterialTheme.typography.titleLarge)
        Text("${person.university} · ${person.campus}")
    }
}

/** Muestra los botones que aplican al estado actual de la solicitud. */
@Composable
private fun RequestActions(
    detail: RideRequestDetails,
    isReceived: Boolean,
    hasCoordination: Boolean,
    isSaving: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onCancel: () -> Unit,
    onOpenCoordinatedRide: () -> Unit,
    onBack: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (detail.request.status) {
            RequestStatus.PENDING -> if (isReceived) {
                RutaUPrimaryButton("Aceptar solicitud", onAccept, Modifier.fillMaxWidth(), enabled = !isSaving)
                RutaUDestructiveButton("Rechazar", onReject, Modifier.fillMaxWidth(), enabled = !isSaving)
            } else {
                RutaUDestructiveButton("Cancelar solicitud", onCancel, Modifier.fillMaxWidth(), enabled = !isSaving)
            }
            RequestStatus.ACCEPTED -> if (hasCoordination) {
                RutaUPrimaryButton("Ver viaje coordinado", onOpenCoordinatedRide, Modifier.fillMaxWidth())
            }
            RequestStatus.REJECTED -> Text("Esta solicitud no fue aceptada.")
            RequestStatus.CANCELLED -> Text("Esta solicitud fue cancelada.")
            RequestStatus.EXPIRED -> Text("El tiempo para responder esta solicitud terminó.")
        }
        RutaUSecondaryButton("Volver a solicitudes", onBack, Modifier.fillMaxWidth())
    }
}

/** Muestra un indicador mientras llegan los datos de la pantalla. */
@Composable
private fun LoadingRequestScreen(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}

/** Guarda la acción que la persona confirma antes de cambiar una solicitud. */
private enum class RequestAction(
    val title: String,
    val message: String,
    val confirmLabel: String,
) {
    ACCEPT("Aceptar solicitud", "Se reservará una plaza para esta persona.", "Aceptar"),
    REJECT("Rechazar solicitud", "La otra persona verá que la solicitud fue rechazada.", "Rechazar"),
    CANCEL("Cancelar solicitud", "La solicitud dejará de estar disponible.", "Cancelar solicitud"),
}

/** Convierte una cantidad de centavos a quetzales para mostrarla en pantalla. */
private fun Long.asQuetzales(): String = "Q %.2f".format(Locale.US, this / 100.0)

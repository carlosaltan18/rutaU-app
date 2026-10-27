package uvg.edu.rutau.feature.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter
import java.util.Locale
import uvg.edu.rutau.core.designsystem.component.RutaUAvatar
import uvg.edu.rutau.core.designsystem.component.RutaUInfoCard
import uvg.edu.rutau.core.designsystem.component.RutaUOutlinedButton
import uvg.edu.rutau.core.designsystem.component.RutaUPrimaryButton
import uvg.edu.rutau.core.designsystem.component.RutaUStatusChip
import uvg.edu.rutau.core.designsystem.component.RutaUStatusType
import uvg.edu.rutau.core.designsystem.component.RutaUTextField
import uvg.edu.rutau.core.model.RequestStatus
import uvg.edu.rutau.core.model.RequestType
import uvg.edu.rutau.core.model.RideRequestDetails
import uvg.edu.rutau.core.model.Trip
import uvg.edu.rutau.core.model.TripRole

/** Da formato simple a una hora para mostrarla en pantalla. */
internal fun Trip.asRequestSchedule(): String = "$dayOfWeek · ${departureTime.format(requestTimeFormatter).lowercase()}"

/** Da formato simple a una fecha para mostrarla en pantalla. */
internal fun RideRequestDetails.asRequestDate(): String = request.rideDate.format(requestDateFormatter)

/** Muestra un selector entre solicitudes recibidas y enviadas. */
@Composable
fun RequestTabSelector(
    selected: RequestTab,
    receivedCount: Int,
    sentCount: Int,
    onSelected: (RequestTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RequestTab.entries.forEach { tab ->
            FilterChip(
                selected = selected == tab,
                onClick = { onSelected(tab) },
                label = {
                    val count = if (tab == RequestTab.RECEIVED) receivedCount else sentCount
                    Text("${if (tab == RequestTab.RECEIVED) "Recibidas" else "Enviadas"} ($count)")
                },
                modifier = Modifier.weight(1f).testTag("RequestsTab-${tab.name}"),
            )
        }
    }
}

/** Muestra un selector para filtrar las solicitudes de la lista. */
@Composable
fun RequestFilterSelector(
    selected: RequestFilter,
    onSelected: (RequestFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RequestFilter.entries.forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelected(filter) },
                label = {
                    Text(
                        when (filter) {
                            RequestFilter.ALL -> "Todas"
                            RequestFilter.PENDING -> "Pendientes"
                            RequestFilter.ACCEPTED -> "Aceptadas"
                        },
                    )
                },
                modifier = Modifier.testTag("RequestsFilter-${filter.name}"),
            )
        }
    }
}

/** Muestra una solicitud resumida dentro de la lista. */
@Composable
fun RequestCard(
    detail: RideRequestDetails,
    isReceived: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val otherStudent = if (isReceived) detail.sender else detail.target
    val otherTrip = if (isReceived) detail.senderTrip else detail.targetTrip
    Card(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth().testTag("RequestCard-${detail.request.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                RequestStatusChip(detail.request.status)
                RequestTypeLabel(detail.request.type)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                RutaUAvatar(otherStudent.fullName, otherStudent.photoUrl, size = 44.dp)
                Column(Modifier.weight(1f)) {
                    Text(otherStudent.fullName, style = MaterialTheme.typography.titleMedium)
                    Text("${otherStudent.university} · ${otherStudent.campus}")
                }
            }
            Text("${otherTrip.originZone} → ${otherTrip.destinationCampus}", fontWeight = FontWeight.SemiBold)
            Text("${detail.asRequestDate()} · ${otherTrip.departureTime.format(requestTimeFormatter).lowercase()}")
            if (otherTrip.role == TripRole.DRIVER) {
                Text("${otherTrip.availableSeats} plazas disponibles")
            }
            Text("Contribución sugerida: ${detail.request.contributionCents.asQuetzales()}")
            Text("Ver detalle", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Muestra si el elemento es una solicitud o una invitación. */
@Composable
fun RequestTypeLabel(type: RequestType, modifier: Modifier = Modifier) {
    Text(
        text = if (type == RequestType.JOIN_REQUEST) "Solicitud" else "Invitación",
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
    )
}

/** Muestra el estado de una solicitud con un color fácil de reconocer. */
@Composable
fun RequestStatusChip(status: RequestStatus, modifier: Modifier = Modifier) {
    val (label, type) = when (status) {
        RequestStatus.PENDING -> "Pendiente" to RutaUStatusType.WARNING
        RequestStatus.ACCEPTED -> "Aceptada" to RutaUStatusType.SUCCESS
        RequestStatus.REJECTED -> "Rechazada" to RutaUStatusType.ERROR
        RequestStatus.CANCELLED -> "Cancelada" to RutaUStatusType.NEUTRAL
        RequestStatus.EXPIRED -> "Vencida" to RutaUStatusType.NEUTRAL
    }
    RutaUStatusChip(label, type, modifier)
}

/** Muestra los datos del trayecto relacionados con una solicitud. */
@Composable
fun RequestTripSummary(detail: RideRequestDetails, modifier: Modifier = Modifier) {
    val driver = listOf(detail.senderTrip, detail.targetTrip).firstOrNull { it.role == TripRole.DRIVER }
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Resumen del trayecto", style = MaterialTheme.typography.titleSmall)
            Text("${detail.senderTrip.originZone} → ${detail.senderTrip.destinationCampus}", fontWeight = FontWeight.SemiBold)
            Text(detail.asRequestDate())
            Text("Salida: ${detail.senderTrip.asRequestSchedule()}")
            if (detail.senderTrip.departureTime != detail.targetTrip.departureTime) {
                Text("La diferencia de horario es ${detail.timeDifferenceMinutes()} minutos.")
            } else {
                Text("Ambos trayectos tienen la misma hora.")
            }
            driver?.let { Text("${it.availableSeats} plazas disponibles") }
        }
    }
}

/** Muestra la información que se comparte después de aceptar un viaje. */
@Composable
fun ContactNoticeCard(modifier: Modifier = Modifier) {
    RutaUInfoCard(
        title = "Contacto autorizado",
        message = "Al aceptar el viaje pueden comunicarse para acordar el punto de encuentro.",
        modifier = modifier,
        icon = Icons.Default.CheckCircle,
    )
}

/** Muestra el contacto de una persona cuando el viaje ya fue aceptado. */
@Composable
fun AuthorizedContactCard(
    fullName: String,
    phone: String?,
    modifier: Modifier = Modifier,
) {
    RutaUInfoCard(
        title = "Contacto autorizado",
        message = if (phone == null) {
            "${fullName} no agregó un teléfono. Pueden acordar el punto de encuentro en persona."
        } else {
            "$fullName\nTeléfono y WhatsApp: $phone"
        },
        modifier = modifier,
        icon = Icons.Default.CheckCircle,
    )
}

/** Muestra acciones para comunicarse después de aceptar un viaje. */
@Composable
fun ExternalContactActions(
    phone: String?,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (phone == null) return
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RutaUOutlinedButton("Llamar", onCall, Modifier.weight(1f))
        RutaUPrimaryButton("WhatsApp", onWhatsApp, Modifier.weight(1f))
    }
}

/** Permite proponer y confirmar el punto donde se reunirán antes del viaje. */
@Composable
fun MeetingPointCard(
    meetingPoint: String?,
    isConfirmedByCurrentUser: Boolean,
    isConfirmedByOtherParticipant: Boolean,
    isSaving: Boolean,
    onSave: (String) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editedPoint by remember(meetingPoint) { mutableStateOf(meetingPoint.orEmpty()) }
    Card(modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Punto de encuentro", style = MaterialTheme.typography.titleSmall)
            Text(
                "Escribe un lugar fácil de identificar. Si lo cambias, ambas confirmaciones se reinician.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            RutaUTextField(
                value = editedPoint,
                onValueChange = { editedPoint = it },
                label = "Lugar para reunirse",
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
            )
            RutaUOutlinedButton(
                text = if (meetingPoint.isNullOrBlank()) "Guardar punto" else "Actualizar punto",
                onClick = { onSave(editedPoint) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving && editedPoint.trim().length >= 3 && editedPoint.trim() != meetingPoint,
            )
            if (meetingPoint.isNullOrBlank()) {
                Text(
                    "Aún no han definido dónde se reunirán.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Text(
                    if (isConfirmedByCurrentUser) {
                        "Ya confirmaste este punto."
                    } else {
                        "Confirma cuando estés de acuerdo con este punto."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    if (isConfirmedByOtherParticipant) {
                        "La otra persona ya lo confirmó."
                    } else {
                        "La otra persona aún debe confirmarlo."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (!isConfirmedByCurrentUser) {
                    RutaUPrimaryButton(
                        text = "Confirmar punto de encuentro",
                        onClick = onConfirm,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving,
                    )
                }
            }
        }
    }
}

/** Muestra el cambio de pendiente a viaje aceptado. */
@Composable
fun CoordinationTimeline(modifier: Modifier = Modifier) {
    RutaUInfoCard(
        title = "Viaje confirmado",
        message = "La solicitud fue aceptada y la plaza quedó confirmada.",
        modifier = modifier,
        icon = Icons.Default.CheckCircle,
    )
}

/** Muestra la ocupación actual de un vehículo. */
@Composable
fun VehicleCapacityCard(driver: Trip, modifier: Modifier = Modifier) {
    val isFull = driver.occupiedSeats == driver.offeredSeats
    RutaUInfoCard(
        title = if (isFull) "Viaje completo" else "Capacidad del viaje",
        message = buildString {
            append("${driver.occupiedSeats} de ${driver.offeredSeats} plazas ocupadas\n")
            append("${driver.occupiedSeats + 1} ocupantes totales")
            if (isFull) append("\nNo se permiten nuevas aceptaciones.")
            else append("\n${driver.availableSeats} plazas disponibles")
        },
        modifier = modifier,
        icon = if (isFull) Icons.Default.CheckCircle else Icons.Default.Info,
    )
}

/** Muestra las plazas confirmadas que el servidor informa para este viaje. */
@Composable
fun CoordinationCapacityCard(
    confirmedPassengers: Int,
    totalSeats: Int,
    availableSeats: Int,
    fallbackConfirmedPassengers: Int,
    modifier: Modifier = Modifier,
) {
    val confirmed = if (totalSeats > 0) confirmedPassengers else fallbackConfirmedPassengers
    val message = if (totalSeats > 0) {
        "$confirmed de $totalSeats plazas confirmadas\n$availableSeats plazas disponibles"
    } else {
        "$confirmed plaza${if (confirmed == 1) "" else "s"} confirmada${if (confirmed == 1) "" else "s"}"
    }
    RutaUInfoCard(
        title = "Capacidad del viaje",
        message = message,
        modifier = modifier,
        icon = if (availableSeats == 0 && totalSeats > 0) Icons.Default.CheckCircle else Icons.Default.Info,
    )
}

/** Muestra una persona que ya tiene una plaza confirmada. */
@Composable
fun ConfirmedPassengerCard(detail: RideRequestDetails, modifier: Modifier = Modifier) {
    val passenger = if (detail.senderTrip.role == TripRole.PASSENGER) detail.sender else detail.target
    Card(modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RutaUAvatar(passenger.fullName, passenger.photoUrl, size = 40.dp)
            Column(Modifier.weight(1f)) {
                Text(passenger.fullName, style = MaterialTheme.typography.titleSmall)
                Text("Contribución: ${detail.request.contributionCents.asQuetzales()}")
                passenger.phone?.let { Text("Teléfono y WhatsApp: $it") }
            }
            Text("Confirmada", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Muestra un aviso cuando no hay solicitudes para el filtro elegido. */
@Composable
fun EmptyRequestsCard(modifier: Modifier = Modifier) {
    RutaUInfoCard(
        title = "No hay solicitudes aquí",
        message = "Cuando recibas o envíes una, aparecerá en esta lista.",
        modifier = modifier,
        icon = Icons.Default.Info,
    )
}

/** Explica qué ocurrió con una solicitud que ya terminó y cuál fue su efecto. */
@Composable
fun TerminalRequestCard(
    detail: RideRequestDetails,
    isReceived: Boolean,
    modifier: Modifier = Modifier,
) {
    val status = detail.request.status
    val message = when (status) {
        RequestStatus.REJECTED -> if (isReceived) {
            "Rechazaste esta solicitud. No se confirmó ninguna plaza ni se compartieron datos de contacto."
        } else {
            "La otra persona rechazó esta solicitud. No se confirmó ninguna plaza ni se compartieron datos de contacto."
        }
        RequestStatus.CANCELLED -> "Esta solicitud fue cancelada y no ocupa ninguna plaza."
        RequestStatus.EXPIRED ->
            "El viaje estaba previsto para ${detail.asRequestDate()} y venció sin ser aceptado. " +
                "No se confirmó ninguna plaza ni se compartieron datos de contacto."
        else -> return
    }
    RutaUInfoCard(
        title = "Solicitud ${status.asUiStatus().lowercase()}",
        message = message,
        modifier = modifier,
        icon = Icons.Default.Info,
    )
}

/** Calcula la diferencia simple de horario entre dos trayectos. */
private fun RideRequestDetails.timeDifferenceMinutes(): Long = kotlin.math.abs(
    java.time.Duration.between(senderTrip.departureTime, targetTrip.departureTime).toMinutes(),
)

private val requestDateFormatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es-GT"))
private val requestTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.forLanguageTag("es-GT"))

/** Convierte una contribución a texto con quetzales. */
internal fun Long.asQuetzales(): String = "Q %.2f".format(Locale.US, this / 100.0)

/** Convierte el estado de una solicitud en un texto corto. */
private fun RequestStatus.asUiStatus(): String = when (this) {
    RequestStatus.PENDING -> "Pendiente"
    RequestStatus.ACCEPTED -> "Aceptada"
    RequestStatus.REJECTED -> "Rechazada"
    RequestStatus.CANCELLED -> "Cancelada"
    RequestStatus.EXPIRED -> "Vencida"
}

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter
import java.util.Locale
import uvg.edu.rutau.core.designsystem.component.RutaUAvatar
import uvg.edu.rutau.core.designsystem.component.RutaUInfoCard
import uvg.edu.rutau.core.designsystem.component.RutaUStatusChip
import uvg.edu.rutau.core.designsystem.component.RutaUStatusType
import uvg.edu.rutau.core.model.RequestStatus
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
    onSelected: (RequestTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RequestTab.entries.forEach { tab ->
            FilterChip(
                selected = selected == tab,
                onClick = { onSelected(tab) },
                label = { Text(if (tab == RequestTab.RECEIVED) "Recibidas" else "Enviadas") },
                modifier = Modifier.weight(1f),
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
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                RequestStatusChip(detail.request.status)
                Text(
                    if (detail.request.type.name == "JOIN_REQUEST") "Solicitud" else "Invitación",
                    style = MaterialTheme.typography.labelMedium,
                )
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
            Text("Ver detalle", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }
    }
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

/** Calcula la diferencia simple de horario entre dos trayectos. */
private fun RideRequestDetails.timeDifferenceMinutes(): Long = kotlin.math.abs(
    java.time.Duration.between(senderTrip.departureTime, targetTrip.departureTime).toMinutes(),
)

private val requestDateFormatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es-GT"))
private val requestTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.forLanguageTag("es-GT"))

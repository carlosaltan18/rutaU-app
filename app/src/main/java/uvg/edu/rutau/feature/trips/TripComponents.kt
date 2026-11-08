package uvg.edu.rutau.feature.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.util.Locale
import uvg.edu.rutau.core.designsystem.component.RutaUDropdownField
import uvg.edu.rutau.core.designsystem.component.RutaUAvatar
import uvg.edu.rutau.core.designsystem.component.RutaUInfoCard
import uvg.edu.rutau.core.designsystem.component.RutaUOutlinedButton
import uvg.edu.rutau.core.designsystem.component.RutaUPrimaryButton
import uvg.edu.rutau.core.designsystem.component.RutaUSecondaryButton
import uvg.edu.rutau.core.data.catalog.CatalogItem
import uvg.edu.rutau.core.model.Student
import uvg.edu.rutau.core.model.Trip
import uvg.edu.rutau.core.model.TripMatch
import uvg.edu.rutau.core.model.TripRole
import uvg.edu.rutau.core.model.CoordinationStatus
import uvg.edu.rutau.core.model.FavoriteTrip
import uvg.edu.rutau.core.designsystem.component.RutaUStatusChip
import uvg.edu.rutau.core.designsystem.component.RutaUStatusType

private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.forLanguageTag("es-GT"))
private val coordinatedTripDateFormatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es-GT"))
internal fun LocalTime.asUiTime(): String = format(timeFormatter).lowercase()
internal fun TripRole.asUiRole(): String = if (this == TripRole.DRIVER) "Conductor" else "Pasajero"

@Composable
fun TripCard(
    trip: Trip,
    onEdit: () -> Unit,
    onMatches: () -> Unit,
    onDeactivate: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showActions by remember { mutableStateOf(false) }
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${trip.originZone}  →  ${trip.destinationCampus}", style = MaterialTheme.typography.titleMedium)
                    Text("${trip.dayOfWeek} · ${trip.departureTime.asUiTime()}")
                }
                Text(
                    trip.role.asUiRole(),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            if (trip.role == TripRole.DRIVER) SeatAvailabilityIndicator(trip)
            if (!trip.active) Text("Trayecto inactivo", color = MaterialTheme.colorScheme.error)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RutaUPrimaryButton("Ver compatibles", onMatches, Modifier.weight(1f), enabled = trip.active)
                Box {
                    IconButton(onClick = { showActions = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Más acciones del trayecto")
                    }
                    DropdownMenu(
                        expanded = showActions,
                        onDismissRequest = { showActions = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Editar trayecto") },
                            onClick = {
                                showActions = false
                                onEdit()
                            },
                        )
                        if (trip.active) {
                            DropdownMenuItem(
                                text = { Text("Desactivar trayecto") },
                                onClick = {
                                    showActions = false
                                    onDeactivate()
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Eliminar trayecto", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showActions = false
                                onRemove()
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Muestra un viaje aceptado para abrir sus detalles sin pasar por solicitudes. */
@Composable
fun CoordinatedTripCard(
    detail: CoordinatedTripSummary,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isActive = detail.status == CoordinationStatus.CONFIRMED
    Card(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoordinatedTripStatus(detail.status)
                Text(detail.rideDate.format(coordinatedTripDateFormatter), style = MaterialTheme.typography.labelMedium)
            }
            Text(
                "${detail.driverTrip.originZone} → ${detail.driverTrip.destinationCampus}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text("${detail.driverTrip.dayOfWeek} · ${detail.driverTrip.departureTime.asUiTime()}")
            Text("Conductor: ${detail.driver.fullName}", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Pasajeros: ${detail.passengers.joinToString { it.fullName }}",
                style = MaterialTheme.typography.bodyMedium,
            )
            RutaUPrimaryButton(
                text = if (isActive) "Abrir viaje" else "Ver resumen",
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Tarjeta de acceso local a un perfil compatible guardado. */
@Composable
fun FavoriteTripCard(
    favorite: FavoriteTrip,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RutaUAvatar(fullName = favorite.studentName, photoUrl = favorite.studentPhotoUrl)
                Column(Modifier.weight(1f)) {
                    Text(favorite.studentName, style = MaterialTheme.typography.titleMedium)
                    Text(favorite.routeDescription, style = MaterialTheme.typography.bodyMedium)
                    Text(favorite.scheduleDescription, style = MaterialTheme.typography.bodySmall)
                }
            }
            RutaUOutlinedButton(
                text = "Quitar de guardados",
                onClick = onRemove,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Muestra un texto claro para saber si un viaje sigue activo o ya terminó. */
@Composable
private fun CoordinatedTripStatus(status: CoordinationStatus) {
    val (text, type) = when (status) {
        CoordinationStatus.CONFIRMED -> "Viaje confirmado" to RutaUStatusType.SUCCESS
        CoordinationStatus.CANCELLED -> "Viaje cancelado" to RutaUStatusType.NEUTRAL
        CoordinationStatus.COMPLETED -> "Viaje finalizado" to RutaUStatusType.SUCCESS
    }
    RutaUStatusChip(text, type)
}

@Composable
fun TripSummaryCard(trip: Trip, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${trip.originZone} → ${trip.destinationCampus}", style = MaterialTheme.typography.titleSmall)
            Text("${trip.dayOfWeek} · ${trip.departureTime.asUiTime()} · ${trip.role.asUiRole()}")
            if (trip.role == TripRole.DRIVER) Text("${trip.availableSeats} plazas disponibles")
        }
    }
}

@Composable
fun TripEditorForm(
    state: TripEditorUiState,
    onOriginChange: (String) -> Unit,
    onCampusChange: (String) -> Unit,
    onDayChange: (String) -> Unit,
    onTimeChange: (LocalTime) -> Unit,
    onRoleChange: (TripRole) -> Unit,
    onSeatsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TripRoleSelector(state.role, onRoleChange)
        OriginZoneSelector(state.originZone, state.zones, onOriginChange)
        CampusSelector(state.destinationCampus, state.campuses, onCampusChange)
        DaySelector(state.dayOfWeek, onDayChange)
        TimeSelector(state.departureTime, onTimeChange)
        if (state.role == TripRole.DRIVER) PassengerSeatSelector(state.offeredSeats, onSeatsChange)
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
fun TripRoleSelector(selected: TripRole, onSelected: (TripRole) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("¿Cómo harás este trayecto?", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TripRole.entries.forEach { role ->
                FilterChip(selected == role, { onSelected(role) }, { Text(role.asUiRole()) })
            }
        }
    }
}

@Composable
fun PassengerSeatSelector(selected: Int, onSelected: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Plazas que publicas", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..3).forEach { seats -> FilterChip(selected == seats, { onSelected(seats) }, { Text("$seats") }) }
        }
        Text("Puedes coordinar con un máximo de 3 pasajeros.", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun SeatAvailabilityIndicator(trip: Trip) {
    Text("${trip.occupiedSeats} ocupadas · ${trip.availableSeats} disponibles", style = MaterialTheme.typography.bodyMedium)
}

@Composable
fun OriginZoneSelector(selected: String, options: List<CatalogItem>, onSelected: (String) -> Unit) =
    RutaUDropdownField(
        options.firstOrNull { it.id == selected }?.name,
        options.map(CatalogItem::name),
        { name -> options.firstOrNull { it.name == name }?.let { onSelected(it.id) } },
        "Zona de origen",
        Modifier.fillMaxWidth(),
    )

@Composable
fun CampusSelector(selected: String, options: List<CatalogItem>, onSelected: (String) -> Unit) =
    RutaUDropdownField(
        options.firstOrNull { it.id == selected }?.name,
        options.map(CatalogItem::name),
        { name -> options.firstOrNull { it.name == name }?.let { onSelected(it.id) } },
        "Campus de destino",
        Modifier.fillMaxWidth(),
    )

@Composable
fun DaySelector(selected: String, onSelected: (String) -> Unit) =
    RutaUDropdownField(selected, listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"), onSelected, "Día", Modifier.fillMaxWidth())

@Composable
fun TimeSelector(selected: LocalTime, onSelected: (LocalTime) -> Unit) {
    val options = listOf(6, 7, 8, 12, 13, 17, 18).flatMap { hour -> listOf(LocalTime.of(hour, 0), LocalTime.of(hour, 30)) }
    RutaUDropdownField(
        selected.asUiTime(),
        options.map(LocalTime::asUiTime),
        { label -> options.firstOrNull { it.asUiTime() == label }?.let(onSelected) },
        "Hora de salida",
        Modifier.fillMaxWidth(),
    )
}

@Composable
fun MatchCard(match: TripMatch, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onOpen, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RutaUAvatar(
                fullName = match.student.fullName,
                photoUrl = match.student.photoUrl,
                size = 48.dp,
            )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(match.student.fullName, style = MaterialTheme.typography.titleMedium)
                Text("${match.student.university} · ${match.student.campus}")
                }
            }
            Text("${match.trip.role.asUiRole()} · ${match.trip.departureTime.asUiTime()}")
            if (match.trip.role == TripRole.DRIVER) {
                Text("${match.trip.availableSeats} plazas disponibles", color = MaterialTheme.colorScheme.primary)
            }
            Text(
                if (match.timeDifferenceMinutes == 0) "Misma hora" else {
                    "${match.timeDifferenceMinutes} minutos de diferencia"
                },
            )
            Text(
                "✓ Misma zona · ✓ Mismo campus · ✓ Mismo día",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            RutaUOutlinedButton("Ver perfil", onOpen, Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun CompatibilitySummaryCard(count: Int, modifier: Modifier = Modifier) {
    RutaUInfoCard("$count ${if (count == 1) "persona compatible" else "personas compatibles"}", "Comparamos zona, campus, día, hora, rol y plazas disponibles.", modifier, Icons.Default.Info)
}

@Composable
fun CompatibilityRuleItem(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.tertiary)
        Text(text)
    }
}

@Composable
fun CandidateProfileHeader(student: Student, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RutaUAvatar(student.fullName, student.photoUrl, size = 80.dp)
        Text(student.fullName, style = MaterialTheme.typography.titleLarge)
        Text("${student.university} · ${student.campus}", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun CandidateTripCard(trip: Trip, modifier: Modifier = Modifier) = TripSummaryCard(trip, modifier)

@Composable
fun CompatibilityReasonsCard(
    sourceTrip: Trip,
    candidateTrip: Trip,
    modifier: Modifier = Modifier,
) {
    val difference = kotlin.math.abs(
        Duration.between(sourceTrip.departureTime, candidateTrip.departureTime).toMinutes(),
    )
    RutaUInfoCard(
        title = "Compatibilidad comprobada",
        message = buildString {
            append("✓ Misma zona de origen\n")
            append("✓ Mismo campus de destino\n")
            append("✓ Mismo día\n")
            append("✓ ${if (difference == 0L) "Misma hora" else "$difference minutos de diferencia"}\n")
            append("✓ Roles complementarios")
            if (candidateTrip.role == TripRole.DRIVER) {
                append("\n✓ ${candidateTrip.availableSeats} plazas disponibles")
            }
        },
        modifier = modifier,
        icon = Icons.Default.CheckCircle,
    )
}

@Composable
fun ContributionEditor(value: Int, onValueChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Contribución sugerida", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(0, 5, 10, 15, 20).forEach { amount ->
                FilterChip(
                    selected = value == amount,
                    onClick = { onValueChange(amount) },
                    label = { Text(if (amount == 0) "Sin aporte" else "Q$amount", maxLines = 1) },
                )
            }
        }
    }
}

@Composable
fun PrivacyNoticeCard(modifier: Modifier = Modifier) {
    RutaUInfoCard("Tu privacidad importa", "Solo mostramos información necesaria para decidir si desean coordinar. Los datos de contacto no son públicos.", modifier, Icons.Default.Info)
}

@Composable
fun CompatibilityInfoSheet(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Cómo calculamos la compatibilidad", style = MaterialTheme.typography.titleLarge)
        Text("Comparamos trayectos universitarios registrados sin usar mapas, tráfico ni ubicación en tiempo real.")
        CompatibilityRuleItem("Misma zona de origen")
        CompatibilityRuleItem("Mismo campus de destino")
        CompatibilityRuleItem("Mismo día")
        CompatibilityRuleItem("Máximo 30 minutos de diferencia")
        CompatibilityRuleItem("Roles complementarios")
        CompatibilityRuleItem("Conductor con plazas disponibles")
        Spacer(Modifier.height(4.dp))
        RutaUPrimaryButton("Entendido", onDismiss, Modifier.fillMaxWidth())
    }
}

@Composable
fun ConfirmCoordinationSheet(
    state: ConfirmCoordinationUiState,
    onMessageChange: (String) -> Unit,
    onContributionChange: (Int) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(if (state.sourceTrip?.role == TripRole.PASSENGER) "Confirmar solicitud" else "Confirmar invitación", style = MaterialTheme.typography.titleLarge)
            state.student?.let { Text("Coordinar con ${it.fullName}") }
            state.sourceTrip?.let { source ->
                Text("${source.originZone} → ${source.destinationCampus}", style = MaterialTheme.typography.titleSmall)
                Text("${source.dayOfWeek} · ${source.departureTime.asUiTime()}")
            }
            state.candidateTrip?.let { candidate ->
                val difference = state.sourceTrip?.let { source ->
                    kotlin.math.abs(Duration.between(source.departureTime, candidate.departureTime).toMinutes())
                }
                if (difference != null) {
                    Text(
                        if (difference == 0L) "Misma hora" else "$difference minutos de diferencia",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            listOfNotNull(state.sourceTrip, state.candidateTrip)
                .firstOrNull { it.role == TripRole.DRIVER }
                ?.let { driver -> SeatAvailabilityIndicator(driver) }
            HorizontalDivider()
            ContributionEditor(state.contributionQuetzales, onContributionChange)
            OutlinedTextField(
                value = state.message,
                onValueChange = { onMessageChange(it.take(MaxOptionalMessageLength)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Mensaje opcional") },
                supportingText = {
                    Text(
                        "Máximo $MaxOptionalMessageLength caracteres · " +
                            "${state.message.length}/$MaxOptionalMessageLength",
                    )
                },
                minLines = 3,
            )
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            RutaUPrimaryButton(
                if (state.sourceTrip?.role == TripRole.PASSENGER) "Enviar solicitud" else "Enviar invitación",
                onConfirm,
                Modifier.fillMaxWidth(),
                enabled = !state.isSubmitting && state.candidateTrip != null,
            )
            Text("Las plazas solo se actualizan cuando la coordinación sea aceptada.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private const val MaxOptionalMessageLength = 160

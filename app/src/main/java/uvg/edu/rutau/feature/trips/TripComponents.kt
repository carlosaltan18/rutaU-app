package uvg.edu.rutau.feature.trips

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import uvg.edu.rutau.core.designsystem.component.RutaUDropdownField
import uvg.edu.rutau.core.designsystem.component.RutaUInfoCard
import uvg.edu.rutau.core.designsystem.component.RutaUOutlinedButton
import uvg.edu.rutau.core.designsystem.component.RutaUPrimaryButton
import uvg.edu.rutau.core.designsystem.component.RutaUSecondaryButton
import uvg.edu.rutau.core.model.Student
import uvg.edu.rutau.core.model.Trip
import uvg.edu.rutau.core.model.TripMatch
import uvg.edu.rutau.core.model.TripRole

private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.forLanguageTag("es-GT"))
internal fun LocalTime.asUiTime(): String = format(timeFormatter).lowercase()
internal fun TripRole.asUiRole(): String = if (this == TripRole.DRIVER) "Conductor" else "Pasajero"

@Composable
fun TripCard(
    trip: Trip,
    onEdit: () -> Unit,
    onMatches: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("${trip.originZone}  →  ${trip.destinationCampus}", style = MaterialTheme.typography.titleMedium)
                    Text("${trip.dayOfWeek} · ${trip.departureTime.asUiTime()}")
                }
                Text(trip.role.asUiRole(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
            if (trip.role == TripRole.DRIVER) SeatAvailabilityIndicator(trip)
            if (!trip.active) Text("Trayecto inactivo", color = MaterialTheme.colorScheme.error)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RutaUPrimaryButton("Ver compatibles", onMatches, Modifier.weight(1f), enabled = trip.active)
                RutaUOutlinedButton("Editar", onEdit)
            }
            RutaUSecondaryButton("Eliminar", onRemove, Modifier.fillMaxWidth())
        }
    }
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
        OriginZoneSelector(state.originZone, onOriginChange)
        CampusSelector(state.destinationCampus, onCampusChange)
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
fun OriginZoneSelector(selected: String, onSelected: (String) -> Unit) =
    RutaUDropdownField(selected, listOf("Zona 1", "Zona 7", "Zona 10", "Zona 11", "Zona 12", "Mixco"), onSelected, "Zona de origen", Modifier.fillMaxWidth())

@Composable
fun CampusSelector(selected: String, onSelected: (String) -> Unit) =
    RutaUDropdownField(selected, listOf("Campus Central", "Campus Sur", "Campus Norte"), onSelected, "Campus de destino", Modifier.fillMaxWidth())

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
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(match.student.fullName, style = MaterialTheme.typography.titleMedium)
            Text(match.trip.role.asUiRole())
            Text(match.trip.departureTime.asUiTime())
            Text(if (match.timeDifferenceMinutes == 0) "Misma hora" else "${match.timeDifferenceMinutes} minutos de diferencia")
            if (match.trip.role == TripRole.DRIVER) Text("${match.trip.availableSeats} plazas disponibles")
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
        Text(student.fullName, style = MaterialTheme.typography.titleLarge)
        Text("${student.university} · ${student.campus}", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun CandidateTripCard(trip: Trip, modifier: Modifier = Modifier) = TripSummaryCard(trip, modifier)

@Composable
fun ContributionEditor(value: Int, onValueChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Contribución sugerida", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0, 5, 10, 15, 20).forEach { amount ->
                FilterChip(value == amount, { onValueChange(amount) }, { Text(if (amount == 0) "Sin aporte" else "Q$amount") })
            }
        }
    }
}

@Composable
fun PrivacyNoticeCard(modifier: Modifier = Modifier) {
    RutaUInfoCard("Tu privacidad importa", "Solo mostramos información necesaria para decidir si desean coordinar. Los datos de contacto no son públicos.", modifier, Icons.Default.Info)
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
            HorizontalDivider()
            ContributionEditor(state.contributionQuetzales, onContributionChange)
            OutlinedTextField(state.message, onMessageChange, Modifier.fillMaxWidth(), label = { Text("Mensaje opcional") }, minLines = 3)
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

package uvg.edu.rutau.feature.trips

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import uvg.edu.rutau.core.data.mock.MockSeed
import uvg.edu.rutau.core.model.TripMatch
import uvg.edu.rutau.core.model.TripRole
import uvg.edu.rutau.ui.theme.RutaUTheme

private val passengerTrip = MockSeed.trips.first { it.id == "trip-mateo-passenger" }
private val driverTrip = MockSeed.trips.first { it.id == "trip-andrea-driver" }
private val driverStudent = MockSeed.students.first { it.id == driverTrip.ownerId }
private val driverMatch = TripMatch(driverTrip, driverStudent, timeDifferenceMinutes = 15)

@Preview(showBackground = true)
@Composable
private fun TripsScreenPreview() {
    RutaUTheme {
        TripsScreen(
            state = TripsUiState(MockSeed.trips.filter { it.ownerId == MockSeed.currentUser.id }),
            onCreateTrip = {},
            onEditTrip = {},
            onOpenMatches = {},
            onDeactivateTrip = {},
            onRemoveTrip = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TripEditorScreenPreview() {
    RutaUTheme {
        TripEditorScreen(
            state = TripEditorUiState(role = TripRole.DRIVER, offeredSeats = 3, isEditing = true),
            onOriginChange = {},
            onCampusChange = {},
            onDayChange = {},
            onTimeChange = {},
            onRoleChange = {},
            onSeatsChange = {},
            onSave = {},
            onDelete = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MatchesScreenPreview() {
    RutaUTheme {
        MatchesScreen(
            state = MatchesUiState(sourceTrip = passengerTrip, matches = listOf(driverMatch)),
            onBack = {},
            onOpenCandidate = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CandidateProfileScreenPreview() {
    RutaUTheme {
        CandidateProfileScreen(
            state = CandidateProfileUiState(
                sourceTrip = passengerTrip,
                candidateTrip = driverTrip,
                student = driverStudent,
            ),
            onBack = {},
            onCoordinate = {},
            onContributionChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfirmCoordinationScreenPreview() {
    RutaUTheme {
        ConfirmCoordinationScreen(
            state = ConfirmCoordinationUiState(
                sourceTrip = passengerTrip,
                candidateTrip = driverTrip,
                student = driverStudent,
                message = "Hola Andrea, ¿podemos coordinar?",
            ),
            onMessageChange = {},
            onContributionChange = {},
            onConfirm = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CompatibilityInfoSheetPreview() {
    RutaUTheme {
        CompatibilityInfoSheet(onDismiss = {})
    }
}

package uvg.edu.rutau.core.data.mock

import kotlinx.coroutines.flow.MutableStateFlow
import uvg.edu.rutau.core.model.Coordination
import uvg.edu.rutau.core.model.RideRequest
import uvg.edu.rutau.core.model.Student
import uvg.edu.rutau.core.model.Trip
import uvg.edu.rutau.core.model.UserAccount

/** In-memory source of truth used by the fake repositories in the frontend MVP. */
class MockRutaUStore {
    val currentUser = MutableStateFlow<UserAccount?>(MockSeed.currentUser)
    val students = MutableStateFlow<List<Student>>(MockSeed.students)
    val trips = MutableStateFlow<List<Trip>>(MockSeed.trips)
    val requests = MutableStateFlow<List<RideRequest>>(MockSeed.requests)
    val coordinations = MutableStateFlow<List<Coordination>>(emptyList())

    private var password: String = MockSeed.DefaultPassword

    fun credentialsMatch(email: String, candidatePassword: String): Boolean =
        currentUser.value?.let { it.email.equals(email.trim(), ignoreCase = true) } == true &&
            password == candidatePassword

    fun updatePassword(value: String) {
        password = value
    }

    fun passwordMatches(value: String): Boolean = password == value
}

package uvg.edu.rutau.app

import uvg.edu.rutau.core.data.mock.MockRutaUStore
import uvg.edu.rutau.core.data.repository.FakeSessionRepository
import uvg.edu.rutau.core.data.repository.FakeUserRepository
import uvg.edu.rutau.core.data.repository.FakeTripRepository
import uvg.edu.rutau.core.data.repository.FakeCoordinationRepository
import uvg.edu.rutau.core.data.repository.TripRepository
import uvg.edu.rutau.core.data.repository.CoordinationRepository
import uvg.edu.rutau.core.data.repository.SessionRepository
import uvg.edu.rutau.core.data.repository.UserRepository
import uvg.edu.rutau.core.model.UserAccount

/** Temporary manual dependency container for the local frontend MVP. */
object RutaUAppDependencies {
    private var store = MockRutaUStore()

    var sessionRepository: SessionRepository = FakeSessionRepository(store)
        private set
    var userRepository: UserRepository = FakeUserRepository(store)
        private set
    var tripRepository: TripRepository = FakeTripRepository(store)
        private set
    var coordinationRepository: CoordinationRepository = FakeCoordinationRepository(store)
        private set

    /** Recreates the in-memory dependencies for isolated instrumentation tests. */
    internal fun resetForTesting() {
        store = MockRutaUStore()
        sessionRepository = FakeSessionRepository(store)
        userRepository = FakeUserRepository(store)
        tripRepository = FakeTripRepository(store)
        coordinationRepository = FakeCoordinationRepository(store)
    }

    internal fun currentUserForTesting(): UserAccount? = store.currentUser.value
}

package uvg.edu.rutau.app

import uvg.edu.rutau.core.data.mock.MockRutaUStore
import uvg.edu.rutau.core.data.repository.FakeSessionRepository
import uvg.edu.rutau.core.data.repository.FakeUserRepository
import uvg.edu.rutau.core.data.repository.FakeTripRepository
import uvg.edu.rutau.core.data.repository.FakeCoordinationRepository
import uvg.edu.rutau.core.data.repository.FakeRideRequestRepository
import uvg.edu.rutau.core.data.repository.TripRepository
import uvg.edu.rutau.core.data.repository.CoordinationRepository
import uvg.edu.rutau.core.data.repository.RideRequestRepository
import uvg.edu.rutau.core.data.repository.SessionRepository
import uvg.edu.rutau.core.data.repository.UserRepository
import uvg.edu.rutau.core.model.UserAccount

/** Reúne los datos compartidos que usa la aplicación. */
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
    var rideRequestRepository: RideRequestRepository = FakeRideRequestRepository(store)
        private set

    /** Reinicia los datos de ejemplo para las pruebas. */
    internal fun resetForTesting() {
        store = MockRutaUStore()
        sessionRepository = FakeSessionRepository(store)
        userRepository = FakeUserRepository(store)
        tripRepository = FakeTripRepository(store)
        coordinationRepository = FakeCoordinationRepository(store)
        rideRequestRepository = FakeRideRequestRepository(store)
    }

    internal fun currentUserForTesting(): UserAccount? = store.currentUser.value
}

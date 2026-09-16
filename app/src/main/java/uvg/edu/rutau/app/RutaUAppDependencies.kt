package uvg.edu.rutau.app

import uvg.edu.rutau.core.data.mock.MockRutaUStore
import uvg.edu.rutau.core.data.repository.FakeSessionRepository
import uvg.edu.rutau.core.data.repository.FakeUserRepository
import uvg.edu.rutau.core.data.repository.SessionRepository
import uvg.edu.rutau.core.data.repository.UserRepository

/** Temporary manual dependency container for the local frontend MVP. */
object RutaUAppDependencies {
    private val store = MockRutaUStore()

    val sessionRepository: SessionRepository = FakeSessionRepository(store)
    val userRepository: UserRepository = FakeUserRepository(store)
}

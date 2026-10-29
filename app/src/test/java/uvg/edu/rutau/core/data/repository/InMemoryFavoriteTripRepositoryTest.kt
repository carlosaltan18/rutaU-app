package uvg.edu.rutau.core.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import uvg.edu.rutau.core.model.FavoriteTrip

class InMemoryFavoriteTripRepositoryTest {
    @Test
    fun `saving replacing and removing a favorite updates the observed list`() = runBlocking {
        val repository = InMemoryFavoriteTripRepository()
        val favorite = FavoriteTrip(
            tripId = "trip-1",
            studentName = "Andrea López",
            studentPhotoUrl = null,
            routeDescription = "Zona 10 → Campus Central",
            scheduleDescription = "Lunes · 7:00",
            savedAtMillis = 1L,
        )

        repository.save(favorite)
        repository.save(favorite.copy(studentName = "Andrea L."))

        assertEquals(listOf("Andrea L."), repository.observeFavorites().first().map(FavoriteTrip::studentName))
        assertEquals(setOf("trip-1"), repository.observeFavoriteTripIds().first())

        repository.remove("trip-1")

        assertEquals(emptyList<FavoriteTrip>(), repository.observeFavorites().first())
    }
}

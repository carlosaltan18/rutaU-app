package uvg.edu.rutau.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import uvg.edu.rutau.core.data.local.FavoriteTripDao
import uvg.edu.rutau.core.data.local.toEntity
import uvg.edu.rutau.core.data.local.toFavoriteTrip
import uvg.edu.rutau.core.model.FavoriteTrip

/** Es la única puerta a los perfiles compatibles guardados localmente. */
interface FavoriteTripRepository {
    fun observeFavorites(): Flow<List<FavoriteTrip>>
    fun observeFavoriteTripIds(): Flow<Set<String>>
    suspend fun save(favorite: FavoriteTrip)
    suspend fun remove(tripId: String)
}

/** Persiste favoritos con Room y devuelve solamente modelos de dominio. */
class RoomFavoriteTripRepository(
    private val favoriteTripDao: FavoriteTripDao,
) : FavoriteTripRepository {
    override fun observeFavorites(): Flow<List<FavoriteTrip>> =
        favoriteTripDao.observeAll().map { entities -> entities.map { it.toFavoriteTrip() } }

    override fun observeFavoriteTripIds(): Flow<Set<String>> =
        observeFavorites().map { favorites -> favorites.mapTo(mutableSetOf(), FavoriteTrip::tripId) }

    override suspend fun save(favorite: FavoriteTrip) {
        favoriteTripDao.upsert(favorite.toEntity())
    }

    override suspend fun remove(tripId: String) {
        favoriteTripDao.deleteByTripId(tripId)
    }
}

/** Sustituye Room en previews y pruebas que no inicializan la aplicación. */
class InMemoryFavoriteTripRepository : FavoriteTripRepository {
    private val favorites = MutableStateFlow<List<FavoriteTrip>>(emptyList())

    override fun observeFavorites(): Flow<List<FavoriteTrip>> = favorites

    override fun observeFavoriteTripIds(): Flow<Set<String>> =
        favorites.map { current -> current.mapTo(mutableSetOf(), FavoriteTrip::tripId) }

    override suspend fun save(favorite: FavoriteTrip) {
        favorites.value = listOf(favorite) + favorites.value.filterNot { it.tripId == favorite.tripId }
    }

    override suspend fun remove(tripId: String) {
        favorites.value = favorites.value.filterNot { it.tripId == tripId }
    }
}

package uvg.edu.rutau.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import uvg.edu.rutau.core.model.FavoriteTrip

/** Representa un perfil compatible guardado localmente. */
@Entity(tableName = "favorite_trips")
data class FavoriteTripEntity(
    @PrimaryKey val tripId: String,
    val studentName: String,
    val studentPhotoUrl: String?,
    val routeDescription: String,
    val scheduleDescription: String,
    val savedAtMillis: Long,
)

/** Convierte el formato local al modelo que consume la interfaz. */
fun FavoriteTripEntity.toFavoriteTrip(): FavoriteTrip = FavoriteTrip(
    tripId = tripId,
    studentName = studentName,
    studentPhotoUrl = studentPhotoUrl,
    routeDescription = routeDescription,
    scheduleDescription = scheduleDescription,
    savedAtMillis = savedAtMillis,
)

/** Convierte un favorito de dominio en una fila persistible. */
fun FavoriteTrip.toEntity(): FavoriteTripEntity = FavoriteTripEntity(
    tripId = tripId,
    studentName = studentName,
    studentPhotoUrl = studentPhotoUrl,
    routeDescription = routeDescription,
    scheduleDescription = scheduleDescription,
    savedAtMillis = savedAtMillis,
)

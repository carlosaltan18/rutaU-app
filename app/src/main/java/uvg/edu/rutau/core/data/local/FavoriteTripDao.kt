package uvg.edu.rutau.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Define las consultas locales de perfiles compatibles guardados. */
@Dao
interface FavoriteTripDao {
    @Query("SELECT * FROM favorite_trips ORDER BY savedAtMillis DESC")
    fun observeAll(): Flow<List<FavoriteTripEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(favorite: FavoriteTripEntity)

    @Query("DELETE FROM favorite_trips WHERE tripId = :tripId")
    suspend fun deleteByTripId(tripId: String)
}

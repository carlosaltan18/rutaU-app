package uvg.edu.rutau.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** Base local de RutaU para datos que deben sobrevivir sin conexión. */
@Database(
    entities = [FavoriteTripEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class RutaULocalDatabase : RoomDatabase() {
    abstract fun favoriteTripDao(): FavoriteTripDao

    companion object {
        fun create(context: Context): RutaULocalDatabase = Room.databaseBuilder(
            context.applicationContext,
            RutaULocalDatabase::class.java,
            "rutau_local.db",
        ).build()
    }
}

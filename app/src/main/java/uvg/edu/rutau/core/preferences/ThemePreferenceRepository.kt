package uvg.edu.rutau.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "rutau_theme")

/** Indica el tema visual que la persona eligió para RutaU. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/** Guarda localmente la preferencia visual sin almacenar datos sensibles. */
interface ThemePreferenceRepository {
    fun observeThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}

/** Conserva el modo claro, oscuro o del sistema usando DataStore. */
class DataStoreThemePreferenceRepository(
    context: Context,
) : ThemePreferenceRepository {
    private val dataStore = context.applicationContext.themeDataStore

    override fun observeThemeMode(): Flow<ThemeMode> = dataStore.data.map { preferences ->
        preferences[ThemeModeKey]
            ?.let { saved -> ThemeMode.entries.firstOrNull { it.name == saved } }
            ?: ThemeMode.SYSTEM
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences -> preferences[ThemeModeKey] = mode.name }
    }

    private companion object {
        val ThemeModeKey: Preferences.Key<String> = stringPreferencesKey("theme_mode")
    }
}

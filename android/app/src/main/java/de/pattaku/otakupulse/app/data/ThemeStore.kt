package de.pattaku.otakupulse.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import de.pattaku.otakupulse.app.ui.theme.ThemeModus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Hell, dunkel oder wie das System — die Wahl bleibt gespeichert. */
class ThemeStore(private val context: Context) {

    private val key = stringPreferencesKey("theme_modus")

    val modus: Flow<ThemeModus> = context.companionDataStore.data.map { prefs ->
        // Ein unbekannter Wert (etwa aus einer älteren Fassung) darf nicht abstürzen.
        runCatching { ThemeModus.valueOf(prefs[key] ?: ThemeModus.SYSTEM.name) }
            .getOrDefault(ThemeModus.SYSTEM)
    }

    suspend fun setze(neu: ThemeModus) {
        context.companionDataStore.edit { it[key] = neu.name }
    }
}

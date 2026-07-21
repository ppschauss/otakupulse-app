package de.pattaku.otakupulse.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import java.time.Instant

/** Merkt sich, bis wann schon nach neuen Folgen gesucht wurde. */
class AbgleichStore(private val context: Context) {

    private val key = stringPreferencesKey("last_episode_check")

    /** Beim ersten Lauf zwei Tage zurück — sonst käme sofort eine Flut alter Folgen. */
    suspend fun letzterFolgenAbgleich(): Instant {
        val gespeichert = context.companionDataStore.data.first()[key]
        return gespeichert?.let { runCatching { Instant.parse(it) }.getOrNull() }
            ?: Instant.now().minusSeconds(2 * 24 * 3600)
    }

    suspend fun setzeLetztenFolgenAbgleich(zeitpunkt: Instant) {
        context.companionDataStore.edit { it[key] = zeitpunkt.toString() }
    }
}

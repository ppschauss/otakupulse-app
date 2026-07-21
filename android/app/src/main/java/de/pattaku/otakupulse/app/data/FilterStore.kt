package de.pattaku.otakupulse.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import de.pattaku.otakupulse.app.domain.DeckFilter
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

/**
 * Merkt sich die Filtereinstellung über Neustarts hinweg.
 *
 * Wer sich einmal „nur Ger Dub, nur laufende Serien" eingestellt hat, will das beim
 * nächsten Öffnen nicht erneut zusammenklicken.
 */
class FilterStore(private val context: Context) {

    private val key = stringPreferencesKey("deck_filter")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun laden(): DeckFilter {
        val gespeichert = context.companionDataStore.data.first()[key] ?: return DeckFilter()
        // Ein kaputter Eintrag darf die App nicht am Start hindern.
        return runCatching { json.decodeFromString(DeckFilter.serializer(), gespeichert) }.getOrDefault(DeckFilter())
    }

    suspend fun speichern(filter: DeckFilter) {
        context.companionDataStore.edit { it[key] = json.encodeToString(DeckFilter.serializer(), filter) }
    }
}

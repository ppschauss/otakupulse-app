package de.pattaku.otakupulse.app.data

import de.pattaku.otakupulse.app.data.api.CompanionApi
import de.pattaku.otakupulse.app.data.local.AppDatabase
import de.pattaku.otakupulse.app.domain.Airing
import de.pattaku.otakupulse.app.domain.neuErschienen
import java.time.Instant

class AiringRepository(
    private val api: CompanionApi,
    private val db: AppDatabase,
    private val settings: SettingsStore,
) {

    suspend fun woche(tage: Int = 7): List<Airing> =
        api.airing(days = tage).airing.mapNotNull { it.toDomainOrNull() }

    /**
     * Sucht Folgen, die seit dem letzten Abgleich erschienen sind.
     *
     * Setzt das Badge in der Watchlist und liefert dieselbe Liste für die
     * Benachrichtigung zurück — eine Quelle, zwei Anzeigen, damit sie nicht
     * auseinanderlaufen.
     */
    suspend fun pruefeNeueFolgen(): List<Airing> {
        val seit = settings.letzterFolgenAbgleich()
        val jetzt = Instant.now()

        val alle = api.airing(days = 1, back = 14, onlyMine = true).airing
            .mapNotNull { it.toDomainOrNull() }
        val neu = neuErschienen(alle, seit, jetzt)

        // Nur Titel, die wirklich auf der Watchlist stehen — gewischt heisst nicht gemerkt.
        val aufWatchlist = db.watchlist().allIds().toSet()
        val relevant = neu.filter { it.animeId in aufWatchlist }

        if (relevant.isNotEmpty()) {
            db.watchlist().markUnseen(relevant.map { it.animeId }.distinct())
        }
        settings.setzeLetztenFolgenAbgleich(jetzt)
        return relevant
    }
}

private fun de.pattaku.otakupulse.app.data.api.AiringDto.toDomainOrNull(): Airing? {
    val zeit = airingAt ?: return null
    val instant = runCatching { Instant.parse(zeit) }
        // Das Backend liefert ISO mit Zeitzonen-Versatz; Instant.parse verlangt Zulu.
        .getOrElse { runCatching { java.time.OffsetDateTime.parse(zeit).toInstant() }.getOrNull() }
        ?: return null
    return Airing(
        animeId = animeId,
        episode = episode,
        airingAt = instant,
        title = title,
        coverImageUrl = coverImageUrl,
    )
}

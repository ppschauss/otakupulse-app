package de.pattaku.otakupulse.app.data

import de.pattaku.otakupulse.app.data.local.AppDatabase
import de.pattaku.otakupulse.app.data.local.WatchStatus
import de.pattaku.otakupulse.app.data.local.WatchlistEntry
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BackupEintrag(
    val animeId: Int,
    val title: String,
    val coverImageUrl: String? = null,
    val episodes: Int? = null,
    val format: String? = null,
    val seasonYear: Int? = null,
    val averageScore: Int? = null,
    val status: String,
    val progress: Int,
    val addedAt: Long,
)

@Serializable
data class Backup(
    /** Steigt nur, wenn sich das Format unverträglich ändert. */
    val version: Int = 1,
    val erstelltAm: Long,
    val geraeteToken: String? = null,
    val eintraege: List<BackupEintrag> = emptyList(),
)

/**
 * Sichert Watchlist und Geräte-Token in eine JSON-Datei.
 *
 * Das Token gehört mit hinein: es ist die einzige Identität, und ohne es wäre man
 * nach einem Gerätewechsel aus allen Partys verschwunden.
 */
class BackupRepository(private val db: AppDatabase, private val tokenStore: TokenStore) {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun exportieren(): String {
        val eintraege = db.watchlist().all().first().map { it.toBackup() }
        return json.encodeToString(
            Backup.serializer(),
            Backup(
                erstelltAm = System.currentTimeMillis(),
                geraeteToken = tokenStore.token(),
                eintraege = eintraege,
            ),
        )
    }

    /**
     * Spielt eine Sicherung ein und meldet, wie viele Einträge dazukamen.
     *
     * Bewusst ein Zusammenführen statt Ersetzen: wer zwischenzeitlich weitergewischt
     * hat, soll das nicht verlieren. Bei Titeln, die es beidseits gibt, gewinnt der
     * höhere Fortschritt — man hat eher mehr geschaut als weniger.
     */
    suspend fun importieren(inhalt: String): ImportErgebnis {
        val backup = json.decodeFromString(Backup.serializer(), inhalt)
        if (backup.version > 1) {
            throw IllegalArgumentException(
                "Diese Sicherung stammt aus einer neueren App-Fassung (Format ${backup.version}).",
            )
        }

        var neu = 0
        var aktualisiert = 0
        for (eintrag in backup.eintraege) {
            val vorhanden = db.watchlist().find(eintrag.animeId)
            if (vorhanden == null) {
                db.watchlist().upsert(eintrag.toEntry())
                neu++
            } else if (eintrag.progress > vorhanden.progress) {
                db.watchlist().setProgress(eintrag.animeId, eintrag.progress)
                aktualisiert++
            }
        }

        // Das Token nur übernehmen, wenn noch keins da ist: sonst würde ein Import
        // auf einem eingerichteten Gerät dessen Identität überschreiben.
        if (tokenStore.token() == null && backup.geraeteToken != null) {
            tokenStore.save(backup.geraeteToken, "Wiederhergestellt")
        }

        return ImportErgebnis(neu = neu, aktualisiert = aktualisiert, gesamt = backup.eintraege.size)
    }
}

data class ImportErgebnis(val neu: Int, val aktualisiert: Int, val gesamt: Int)

private fun WatchlistEntry.toBackup() = BackupEintrag(
    animeId = animeId,
    title = title,
    coverImageUrl = coverImageUrl,
    episodes = episodes,
    format = format,
    seasonYear = seasonYear,
    averageScore = averageScore,
    status = status.name,
    progress = progress,
    addedAt = addedAt,
)

private fun BackupEintrag.toEntry() = WatchlistEntry(
    animeId = animeId,
    title = title,
    coverImageUrl = coverImageUrl,
    episodes = episodes,
    format = format,
    seasonYear = seasonYear,
    averageScore = averageScore,
    status = runCatching { WatchStatus.valueOf(status) }.getOrDefault(WatchStatus.PLANNED),
    progress = progress,
    addedAt = addedAt,
)

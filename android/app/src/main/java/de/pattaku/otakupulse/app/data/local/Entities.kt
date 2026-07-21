package de.pattaku.otakupulse.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import de.pattaku.otakupulse.app.domain.Anime

/** Status eines Titels auf der Watchlist. */
enum class WatchStatus { PLANNED, WATCHING, COMPLETED, DROPPED }

/**
 * Ein Titel auf der Watchlist — mit allem, was zur Anzeige nötig ist.
 *
 * Die Anime-Daten werden bewusst mitkopiert statt nur referenziert: die Watchlist
 * muss auch offline und ohne Backend vollständig lesbar sein.
 */
@Entity(tableName = "watchlist_entry")
data class WatchlistEntry(
    @PrimaryKey val animeId: Int,
    val title: String,
    val coverImageUrl: String?,
    val episodes: Int?,
    val format: String?,
    val seasonYear: Int?,
    val averageScore: Int?,
    val status: WatchStatus = WatchStatus.PLANNED,
    val progress: Int = 0,
    /** Neue Folge erschienen, aber noch nicht angesehen — treibt das Badge in der Liste. */
    val hasUnseenEpisode: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
) {
    companion object {
        fun from(anime: Anime, status: WatchStatus = WatchStatus.PLANNED) = WatchlistEntry(
            animeId = anime.id,
            title = anime.title,
            coverImageUrl = anime.coverImageUrl,
            episodes = anime.episodes,
            format = anime.format,
            seasonYear = anime.seasonYear,
            averageScore = anime.averageScore,
            status = status,
        )
    }
}

/**
 * Ein Swipe, der noch nicht beim Server angekommen ist.
 *
 * Wischen soll auch im Funkloch funktionieren, deshalb landet jeder Swipe zuerst hier
 * und wird später gebündelt hochgeladen.
 */
@Entity(tableName = "pending_swipe")
data class PendingSwipe(
    @PrimaryKey val animeId: Int,
    val direction: String,
    /** Beim Super-Swipe die gewählten Partys, komma-getrennt. */
    val partyIds: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Eine empfangene Benachrichtigung.
 *
 * Android-Benachrichtigungen sind flüchtig — einmal weggewischt, sind sie weg.
 * Wer nachts einen Super-Swipe bekommt, soll morgens noch sehen können, worum es ging.
 */
@Entity(tableName = "meldung")
data class Meldung(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titel: String,
    val text: String,
    val animeId: Int? = null,
    /** SUPER_SWIPE oder NEUE_FOLGE — bestimmt das Symbol in der Liste. */
    val art: String,
    val empfangenAm: Long = System.currentTimeMillis(),
    val gelesen: Boolean = false,
)

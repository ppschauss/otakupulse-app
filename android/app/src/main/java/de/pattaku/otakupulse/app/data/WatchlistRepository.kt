package de.pattaku.otakupulse.app.data

import de.pattaku.otakupulse.app.data.api.CompanionApi
import de.pattaku.otakupulse.app.data.api.SwipeDto
import de.pattaku.otakupulse.app.data.api.SwipeUpload
import de.pattaku.otakupulse.app.data.local.AppDatabase
import de.pattaku.otakupulse.app.data.local.PendingSwipe
import de.pattaku.otakupulse.app.data.local.WatchStatus
import de.pattaku.otakupulse.app.data.local.WatchlistEntry
import de.pattaku.otakupulse.app.domain.Anime
import de.pattaku.otakupulse.app.domain.SwipeDirection
import kotlinx.coroutines.flow.Flow

/**
 * Watchlist und Swipe-Puffer.
 *
 * Jeder Swipe wirkt sofort lokal und wird erst danach hochgeladen — wischen soll
 * auch im Funkloch funktionieren.
 */
class WatchlistRepository(
    private val db: AppDatabase,
    private val api: CompanionApi,
) {

    fun all(): Flow<List<WatchlistEntry>> = db.watchlist().all()

    fun byStatus(status: WatchStatus): Flow<List<WatchlistEntry>> = db.watchlist().byStatus(status)

    /** Nimmt einen Swipe auf: lokal wirksam, zur Übertragung vorgemerkt. */
    suspend fun record(anime: Anime, direction: SwipeDirection, partyIds: List<Int>? = null) {
        if (direction != SwipeDirection.LEFT) {
            // Rechts und Super bedeuten beide Interesse — der Titel wandert auf die Watchlist.
            val existing = db.watchlist().find(anime.id)
            if (existing == null) {
                db.watchlist().upsert(WatchlistEntry.from(anime))
            }
        }
        db.pendingSwipes().add(
            PendingSwipe(
                animeId = anime.id,
                direction = direction.name,
                partyIds = partyIds?.takeIf { it.isNotEmpty() }?.joinToString(","),
            ),
        )
    }

    suspend fun pendingCount(): Int = db.pendingSwipes().count()

    /**
     * Schickt gepufferte Swipes zum Server und räumt nur das weg, was wirklich ankam.
     *
     * Gibt zurück, wie viele übertragen wurden. Wirft bei Netzfehlern — der Worker
     * versucht es dann später erneut.
     */
    suspend fun syncPending(): Int {
        val pending = db.pendingSwipes().oldest()
        if (pending.isEmpty()) return 0

        api.uploadSwipes(
            SwipeUpload(
                pending.map {
                    SwipeDto(
                        animeId = it.animeId,
                        direction = it.direction,
                        partyIds = it.partyIds?.split(",")?.mapNotNull(String::toIntOrNull),
                    )
                },
            ),
        )
        db.pendingSwipes().remove(pending.map { it.animeId })
        return pending.size
    }

    suspend fun setStatus(animeId: Int, status: WatchStatus) =
        db.watchlist().setStatus(animeId, status)

    suspend fun setProgress(animeId: Int, progress: Int) =
        db.watchlist().setProgress(animeId, progress.coerceAtLeast(0))

    suspend fun remove(animeId: Int) = db.watchlist().remove(animeId)

    /** Steht dieser Titel auf der Watchlist? Für den Knopf in der Detailansicht. */
    suspend fun enthaelt(animeId: Int): Boolean = db.watchlist().find(animeId) != null

    /** Nimmt einen Titel aus der Detailansicht heraus auf. */
    suspend fun hinzufuegen(anime: Anime) {
        if (db.watchlist().find(anime.id) == null) {
            db.watchlist().upsert(WatchlistEntry.from(anime))
        }
    }
}

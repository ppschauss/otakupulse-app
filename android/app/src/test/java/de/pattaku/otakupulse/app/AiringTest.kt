package de.pattaku.otakupulse.app

import de.pattaku.otakupulse.app.domain.Airing
import de.pattaku.otakupulse.app.domain.gruppiereNachTag
import de.pattaku.otakupulse.app.domain.neuErschienen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private fun airing(id: Int, zeit: String, episode: Int = 1) = Airing(
    animeId = id,
    episode = episode,
    airingAt = Instant.parse(zeit),
    title = "Anime $id",
    coverImageUrl = null,
)

class AiringTest {

    private val berlin = ZoneId.of("Europe/Berlin")

    @Test
    fun `leere Liste ergibt keine Tage`() {
        assertTrue(gruppiereNachTag(emptyList(), berlin).isEmpty())
    }

    @Test
    fun `Folgen desselben Tages landen zusammen`() {
        val tage = gruppiereNachTag(
            listOf(airing(1, "2026-07-21T10:00:00Z"), airing(2, "2026-07-21T18:00:00Z")),
            berlin,
        )
        assertEquals(1, tage.size)
        assertEquals(2, tage.first().folgen.size)
    }

    @Test
    fun `Tage kommen in zeitlicher Reihenfolge`() {
        val tage = gruppiereNachTag(
            listOf(airing(1, "2026-07-23T10:00:00Z"), airing(2, "2026-07-21T10:00:00Z")),
            berlin,
        )
        assertEquals(listOf(LocalDate.of(2026, 7, 21), LocalDate.of(2026, 7, 23)), tage.map { it.datum })
    }

    @Test
    fun `innerhalb eines Tages nach Uhrzeit sortiert`() {
        val tage = gruppiereNachTag(
            listOf(airing(1, "2026-07-21T18:00:00Z"), airing(2, "2026-07-21T09:00:00Z")),
            berlin,
        )
        assertEquals(listOf(2, 1), tage.first().folgen.map { it.animeId })
    }

    @Test
    fun `Zeitzone entscheidet ueber den Kalendertag`() {
        // 22:30 UTC ist in Berlin bereits der Folgetag — japanische Ausstrahlungen
        // liegen oft in diesem Bereich.
        val spaet = listOf(airing(1, "2026-07-21T22:30:00Z"))
        assertEquals(LocalDate.of(2026, 7, 22), gruppiereNachTag(spaet, berlin).first().datum)
        assertEquals(
            LocalDate.of(2026, 7, 21),
            gruppiereNachTag(spaet, ZoneId.of("UTC")).first().datum,
        )
    }

    @Test
    fun `neu erschienen sind nur Folgen zwischen letztem Abgleich und jetzt`() {
        val folgen = listOf(
            airing(1, "2026-07-20T10:00:00Z"),  // vor dem Abgleich
            airing(2, "2026-07-21T10:00:00Z"),  // dazwischen → neu
            airing(3, "2026-07-22T10:00:00Z"),  // noch nicht ausgestrahlt
        )
        val neu = neuErschienen(
            folgen,
            seit = Instant.parse("2026-07-20T12:00:00Z"),
            jetzt = Instant.parse("2026-07-21T12:00:00Z"),
        )
        assertEquals(listOf(2), neu.map { it.animeId })
    }

    @Test
    fun `kuenftige Folgen loesen keine Benachrichtigung aus`() {
        val neu = neuErschienen(
            listOf(airing(1, "2026-07-25T10:00:00Z")),
            seit = Instant.parse("2026-07-20T00:00:00Z"),
            jetzt = Instant.parse("2026-07-21T00:00:00Z"),
        )
        assertTrue(neu.isEmpty())
    }

    @Test
    fun `derselbe Abgleich meldet nichts zweimal`() {
        val folgen = listOf(airing(1, "2026-07-21T10:00:00Z"))
        val jetzt = Instant.parse("2026-07-21T12:00:00Z")
        assertEquals(1, neuErschienen(folgen, Instant.parse("2026-07-20T00:00:00Z"), jetzt).size)
        // Zweiter Lauf, Zeitstempel steht jetzt auf „jetzt" — nichts Neues mehr.
        assertTrue(neuErschienen(folgen, jetzt, jetzt).isEmpty())
    }
}

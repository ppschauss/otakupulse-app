package de.pattaku.otakupulse.app

import de.pattaku.otakupulse.app.domain.Anime
import de.pattaku.otakupulse.app.domain.DeckBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun anime(id: Int) = Anime(
    id = id,
    anilistId = id,
    slug = "anime-$id",
    title = "Anime $id",
    titleRomaji = "Anime $id",
    description = null,
    coverImageUrl = "https://example.invalid/$id.jpg",
    bannerImageUrl = null,
    format = "TV",
    status = "FINISHED",
    episodes = 12,
    season = "WINTER",
    seasonYear = 2020,
    averageScore = 75,
)

class DeckBufferTest {

    @Test
    fun `leerer Stapel meldet keine Karte`() {
        assertEquals(null, DeckBuffer().top())
    }

    @Test
    fun `Karten werden in gelieferter Reihenfolge gezeigt`() {
        val buffer = DeckBuffer()
        buffer.append(listOf(anime(1), anime(2)))
        assertEquals(1, buffer.top()?.id)
        buffer.drop()
        assertEquals(2, buffer.top()?.id)
    }

    @Test
    fun `nachladen sobald weniger als fuenf Karten uebrig sind`() {
        val buffer = DeckBuffer()
        buffer.append((1..6).map(::anime))
        assertFalse(buffer.needsMore())
        buffer.drop()
        // Jetzt sind es fünf — noch kein Nachladen, sonst lädt die App unnötig früh.
        assertFalse(buffer.needsMore())
        buffer.drop()
        assertTrue(buffer.needsMore())
    }

    @Test
    fun `doppelte Karten werden verworfen`() {
        // Der Server paginiert; bei gleichzeitigem Wischen kann eine Karte doppelt kommen.
        val buffer = DeckBuffer()
        buffer.append(listOf(anime(1), anime(2)))
        buffer.append(listOf(anime(2), anime(3)))
        assertEquals(listOf(1, 2, 3), buffer.remaining().map { it.id })
    }

    @Test
    fun `bereits gewischte Karten kommen nicht zurueck`() {
        // Sonst taucht ein weggewischter Titel beim Nachladen erneut auf.
        val buffer = DeckBuffer()
        buffer.append(listOf(anime(1), anime(2)))
        buffer.drop()
        buffer.append(listOf(anime(1), anime(3)))
        assertEquals(listOf(2, 3), buffer.remaining().map { it.id })
    }

    @Test
    fun `offset waechst mit der Zahl geladener Karten`() {
        val buffer = DeckBuffer()
        assertEquals(0, buffer.nextOffset())
        buffer.append((1..20).map(::anime))
        assertEquals(20, buffer.nextOffset())
        // Wischen verschiebt den Offset nicht — der Server zählt Katalogpositionen, keine Restkarten.
        buffer.drop()
        assertEquals(20, buffer.nextOffset())
    }

    @Test
    fun `erschoepfter Stapel meldet sich`() {
        val buffer = DeckBuffer()
        buffer.append(listOf(anime(1)))
        assertFalse(buffer.isExhausted)
        buffer.append(emptyList())
        assertTrue(buffer.isExhausted)
    }
}

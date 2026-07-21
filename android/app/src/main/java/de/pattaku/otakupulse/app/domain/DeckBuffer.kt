package de.pattaku.otakupulse.app.domain

/**
 * Hält die Karten des Swipe-Stapels und entscheidet, wann nachgeladen wird.
 *
 * Bewusst frei von Android und Netzwerk, damit die Logik als reiner Unit-Test läuft —
 * auf dem Unraid-Host gibt es keinen Emulator.
 */
class DeckBuffer(private val prefetchThreshold: Int = 5) {

    private val cards = ArrayDeque<Anime>()

    /** Alles, was schon einmal im Stapel lag — verhindert Wiedergänger beim Nachladen. */
    private val known = mutableSetOf<Int>()

    /** Zählt die vom Server gelieferten Karten; entspricht dem Offset der nächsten Seite. */
    private var loaded = 0

    /** Wahr, sobald der Server eine leere Seite geliefert hat. */
    var isExhausted: Boolean = false
        private set

    fun top(): Anime? = cards.firstOrNull()

    fun remaining(): List<Anime> = cards.toList()

    fun size(): Int = cards.size

    fun nextOffset(): Int = loaded

    fun needsMore(): Boolean = !isExhausted && cards.size < prefetchThreshold

    /** Nimmt eine Serverseite auf. Bereits bekannte Titel werden still verworfen. */
    fun append(page: List<Anime>) {
        if (page.isEmpty()) {
            isExhausted = true
            return
        }
        loaded += page.size
        for (anime in page) {
            if (known.add(anime.id)) cards.addLast(anime)
        }
    }

    /** Entfernt die oberste Karte, nachdem sie weggewischt wurde. */
    fun drop(): Anime? = cards.removeFirstOrNull()

    fun clear() {
        cards.clear()
        known.clear()
        loaded = 0
        isExhausted = false
    }
}

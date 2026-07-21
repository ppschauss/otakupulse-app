package de.pattaku.otakupulse.app.domain

/**
 * Feste Auswahlwerte für das Filterblatt.
 *
 * Die Beschriftungen sind deutsch, die Werte entsprechen exakt den Enums in der
 * OtakuPulse-Datenbank — ein Tippfehler hier liefert stillschweigend einen leeren
 * Stapel statt einer Fehlermeldung.
 */
data class Auswahl(val wert: String, val label: String)

/** Synchronisation: was an Sprachfassungen vorliegt. */
val SYNC_OPTIONEN = listOf(
    Auswahl("de-dub", "Deutsche Synchro"),
    Auswahl("de-sub", "Deutscher Sub"),
    Auswahl("en-dub", "Englische Synchro"),
    Auswahl("en-sub", "Englischer Sub"),
    Auswahl("ja", "Japanisch"),
)

val TYP_OPTIONEN = listOf(
    Auswahl("TV", "Serie"),
    Auswahl("TV_SHORT", "Kurzserie"),
    Auswahl("MOVIE", "Film"),
    Auswahl("OVA", "OVA"),
    Auswahl("ONA", "ONA"),
    Auswahl("SPECIAL", "Special"),
)

val STATUS_OPTIONEN = listOf(
    Auswahl("RELEASING", "Läuft gerade"),
    Auswahl("FINISHED", "Abgeschlossen"),
    Auswahl("NOT_YET_RELEASED", "Angekündigt"),
)

/** Wie viele Bereiche eingeschränkt sind — für das Abzeichen am Filterknopf. */
fun DeckFilter.anzahlAktiv(): Int =
    listOf(genres, tags, languages, formats, statuses).count { it.isNotEmpty() } +
        listOfNotNull(minScore).size

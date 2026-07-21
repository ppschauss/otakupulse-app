package de.pattaku.otakupulse.app.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Eine angekündigte Folge. */
data class Airing(
    val animeId: Int,
    val episode: Int?,
    val airingAt: Instant,
    val title: String,
    val coverImageUrl: String?,
)

/** Ein Kalendertag mit seinen Folgen. */
data class AiringTag(val datum: LocalDate, val folgen: List<Airing>)

/**
 * Gruppiert Folgen nach Kalendertag in der angegebenen Zeitzone.
 *
 * Die Zeitzone ist ein Parameter statt fest verdrahtet: japanische Ausstrahlungen
 * liegen oft nach Mitternacht, und ob eine Folge auf Montag oder Dienstag fällt,
 * hängt genau daran.
 */
fun gruppiereNachTag(
    folgen: List<Airing>,
    zone: ZoneId = ZoneId.systemDefault(),
): List<AiringTag> =
    folgen
        .sortedBy { it.airingAt }
        .groupBy { it.airingAt.atZone(zone).toLocalDate() }
        .map { (datum, liste) -> AiringTag(datum, liste) }
        .sortedBy { it.datum }

/**
 * Folgen, die seit dem letzten Abgleich erschienen sind.
 *
 * Grundlage für Benachrichtigung und Badge — beide speisen sich aus derselben
 * Quelle, damit sie nicht auseinanderlaufen können.
 */
fun neuErschienen(folgen: List<Airing>, seit: Instant, jetzt: Instant): List<Airing> =
    folgen.filter { it.airingAt > seit && it.airingAt <= jetzt }

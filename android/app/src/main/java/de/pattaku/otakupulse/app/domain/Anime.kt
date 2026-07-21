package de.pattaku.otakupulse.app.domain

/** Ein Anime, so wie ihn die App braucht — auf einer Swipe-Karte und in der Detailansicht. */
data class Anime(
    val id: Int,
    val anilistId: Int?,
    val slug: String,
    val title: String,
    val titleRomaji: String,
    val description: String?,
    val coverImageUrl: String?,
    val bannerImageUrl: String?,
    val format: String?,
    val status: String?,
    val episodes: Int?,
    val season: String?,
    val seasonYear: Int?,
    val averageScore: Int?,
)

/** In welche Richtung eine Karte weggewischt wurde. */
enum class SwipeDirection {
    /** Kein Interesse. */
    LEFT,

    /** Auf die Watchlist. */
    RIGHT,

    /** Super-Swipe: alle in der Party bekommen eine Benachrichtigung. */
    SUPER,
}

/** Wonach der Stapel gefüllt wird. Leere Listen bedeuten „egal". */
data class DeckFilter(
    val genres: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val providers: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val formats: List<String> = emptyList(),
    val yearFrom: Int? = null,
    val yearTo: Int? = null,
    val minScore: Int? = null,
)

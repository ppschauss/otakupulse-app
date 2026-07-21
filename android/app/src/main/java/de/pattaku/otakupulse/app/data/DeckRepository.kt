package de.pattaku.otakupulse.app.data

import de.pattaku.otakupulse.app.data.api.CompanionApi
import de.pattaku.otakupulse.app.data.api.DeviceRequest
import de.pattaku.otakupulse.app.data.api.UpdateDeviceRequest
import de.pattaku.otakupulse.app.domain.Anime
import de.pattaku.otakupulse.app.domain.DeckFilter

/** Holt Karten für den Stapel und sorgt dafür, dass das Gerät registriert ist. */
class DeckRepository(
    private val api: CompanionApi,
    private val tokenStore: TokenStore,
) {

    /** Beim ersten Start registriert sich das Gerät selbst — der Nutzer meldet sich nirgends an. */
    suspend fun ensureRegistered(defaultName: String) {
        if (tokenStore.token() != null) return
        val response = api.registerDevice(DeviceRequest(displayName = defaultName))
        tokenStore.save(response.token, response.displayName)
    }

    /** Meldet das FCM-Token, damit Super-Swipes dieses Gerät erreichen. */
    suspend fun meldeFcmToken(token: String) {
        if (tokenStore.token() == null) return  // ohne Registrierung gibt es nichts zu aktualisieren
        api.updateDevice(UpdateDeviceRequest(fcmToken = token))
    }

    /** Einzelner Titel für die Detailansicht aus Watchlist oder Meldung heraus. */
    suspend fun detail(animeId: Int) = api.anime(animeId)

    suspend fun loadPage(filter: DeckFilter, seed: String, offset: Int, limit: Int = 20): List<Anime> =
        api.deck(
            genres = filter.genres,
            tags = filter.tags,
            providers = filter.providers,
            languages = filter.languages,
            formats = filter.formats,
            statuses = filter.statuses,
            yearFrom = filter.yearFrom,
            yearTo = filter.yearTo,
            minScore = filter.minScore,
            sort = "random",
            seed = seed,
            limit = limit,
            offset = offset,
        ).cards.map { it.toDomain() }
}

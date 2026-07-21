package de.pattaku.otakupulse.app.data.api

import de.pattaku.otakupulse.app.domain.Anime
import kotlinx.serialization.Serializable

@Serializable
data class AnimeDto(
    val id: Int,
    val anilistId: Int? = null,
    val slug: String,
    val title: String,
    val titleRomaji: String,
    val description: String? = null,
    val coverImageUrl: String? = null,
    val bannerImageUrl: String? = null,
    val format: String? = null,
    val status: String? = null,
    val episodes: Int? = null,
    val season: String? = null,
    val seasonYear: Int? = null,
    val averageScore: Int? = null,
) {
    fun toDomain(): Anime = Anime(
        id = id,
        anilistId = anilistId,
        slug = slug,
        title = title,
        titleRomaji = titleRomaji,
        description = description,
        coverImageUrl = coverImageUrl,
        bannerImageUrl = bannerImageUrl,
        format = format,
        status = status,
        episodes = episodes,
        season = season,
        seasonYear = seasonYear,
        averageScore = averageScore,
    )
}

@Serializable
data class DeckResponse(val cards: List<AnimeDto>, val count: Int)

@Serializable
data class DeviceRequest(val displayName: String, val fcmToken: String? = null)

@Serializable
data class DeviceResponse(val deviceId: Int, val token: String, val displayName: String)

@Serializable
data class GenreDto(val slug: String, val name: String)

@Serializable
data class TagDto(val slug: String, val name: String, val category: String? = null)

@Serializable
data class FiltersResponse(val genres: List<GenreDto>, val tags: List<TagDto>)

@Serializable
data class SwipeDto(val animeId: Int, val direction: String)

@Serializable
data class SwipeUpload(val swipes: List<SwipeDto>)

@Serializable
data class MatchDto(val partyId: Int, val animeId: Int)

@Serializable
data class SwipeUploadResponse(val accepted: Int, val matches: List<MatchDto> = emptyList())

@Serializable
data class HealthResponse(val status: String)

@Serializable
data class PartyMemberDto(val id: Int, val displayName: String, val isMe: Boolean = false)

@Serializable
data class PartyDto(
    val id: Int,
    val name: String,
    val joinCode: String,
    val members: List<PartyMemberDto> = emptyList(),
    val matches: List<AnimeDto> = emptyList(),
)

@Serializable
data class PartiesResponse(val parties: List<PartyDto>)

@Serializable
data class CreatePartyRequest(val name: String)

@Serializable
data class JoinPartyRequest(val joinCode: String)

@Serializable
data class AiringDto(
    val animeId: Int,
    val episode: Int? = null,
    val airingAt: String? = null,
    val title: String,
    val coverImageUrl: String? = null,
)

@Serializable
data class AiringResponse(val airing: List<AiringDto>)

@Serializable
data class UpdateDeviceRequest(val displayName: String? = null, val fcmToken: String? = null)

@Serializable
data class UpdateDeviceResponse(val deviceId: Int, val displayName: String)

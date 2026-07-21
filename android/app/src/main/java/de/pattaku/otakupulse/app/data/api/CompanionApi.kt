package de.pattaku.otakupulse.app.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Das Backend unter app.otakupulse.de.
 *
 * Als Interface geschnitten, damit Tests eine Attrappe einsetzen können, ohne
 * einen Server zu starten.
 */
interface CompanionApi {

    /** Erreichbarkeitsprüfung — die einzige Route ohne Token. */
    @GET("health")
    suspend fun health(): HealthResponse

    @POST("v1/devices")
    suspend fun registerDevice(@Body body: DeviceRequest): DeviceResponse

    @PATCH("v1/devices/me")
    suspend fun updateDevice(@Body body: UpdateDeviceRequest): UpdateDeviceResponse

    @GET("v1/deck")
    suspend fun deck(
        @Query("genres") genres: List<String> = emptyList(),
        @Query("tags") tags: List<String> = emptyList(),
        @Query("providers") providers: List<String> = emptyList(),
        @Query("languages") languages: List<String> = emptyList(),
        @Query("formats") formats: List<String> = emptyList(),
        @Query("statuses") statuses: List<String> = emptyList(),
        @Query("yearFrom") yearFrom: Int? = null,
        @Query("yearTo") yearTo: Int? = null,
        @Query("minScore") minScore: Int? = null,
        @Query("sort") sort: String = "popularity",
        @Query("seed") seed: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
    ): DeckResponse

    @POST("v1/swipes")
    suspend fun uploadSwipes(@Body body: SwipeUpload): SwipeUploadResponse

    @GET("v1/parties")
    suspend fun parties(): PartiesResponse

    @POST("v1/parties")
    suspend fun createParty(@Body body: CreatePartyRequest): PartyDto

    @POST("v1/parties/join")
    suspend fun joinParty(@Body body: JoinPartyRequest): PartyDto

    @GET("v1/anime/{id}")
    suspend fun anime(@retrofit2.http.Path("id") id: Int): AnimeDto

    @GET("v1/airing")
    suspend fun airing(
        @Query("days") days: Int = 7,
        @Query("back") back: Int = 0,
        @Query("onlyMine") onlyMine: Boolean = false,
    ): AiringResponse

    @retrofit2.http.PATCH("v1/parties/{id}")
    suspend fun renameParty(
        @retrofit2.http.Path("id") id: Int,
        @Body body: RenamePartyRequest,
    ): PartyDto

    @retrofit2.http.DELETE("v1/parties/{id}")
    suspend fun deleteParty(@retrofit2.http.Path("id") id: Int)

    @POST("v1/parties/{id}/leave")
    suspend fun leaveParty(@retrofit2.http.Path("id") id: Int)

    @GET("v1/filters")
    suspend fun filters(): FiltersResponse
}

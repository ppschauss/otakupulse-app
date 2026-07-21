package de.pattaku.otakupulse.app.di

import android.content.Context
import de.pattaku.otakupulse.app.BuildConfig
import de.pattaku.otakupulse.app.data.AiringRepository
import de.pattaku.otakupulse.app.data.DeckRepository
import de.pattaku.otakupulse.app.data.SettingsStore
import de.pattaku.otakupulse.app.data.TokenStore
import de.pattaku.otakupulse.app.data.WatchlistRepository
import de.pattaku.otakupulse.app.data.local.AppDatabase
import de.pattaku.otakupulse.app.data.api.CompanionApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/** Manuelle Abhängigkeitsverdrahtung — dieselbe Bauweise wie in WorkTracker. */
class AppContainer(context: Context) {

    val applicationContext: Context = context.applicationContext

    val tokenStore = TokenStore(applicationContext)

    val settingsStore = SettingsStore(applicationContext)

    private val json = Json {
        ignoreUnknownKeys = true  // Backend darf Felder ergänzen, ohne alte Apps zu brechen
        explicitNulls = false
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            // Serveradresse zur Laufzeit umbiegen: Retrofit kennt nur die Basis-URL aus
            // dem Build, die tatsächliche steht in den Einstellungen.
            val ziel = settingsStore.cachedServerUrl().toHttpUrlOrNull()
            var request = chain.request()
            if (ziel != null) {
                request = request.newBuilder().url(
                    request.url.newBuilder()
                        .scheme(ziel.scheme)
                        .host(ziel.host)
                        .port(ziel.port)
                        .build(),
                ).build()
            }

            val token = tokenStore.cachedToken()
            if (token != null) {
                request = request.newBuilder().header("Authorization", "Bearer $token").build()
            }
            chain.proceed(request)
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(http)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val api: CompanionApi = retrofit.create(CompanionApi::class.java)

    val database = AppDatabase.build(applicationContext)

    val deckRepository = DeckRepository(api, tokenStore)

    val watchlistRepository = WatchlistRepository(database, api)

    val airingRepository = AiringRepository(api, database, settingsStore)
}

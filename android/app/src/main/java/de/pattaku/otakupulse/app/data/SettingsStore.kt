package de.pattaku.otakupulse.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import de.pattaku.otakupulse.app.BuildConfig
import kotlinx.coroutines.flow.first

/**
 * Die Adresse des Backends.
 *
 * Einstellbar statt einkompiliert: je nachdem, wo das Handy gerade steckt, ist der
 * Server über die öffentliche Domain, über die LAN-IP oder über Tailscale erreichbar.
 * Ohne diese Einstellung müsste für jeden Fall eine eigene APK gebaut werden.
 */
class SettingsStore(private val context: Context) {

    private val urlKey = stringPreferencesKey("server_url")

    @Volatile
    private var cached: String = BuildConfig.API_BASE_URL

    /** Synchron für den OkHttp-Interceptor. */
    fun cachedServerUrl(): String = cached

    suspend fun load(): String {
        cached = context.companionDataStore.data.first()[urlKey] ?: BuildConfig.API_BASE_URL
        return cached
    }

    suspend fun save(url: String) {
        val normalized = normalize(url)
        cached = normalized
        context.companionDataStore.edit { it[urlKey] = normalized }
    }

    companion object {
        /** Nimmt „192.168.0.161:3005" ebenso an wie eine vollständige URL. */
        fun normalize(input: String): String {
            var url = input.trim()
            if (url.isEmpty()) return BuildConfig.API_BASE_URL
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                // Nackte IP mit Port meint fast immer das LAN, also unverschlüsselt.
                url = if (url.firstOrNull()?.isDigit() == true) "http://$url" else "https://$url"
            }
            if (!url.endsWith("/")) url = "$url/"
            return url
        }
    }
}

package de.pattaku.otakupulse.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

// Ein gemeinsamer DataStore für Token und Einstellungen — zwei Instanzen auf derselben
// Datei wären ein Laufzeitfehler.
val Context.companionDataStore by preferencesDataStore(name = "companion")

/**
 * Speichert das Geräte-Token. Es gibt bewusst keine Konten — dieses Token *ist* die Identität,
 * deshalb wird es von Androids Auto Backup mitgesichert und übersteht einen Gerätewechsel.
 */
class TokenStore(private val context: Context) {

    private val tokenKey = stringPreferencesKey("device_token")
    private val nameKey = stringPreferencesKey("display_name")

    @Volatile
    private var cached: String? = null

    /** Synchroner Zugriff für den OkHttp-Interceptor. */
    fun cachedToken(): String? = cached

    suspend fun token(): String? {
        cached?.let { return it }
        val stored = context.companionDataStore.data.first()[tokenKey]
        cached = stored
        return stored
    }

    suspend fun save(token: String, displayName: String) {
        cached = token
        context.companionDataStore.edit {
            it[tokenKey] = token
            it[nameKey] = displayName
        }
    }

    suspend fun displayName(): String? = context.companionDataStore.data.first()[nameKey]
}

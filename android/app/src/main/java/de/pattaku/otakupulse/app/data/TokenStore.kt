package de.pattaku.otakupulse.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "companion")

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
        val stored = context.dataStore.data.first()[tokenKey]
        cached = stored
        return stored
    }

    suspend fun save(token: String, displayName: String) {
        cached = token
        context.dataStore.edit {
            it[tokenKey] = token
            it[nameKey] = displayName
        }
    }

    suspend fun displayName(): String? = context.dataStore.data.first()[nameKey]
}

package de.pattaku.otakupulse.app.ui.settings

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.pattaku.otakupulse.app.data.DeckRepository
import de.pattaku.otakupulse.app.data.SettingsStore
import de.pattaku.otakupulse.app.data.api.CompanionApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ServerUiState(
    val url: String = "",
    val pruefend: Boolean = false,
    val meldung: String? = null,
    val erfolgreich: Boolean = false,
)

class ServerViewModel(
    private val settings: SettingsStore,
    private val api: CompanionApi,
    private val deck: DeckRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ServerUiState())
    val state: StateFlow<ServerUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(url = settings.load().removeSuffix("/"))
        }
    }

    fun setzeUrl(url: String) {
        _state.value = _state.value.copy(url = url, meldung = null, erfolgreich = false)
    }

    /**
     * Speichert die Adresse und arbeitet drei Stufen ab.
     *
     * Wichtig ist die Reihenfolge: erst /health, das als einzige Route kein Token
     * verlangt. Sonst meldet ein noch nicht registriertes Gerät „401", obwohl in
     * Wahrheit nur die Registrierung fehlt — und man sucht den Fehler am falschen Ende.
     */
    fun pruefenUndSpeichern() = viewModelScope.launch {
        _state.value = _state.value.copy(pruefend = true, meldung = null)
        val vorher = settings.cachedServerUrl()
        settings.save(_state.value.url)

        try {
            api.health()
        } catch (e: Exception) {
            settings.save(vorher)  // unerreichbare Adresse nicht dauerhaft übernehmen
            melde("Server nicht erreichbar: ${grund(e)}")
            return@launch
        }

        try {
            // Legt das Gerät an, falls noch keins existiert — hier fehlte es bisher.
            deck.ensureRegistered(defaultName = Build.MODEL ?: "Mein Gerät")
        } catch (e: Exception) {
            melde("Server antwortet, aber die Anmeldung des Geräts scheiterte: ${grund(e)}")
            return@launch
        }

        try {
            api.filters()
            _state.value = _state.value.copy(
                pruefend = false,
                erfolgreich = true,
                meldung = "Verbunden. Der Stapel sollte jetzt laden.",
            )
        } catch (e: Exception) {
            melde("Angemeldet, aber die Daten kamen nicht durch: ${grund(e)}")
        }
    }

    private fun melde(text: String) {
        _state.value = _state.value.copy(pruefend = false, erfolgreich = false, meldung = text)
    }

    private fun grund(e: Exception): String = when (e) {
        is retrofit2.HttpException -> "HTTP ${e.code()}"
        is java.net.UnknownHostException -> "Adresse unbekannt"
        is java.net.ConnectException -> "keine Verbindung"
        is javax.net.ssl.SSLException -> "TLS-Fehler — bei einer LAN-IP http statt https nutzen"
        else -> e.message ?: e::class.simpleName ?: "unbekannt"
    }
}

package de.pattaku.otakupulse.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
     * Speichert die Adresse und prüft sie sofort.
     *
     * Gespeichert wird vor der Prüfung, weil der Interceptor die Adresse aus den
     * Einstellungen zieht — sonst würde gegen den alten Server geprüft.
     */
    fun pruefenUndSpeichern() = viewModelScope.launch {
        _state.value = _state.value.copy(pruefend = true, meldung = null)
        val vorher = settings.cachedServerUrl()
        try {
            settings.save(_state.value.url)
            api.filters()  // eine echte Abfrage, keine bloße Erreichbarkeitsprüfung
            _state.value = _state.value.copy(
                pruefend = false,
                erfolgreich = true,
                meldung = "Verbunden. Der Stapel sollte jetzt laden.",
            )
        } catch (e: Exception) {
            settings.save(vorher)  // kaputte Adresse nicht dauerhaft übernehmen
            _state.value = _state.value.copy(
                pruefend = false,
                erfolgreich = false,
                meldung = "Keine Verbindung: ${e.message ?: e::class.simpleName}",
            )
        }
    }
}

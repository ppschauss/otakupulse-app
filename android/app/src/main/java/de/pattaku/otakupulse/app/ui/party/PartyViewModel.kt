package de.pattaku.otakupulse.app.ui.party

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.pattaku.otakupulse.app.data.api.CompanionApi
import de.pattaku.otakupulse.app.data.api.CreatePartyRequest
import de.pattaku.otakupulse.app.data.api.JoinPartyRequest
import de.pattaku.otakupulse.app.data.api.PartyDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PartyUiState(
    val parties: List<PartyDto> = emptyList(),
    val laden: Boolean = true,
    val fehler: String? = null,
)

class PartyViewModel(private val api: CompanionApi) : ViewModel() {

    private val _state = MutableStateFlow(PartyUiState())
    val state: StateFlow<PartyUiState> = _state.asStateFlow()

    init {
        aktualisieren()
    }

    fun aktualisieren() = viewModelScope.launch {
        _state.value = _state.value.copy(laden = true, fehler = null)
        try {
            _state.value = PartyUiState(parties = api.parties().parties, laden = false)
        } catch (e: Exception) {
            _state.value = _state.value.copy(laden = false, fehler = beschreibe(e))
        }
    }

    fun anlegen(name: String) = viewModelScope.launch {
        try {
            api.createParty(CreatePartyRequest(name.ifBlank { "Meine Party" }))
            aktualisieren()
        } catch (e: Exception) {
            _state.value = _state.value.copy(fehler = beschreibe(e))
        }
    }

    fun beitreten(code: String) = viewModelScope.launch {
        try {
            api.joinParty(JoinPartyRequest(code.trim().uppercase()))
            aktualisieren()
        } catch (e: Exception) {
            _state.value = _state.value.copy(fehler = beschreibe(e))
        }
    }

    fun umbenennen(id: Int, name: String) = viewModelScope.launch {
        try {
            api.renameParty(id, de.pattaku.otakupulse.app.data.api.RenamePartyRequest(name.trim()))
            aktualisieren()
        } catch (e: Exception) {
            _state.value = _state.value.copy(fehler = beschreibe(e))
        }
    }

    fun loeschen(id: Int) = viewModelScope.launch {
        try {
            api.deleteParty(id)
            aktualisieren()
        } catch (e: Exception) {
            _state.value = _state.value.copy(fehler = beschreibe(e))
        }
    }

    fun verlassen(id: Int) = viewModelScope.launch {
        try {
            api.leaveParty(id)
            aktualisieren()
        } catch (e: Exception) {
            _state.value = _state.value.copy(fehler = beschreibe(e))
        }
    }

    private fun beschreibe(e: Exception): String = when {
        e is retrofit2.HttpException && e.code() == 404 -> "Diesen Party-Code gibt es nicht."
        e is retrofit2.HttpException && e.code() == 403 ->
            "Nur wer die Party angelegt hat, kann sie löschen — du kannst sie verlassen."
        e is retrofit2.HttpException -> "Server meldet HTTP ${e.code()}"
        e is java.net.UnknownHostException || e is java.net.ConnectException ->
            "Keine Verbindung — stimmt die Adresse unter „Server“?"
        else -> e.message ?: "Unbekannter Fehler"
    }
}

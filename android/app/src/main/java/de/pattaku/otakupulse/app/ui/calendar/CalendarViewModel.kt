package de.pattaku.otakupulse.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.pattaku.otakupulse.app.data.AiringRepository
import de.pattaku.otakupulse.app.domain.AiringTag
import de.pattaku.otakupulse.app.domain.gruppiereNachTag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CalendarUiState(
    val tage: List<AiringTag> = emptyList(),
    val laden: Boolean = true,
    val fehler: String? = null,
)

class CalendarViewModel(private val repository: AiringRepository) : ViewModel() {

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    init {
        laden()
    }

    fun laden() = viewModelScope.launch {
        _state.value = _state.value.copy(laden = true, fehler = null)
        try {
            _state.value = CalendarUiState(tage = gruppiereNachTag(repository.woche()), laden = false)
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                laden = false,
                fehler = when (e) {
                    is java.net.UnknownHostException, is java.net.ConnectException ->
                        "Keine Verbindung — stimmt die Adresse unter „Server“?"
                    else -> e.message ?: "Unbekannter Fehler"
                },
            )
        }
    }
}

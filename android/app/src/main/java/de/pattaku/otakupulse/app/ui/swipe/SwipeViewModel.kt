package de.pattaku.otakupulse.app.ui.swipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import de.pattaku.otakupulse.app.data.DeckRepository
import de.pattaku.otakupulse.app.data.WatchlistRepository
import de.pattaku.otakupulse.app.work.SwipeSyncWorker
import de.pattaku.otakupulse.app.domain.Anime
import de.pattaku.otakupulse.app.domain.DeckBuffer
import de.pattaku.otakupulse.app.domain.DeckFilter
import de.pattaku.otakupulse.app.domain.SwipeDirection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class SwipeUiState(
    val cards: List<Anime> = emptyList(),
    val loading: Boolean = true,
    val exhausted: Boolean = false,
    val error: String? = null,
)

class SwipeViewModel(
    private val repository: DeckRepository,
    private val watchlist: WatchlistRepository,
    private val appContext: Context,
) : ViewModel() {

    private val buffer = DeckBuffer()

    // Ein Seed pro Sitzung: die Reihenfolge bleibt beim Nachladen stabil, ist aber
    // beim nächsten Start eine andere. Sonst sieht man immer dieselben Karten zuerst.
    private val seed = UUID.randomUUID().toString()

    private var filter = DeckFilter()
    private var loadingPage = false

    private val _state = MutableStateFlow(SwipeUiState())
    val state: StateFlow<SwipeUiState> = _state.asStateFlow()

    init {
        start()
    }

    private fun start() = viewModelScope.launch {
        try {
            _state.value = _state.value.copy(loading = true)
            repository.ensureRegistered(defaultName = android.os.Build.MODEL ?: "Mein Gerät")
            loadMore()
        } catch (e: Exception) {
            _state.value = _state.value.copy(loading = false, error = fehlertext(e))
        }
    }

    fun setFilter(newFilter: DeckFilter) {
        filter = newFilter
        buffer.clear()
        _state.value = SwipeUiState(loading = true)
        viewModelScope.launch { loadMore() }
    }

    fun onSwiped(direction: SwipeDirection) {
        val karte = buffer.top()
        buffer.drop()
        publish()

        if (karte != null) {
            viewModelScope.launch {
                // Erst lokal wirksam machen, dann übertragen — Wischen muss auch
                // im Funkloch funktionieren.
                watchlist.record(karte, direction)
                SwipeSyncWorker.enqueue(appContext)
            }
        }

        if (buffer.needsMore()) viewModelScope.launch { loadMore() }
    }

    /**
     * Erneut versuchen — inklusive Registrierung.
     *
     * Die lief bisher nur beim allerersten Start. Scheiterte sie dort (Server noch
     * nicht erreichbar), blieb das Gerät ohne Token und jeder weitere Versuch endete
     * in einem irreführenden 401.
     */
    fun retry() {
        _state.value = _state.value.copy(loading = true, error = null)
        start()
    }

    private suspend fun loadMore() {
        if (loadingPage || buffer.isExhausted) return
        loadingPage = true
        try {
            val page = repository.loadPage(filter, seed, buffer.nextOffset())
            buffer.append(page)
            _state.value = _state.value.copy(error = null)
        } catch (e: Exception) {
            _state.value = _state.value.copy(error = fehlertext(e))
        } finally {
            loadingPage = false
            publish()
        }
    }

    private fun publish() {
        _state.value = _state.value.copy(
            cards = buffer.remaining(),
            loading = false,
            exhausted = buffer.isExhausted && buffer.size() == 0,
        )
    }

    private fun fehlertext(e: Exception): String = when (e) {
        is java.net.UnknownHostException, is java.net.ConnectException ->
            "Keine Verbindung zum Server."
        else -> e.message ?: "Unbekannter Fehler"
    }
}

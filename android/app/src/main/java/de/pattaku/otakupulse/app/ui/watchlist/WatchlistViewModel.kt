package de.pattaku.otakupulse.app.ui.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.pattaku.otakupulse.app.data.WatchlistRepository
import de.pattaku.otakupulse.app.data.local.WatchStatus
import de.pattaku.otakupulse.app.data.local.WatchlistEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WatchlistViewModel(private val repository: WatchlistRepository) : ViewModel() {

    val entries: StateFlow<List<WatchlistEntry>> = repository.all()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun folgeAbhaken(entry: WatchlistEntry) = viewModelScope.launch {
        val neu = entry.progress + 1
        repository.setProgress(entry.animeId, neu)
        // Letzte Folge abgehakt? Dann ist der Titel durch.
        if (entry.episodes != null && neu >= entry.episodes) {
            repository.setStatus(entry.animeId, WatchStatus.COMPLETED)
        }
    }

    fun setzeStatus(animeId: Int, status: WatchStatus) = viewModelScope.launch {
        repository.setStatus(animeId, status)
    }

    fun entfernen(animeId: Int) = viewModelScope.launch { repository.remove(animeId) }
}

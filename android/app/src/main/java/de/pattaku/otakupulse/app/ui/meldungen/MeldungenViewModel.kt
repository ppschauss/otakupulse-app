package de.pattaku.otakupulse.app.ui.meldungen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.pattaku.otakupulse.app.data.local.AppDatabase
import de.pattaku.otakupulse.app.data.local.Meldung
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MeldungenViewModel(private val db: AppDatabase) : ViewModel() {

    val meldungen: StateFlow<List<Meldung>> = db.meldungen().alle()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val ungelesen: StateFlow<Int> = db.meldungen().ungelesen()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun alleGelesen() = viewModelScope.launch { db.meldungen().alleGelesen() }

    fun leeren() = viewModelScope.launch { db.meldungen().leeren() }
}

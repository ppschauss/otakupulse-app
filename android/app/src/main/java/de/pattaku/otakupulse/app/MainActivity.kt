package de.pattaku.otakupulse.app

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import de.pattaku.otakupulse.app.di.AppContainer
import de.pattaku.otakupulse.app.domain.Anime
import de.pattaku.otakupulse.app.ui.detail.DetailScreen
import de.pattaku.otakupulse.app.ui.swipe.SwipeScreen
import de.pattaku.otakupulse.app.ui.swipe.SwipeViewModel
import de.pattaku.otakupulse.app.ui.theme.CompanionTheme

class CompanionApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CompanionApplication).container

        setContent {
            CompanionTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(Modifier.padding(innerPadding)) {
                        Root(container)
                    }
                }
            }
        }
    }
}

/**
 * Vorläufige Navigation: Stapel und Detailansicht.
 * Die Bottom-Navigation (Watchlist, Kalender, Party) kommt mit M3 bis M5 dazu.
 */
@Composable
private fun Root(container: AppContainer) {
    var detail by remember { mutableStateOf<Anime?>(null) }
    val viewModel: SwipeViewModel = viewModel(factory = factory(container))

    val offen = detail
    if (offen != null) {
        DetailScreen(anime = offen, onBack = { detail = null })
    } else {
        SwipeScreen(viewModel = viewModel, onOpenDetail = { detail = it })
    }
}

/** Manuelle ViewModel-Erzeugung — passend zur manuellen Abhängigkeitsverdrahtung. */
private fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SwipeViewModel(container.deckRepository) as T
}

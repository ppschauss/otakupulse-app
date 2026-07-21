package de.pattaku.otakupulse.app

import android.app.Application
import kotlinx.coroutines.launch
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import de.pattaku.otakupulse.app.di.AppContainer
import de.pattaku.otakupulse.app.domain.Anime
import de.pattaku.otakupulse.app.ui.detail.DetailScreen
import de.pattaku.otakupulse.app.ui.swipe.SwipeScreen
import de.pattaku.otakupulse.app.ui.swipe.SwipeViewModel
import de.pattaku.otakupulse.app.ui.party.PartyScreen
import de.pattaku.otakupulse.app.ui.party.PartyViewModel
import de.pattaku.otakupulse.app.ui.settings.ServerScreen
import de.pattaku.otakupulse.app.ui.settings.ServerViewModel
import de.pattaku.otakupulse.app.ui.theme.CompanionTheme
import de.pattaku.otakupulse.app.ui.watchlist.WatchlistScreen
import de.pattaku.otakupulse.app.ui.watchlist.WatchlistViewModel

class CompanionApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Gespeicherte Serveradresse früh in den Zwischenspeicher holen — der
        // Interceptor liest sie synchron und fiele sonst auf den Build-Standard zurück.
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            container.settingsStore.load()
            container.tokenStore.token()
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CompanionApplication).container

        setContent {
            CompanionTheme {
                Root(container)
            }
        }
    }
}

private data class Ziel(val label: String, val icon: ImageVector)

// Kalender und Party kommen mit M4/M5 dazu.
private val ZIELE = listOf(
    Ziel("Entdecken", Icons.Default.Style),
    Ziel("Watchlist", Icons.Default.Bookmark),
    Ziel("Party", Icons.Default.Groups),
    Ziel("Server", Icons.Default.Dns),
)

@Composable
private fun Root(container: AppContainer) {
    var ziel by remember { mutableIntStateOf(0) }
    var detail by remember { mutableStateOf<Anime?>(null) }

    val offen = detail
    if (offen != null) {
        DetailScreen(anime = offen, onBack = { detail = null })
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                ZIELE.forEachIndexed { index, z ->
                    NavigationBarItem(
                        selected = ziel == index,
                        onClick = { ziel = index },
                        icon = { Icon(z.icon, contentDescription = z.label) },
                        label = { Text(z.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            when (ziel) {
                0 -> SwipeScreen(
                    viewModel = viewModel(factory = swipeFactory(container)),
                    onOpenDetail = { detail = it },
                )
                1 -> WatchlistScreen(
                    viewModel = viewModel(factory = watchlistFactory(container)),
                )
                2 -> PartyScreen(
                    viewModel = viewModel(factory = partyFactory(container)),
                )
                else -> ServerScreen(
                    viewModel = viewModel(factory = serverFactory(container)),
                )
            }
        }
    }
}

/** Manuelle ViewModel-Erzeugung — passend zur manuellen Abhängigkeitsverdrahtung. */
private fun swipeFactory(container: AppContainer) = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SwipeViewModel(
            container.deckRepository,
            container.watchlistRepository,
            container.applicationContext,
        ) as T
}

private fun partyFactory(container: AppContainer) = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        PartyViewModel(container.api) as T
}

private fun serverFactory(container: AppContainer) = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ServerViewModel(container.settingsStore, container.api, container.deckRepository) as T
}

private fun watchlistFactory(container: AppContainer) = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        WatchlistViewModel(container.watchlistRepository) as T
}

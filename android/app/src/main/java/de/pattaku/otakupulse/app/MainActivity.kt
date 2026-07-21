package de.pattaku.otakupulse.app

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.pattaku.otakupulse.app.di.AppContainer
import de.pattaku.otakupulse.app.domain.Anime
import de.pattaku.otakupulse.app.ui.calendar.CalendarScreen
import de.pattaku.otakupulse.app.ui.calendar.CalendarViewModel
import de.pattaku.otakupulse.app.ui.detail.DetailScreen
import de.pattaku.otakupulse.app.ui.meldungen.MeldungenScreen
import de.pattaku.otakupulse.app.ui.meldungen.MeldungenViewModel
import de.pattaku.otakupulse.app.ui.party.PartyScreen
import de.pattaku.otakupulse.app.ui.party.PartyViewModel
import de.pattaku.otakupulse.app.ui.settings.EinstellungenScreen
import de.pattaku.otakupulse.app.ui.settings.ServerViewModel
import de.pattaku.otakupulse.app.ui.swipe.SwipeScreen
import de.pattaku.otakupulse.app.ui.swipe.SwipeViewModel
import de.pattaku.otakupulse.app.ui.theme.CompanionTheme
import de.pattaku.otakupulse.app.ui.theme.IntroScreen
import de.pattaku.otakupulse.app.ui.theme.ThemeModus
import de.pattaku.otakupulse.app.ui.watchlist.WatchlistScreen
import de.pattaku.otakupulse.app.ui.watchlist.WatchlistViewModel
import de.pattaku.otakupulse.app.work.EpisodeCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CompanionApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Gespeicherte Serveradresse früh in den Zwischenspeicher holen — der
        // Interceptor liest sie synchron und fiele sonst auf den Build-Standard zurück.
        CoroutineScope(Dispatchers.IO).launch {
            container.settingsStore.load()
            container.tokenStore.token()
            container.meldeFcmTokenFallsMoeglich()
        }
        EpisodeCheckWorker.planen(this)
    }
}

class MainActivity : ComponentActivity() {

    private val frageBenachrichtigung =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* Ablehnen ist ok */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CompanionApplication).container

        // Ab Android 13 muss man fragen. Eine Ablehnung ist kein Beinbruch: die
        // Meldungsliste in der App zeigt dieselbe Information weiterhin an.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            frageBenachrichtigung.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val modus by container.themeStore.modus.collectAsStateWithLifecycle(ThemeModus.SYSTEM)
            var introLaeuft by remember { mutableStateOf(true) }

            CompanionTheme(modus = modus) {
                if (introLaeuft) {
                    IntroScreen(onFertig = { introLaeuft = false })
                } else {
                    Root(container, modus)
                }
            }
        }
    }
}

private data class Ziel(val label: String, val icon: ImageVector)

private val ZIELE = listOf(
    Ziel("Entdecken", Icons.Default.Style),
    Ziel("Watchlist", Icons.Default.Bookmark),
    Ziel("Kalender", Icons.Default.CalendarMonth),
    Ziel("Party", Icons.Default.Groups),
    Ziel("Meldungen", Icons.Default.Notifications),
)  // Material erlaubt drei bis fünf Ziele; Einstellungen sitzen deshalb in der Kopfleiste.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Root(container: AppContainer, modus: ThemeModus) {
    val bereich = rememberCoroutineScope()
    var einstellungenOffen by remember { mutableStateOf(false) }
    var ziel by remember { mutableIntStateOf(0) }
    var detail by remember { mutableStateOf<Anime?>(null) }
    var ladeId by remember { mutableStateOf<Int?>(null) }

    val meldungenVm: MeldungenViewModel = viewModel(factory = meldungenFactory(container))
    val ungelesen by meldungenVm.ungelesen.collectAsStateWithLifecycle()

    // Detailansicht aus Watchlist oder Meldung: dort liegt nur die ID vor,
    // der vollständige Titel muss nachgeladen werden.
    LaunchedEffect(ladeId) {
        val id = ladeId ?: return@LaunchedEffect
        runCatching { container.deckRepository.detail(id) }.onSuccess { detail = it }
        ladeId = null
    }

    val offen = detail
    if (offen != null) {
        DetailScreen(anime = offen, onBack = { detail = null })
        return
    }

    if (einstellungenOffen) {
        EinstellungenScreen(
            modus = modus,
            onModus = { neu -> bereich.launch { container.themeStore.setze(neu) } },
            serverViewModel = viewModel(factory = serverFactory(container)),
            onBack = { einstellungenOffen = false },
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(ZIELE[ziel].label) },
                actions = {
                    IconButton(onClick = { einstellungenOffen = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                ZIELE.forEachIndexed { index, z ->
                    NavigationBarItem(
                        selected = ziel == index,
                        onClick = { ziel = index },
                        icon = {
                            if (z.label == "Meldungen" && ungelesen > 0) {
                                BadgedBox(badge = { Badge { Text("$ungelesen") } }) {
                                    Icon(z.icon, contentDescription = z.label)
                                }
                            } else {
                                Icon(z.icon, contentDescription = z.label)
                            }
                        },
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
                    onOeffneAnime = { ladeId = it },
                )
                2 -> CalendarScreen(
                    viewModel = viewModel(factory = calendarFactory(container)),
                )
                3 -> PartyScreen(
                    viewModel = viewModel(factory = partyFactory(container)),
                )
                else -> MeldungenScreen(
                    viewModel = meldungenVm,
                    onOeffneAnime = { ladeId = it },
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
            container.filterStore,
            container.api,
            container.applicationContext,
        ) as T
}

private fun calendarFactory(container: AppContainer) = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CalendarViewModel(container.airingRepository) as T
}

private fun partyFactory(container: AppContainer) = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        PartyViewModel(container.api) as T
}

private fun meldungenFactory(container: AppContainer) = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        MeldungenViewModel(container.database) as T
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

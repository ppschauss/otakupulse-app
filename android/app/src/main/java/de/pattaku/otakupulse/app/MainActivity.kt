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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.pattaku.otakupulse.app.di.AppContainer
import de.pattaku.otakupulse.app.data.api.AnimeDetailDto
import de.pattaku.otakupulse.app.ui.calendar.CalendarScreen
import de.pattaku.otakupulse.app.ui.calendar.CalendarViewModel
import de.pattaku.otakupulse.app.ui.detail.DetailScreen
import de.pattaku.otakupulse.app.ui.meldungen.MeldungenScreen
import de.pattaku.otakupulse.app.ui.meldungen.MeldungenViewModel
import de.pattaku.otakupulse.app.ui.party.PartyScreen
import de.pattaku.otakupulse.app.ui.party.PartyViewModel
import de.pattaku.otakupulse.app.ui.settings.EinstellungenScreen
import de.pattaku.otakupulse.app.ui.swipe.SwipeScreen
import de.pattaku.otakupulse.app.ui.swipe.SwipeViewModel
import de.pattaku.otakupulse.app.ui.theme.CompanionTheme
import de.pattaku.otakupulse.app.ui.theme.Breite
import de.pattaku.otakupulse.app.ui.theme.IntroScreen
import de.pattaku.otakupulse.app.ui.theme.LocalBreite
import de.pattaku.otakupulse.app.ui.theme.inhaltsBreite
import de.pattaku.otakupulse.app.ui.theme.zuBreite
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
        // Token früh in den Zwischenspeicher holen — der Interceptor liest es
        // synchron und schickte sonst die erste Anfrage ohne Anmeldung los.
        CoroutineScope(Dispatchers.IO).launch {
            container.tokenStore.token()
            container.meldeFcmTokenFallsMoeglich()
        }
        EpisodeCheckWorker.planen(this)
    }
}

class MainActivity : ComponentActivity() {

    private var sicherungsMeldung by mutableStateOf<String?>(null)

    /** Speichern-Dialog des Systems — kein eigener Dateibrowser nötig. */
    private val exportZiel =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri == null) return@registerForActivityResult
            val container = (application as CompanionApplication).container
            lifecycleScope.launch {
                sicherungsMeldung = runCatching {
                    val inhalt = container.backupRepository.exportieren()
                    contentResolver.openOutputStream(uri)?.use { it.write(inhalt.toByteArray()) }
                    "Gesichert."
                }.getOrElse { "Sichern fehlgeschlagen: ${it.message}" }
            }
        }

    private val importQuelle =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@registerForActivityResult
            val container = (application as CompanionApplication).container
            lifecycleScope.launch {
                sicherungsMeldung = runCatching {
                    val inhalt = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: error("Datei nicht lesbar")
                    val ergebnis = container.backupRepository.importieren(inhalt)
                    "${ergebnis.neu} neu, ${ergebnis.aktualisiert} aktualisiert " +
                        "(von ${ergebnis.gesamt} in der Datei)."
                }.getOrElse { "Import fehlgeschlagen: ${it.message}" }
            }
        }

    private val frageBenachrichtigung =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* Ablehnen ist ok */ }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
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
            // Aus der Fensterbreite, nicht aus dem Gerätemodell: ein Tablet im
            // geteilten Bildschirm ist so schmal wie ein Handy.
            val breite = calculateWindowSizeClass(this).widthSizeClass.zuBreite()
            var introLaeuft by remember { mutableStateOf(true) }

            CompositionLocalProvider(LocalBreite provides breite) {
            CompanionTheme(modus = modus) {
                // Surface malt den Hintergrund des Farbschemas. Ohne diese Ebene
                // scheint bei Bildschirmen ausserhalb des Scaffold der
                // XML-Fensterhintergrund durch.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    if (introLaeuft) {
                        IntroScreen(onFertig = { introLaeuft = false })
                    } else {
                        Root(
                            container = container,
                            modus = modus,
                            sicherungsMeldung = sicherungsMeldung,
                            onExport = {
                                sicherungsMeldung = null
                                exportZiel.launch("otakupulse-watchlist.json")
                            },
                            onImport = {
                                sicherungsMeldung = null
                                importQuelle.launch(arrayOf("application/json", "text/plain"))
                            },
                        )
                    }
                }
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
private fun Root(
    container: AppContainer,
    modus: ThemeModus,
    sicherungsMeldung: String?,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    val bereich = rememberCoroutineScope()
    var einstellungenOffen by remember { mutableStateOf(false) }
    var ziel by remember { mutableIntStateOf(0) }
    var detail by remember { mutableStateOf<AnimeDetailDto?>(null) }
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
        DetailScreen(
            anime = offen,
            onBack = { detail = null },
            onOeffneAnime = { detail = null; ladeId = it },
        )
        return
    }

    if (einstellungenOffen) {
        EinstellungenScreen(
            modus = modus,
            onModus = { neu -> bereich.launch { container.themeStore.setze(neu) } },
            sicherungsMeldung = sicherungsMeldung,
            onExport = onExport,
            onImport = onImport,
            onBack = { einstellungenOffen = false },
        )
        return
    }

    val breite = LocalBreite.current
    val schiene = breite != Breite.KOMPAKT

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
            // Ab mittlerer Breite übernimmt die Schiene an der Seite; eine Leiste
            // unten wäre dort quer über den halben Bildschirm gezogen.
            if (!schiene) {
                NavigationBar {
                    ZIELE.forEachIndexed { index, z ->
                        NavigationBarItem(
                            selected = ziel == index,
                            onClick = { ziel = index },
                            icon = { ZielSymbol(z, ungelesen) },
                            label = { Text(z.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Row(Modifier.padding(innerPadding)) {
            if (schiene) {
                NavigationRail {
                    ZIELE.forEachIndexed { index, z ->
                        NavigationRailItem(
                            selected = ziel == index,
                            onClick = { ziel = index },
                            icon = { ZielSymbol(z, ungelesen) },
                            label = { Text(z.label) },
                        )
                    }
                }
            }
            Box(
                Modifier
                    // weight statt fillMaxWidth: die Schiene links behält ihren Platz.
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Box(
                    // Auf breiten Anzeigen mittig begrenzen — über die volle Breite
                    // gesetzter Text verliert beim Zeilenwechsel den Anschluss.
                    Modifier.widthIn(max = breite.inhaltsBreite),
                ) {
            when (ziel) {
                0 -> SwipeScreen(
                    viewModel = viewModel(factory = swipeFactory(container)),
                    onOpenDetail = { ladeId = it.id },
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
    }
}

@Composable
private fun ZielSymbol(z: Ziel, ungelesen: Int) {
    if (z.label == "Meldungen" && ungelesen > 0) {
        BadgedBox(badge = { Badge { Text("$ungelesen") } }) {
            Icon(z.icon, contentDescription = z.label)
        }
    } else {
        Icon(z.icon, contentDescription = z.label)
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

private fun watchlistFactory(container: AppContainer) = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        WatchlistViewModel(container.watchlistRepository) as T
}

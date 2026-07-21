package de.pattaku.otakupulse.app.ui.meldungen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.pattaku.otakupulse.app.data.local.Meldung
import de.pattaku.otakupulse.app.ui.theme.SwipeSuper
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ZEITPUNKT = DateTimeFormatter.ofPattern("EEE, d. MMM · HH:mm", Locale.GERMAN)

@Composable
fun MeldungenScreen(viewModel: MeldungenViewModel, onOeffneAnime: (Int) -> Unit) {
    val meldungen by viewModel.meldungen.collectAsStateWithLifecycle()

    // Beim Betreten als gelesen markieren — das Abzeichen soll verschwinden,
    // sobald man hingeschaut hat.
    LaunchedEffect(Unit) { viewModel.alleGelesen() }

    if (meldungen.isEmpty()) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text(
                "Noch nichts passiert.\n\nHier landen Super-Swipes aus deinen Partys " +
                    "und Hinweise auf neue Folgen — auch dann, wenn du die " +
                    "Android-Benachrichtigung schon weggewischt hast.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(32.dp),
            )
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Meldungen",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = viewModel::leeren) { Text("Leeren") }
            }
        }

        items(meldungen, key = { it.id }) { meldung ->
            MeldungsKarte(meldung, onKlick = { meldung.animeId?.let(onOeffneAnime) })
        }
    }
}

@Composable
private fun MeldungsKarte(meldung: Meldung, onKlick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = meldung.animeId != null, onClick = onKlick),
        colors = if (meldung.gelesen) {
            CardDefaults.cardColors()
        } else {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        },
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(symbolFarbe(meldung.art).copy(alpha = 0.18f))
                    .padding(8.dp),
            ) {
                Icon(
                    if (meldung.art == ART_SUPER_SWIPE) Icons.Default.Star else Icons.Default.NewReleases,
                    contentDescription = null,
                    tint = symbolFarbe(meldung.art),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(meldung.titel, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(meldung.text, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    Instant.ofEpochMilli(meldung.empfangenAm)
                        .atZone(ZoneId.systemDefault())
                        .format(ZEITPUNKT),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

const val ART_SUPER_SWIPE = "SUPER_SWIPE"
const val ART_NEUE_FOLGE = "NEUE_FOLGE"

@Composable
private fun symbolFarbe(art: String) =
    if (art == ART_SUPER_SWIPE) SwipeSuper else MaterialTheme.colorScheme.primary

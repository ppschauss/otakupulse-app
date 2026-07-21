package de.pattaku.otakupulse.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Vorschläge für die üblichen Wege zum Server — spart Tipparbeit auf dem Handy. */
private val VORSCHLAEGE = listOf(
    "https://app.otakupulse.de" to "Öffentlich",
    "http://192.168.0.161:3005" to "WLAN",
    "http://100.87.113.82:3005" to "Tailscale",
)

/** Server-Einstellung als Abschnitt innerhalb der Einstellungen. */
@Composable
fun ServerAbschnitt(viewModel: ServerViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxWidth()) {
        Text("Server", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Wo läuft das Backend? Zu Hause geht die WLAN-Adresse, von unterwegs die " +
                "öffentliche Domain — sofern sie schon eingerichtet ist.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = state.url,
            onValueChange = viewModel::setzeUrl,
            label = { Text("Adresse") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))
        Row {
            VORSCHLAEGE.forEach { (url, label) ->
                AssistChip(
                    onClick = { viewModel.setzeUrl(url) },
                    label = { Text(label) },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Row {
            Button(onClick = viewModel::pruefenUndSpeichern, enabled = !state.pruefend) {
                Text(if (state.pruefend) "Prüfe…" else "Verbinden")
            }
            if (state.pruefend) {
                Spacer(Modifier.height(0.dp))
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .height(24.dp),
                    strokeWidth = 2.dp,
                )
            }
        }

        state.meldung?.let {
            Spacer(Modifier.height(16.dp))
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.erfolgreich) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        }
    }
}

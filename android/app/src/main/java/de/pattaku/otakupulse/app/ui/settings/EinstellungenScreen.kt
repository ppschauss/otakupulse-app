package de.pattaku.otakupulse.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.pattaku.otakupulse.app.ui.theme.ThemeModus

private val MODI = listOf(
    Triple(ThemeModus.SYSTEM, "System", Icons.Default.Brightness6),
    Triple(ThemeModus.HELL, "Hell", Icons.Default.LightMode),
    Triple(ThemeModus.DUNKEL, "Dunkel", Icons.Default.DarkMode),
)

@Composable
fun EinstellungenScreen(
    modus: ThemeModus,
    onModus: (ThemeModus) -> Unit,
    serverViewModel: ServerViewModel,
    onBack: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            androidx.compose.material3.IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Zurück",
                )
            }
            Text(
                "Einstellungen",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(20.dp))
        Column(Modifier.padding(horizontal = 12.dp)) {
        Text("Darstellung", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            MODI.forEachIndexed { index, (wert, label, icon) ->
                SegmentedButton(
                    selected = modus == wert,
                    onClick = { onModus(wert) },
                    shape = SegmentedButtonDefaults.itemShape(index, MODI.size),
                    icon = { Icon(icon, contentDescription = null) },
                    label = { Text(label) },
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Dunkel passt zur Website und lässt die Cover kräftiger wirken.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(28.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        ServerAbschnitt(serverViewModel)
        }
    }
}

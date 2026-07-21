package de.pattaku.otakupulse.app.ui.swipe

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import de.pattaku.otakupulse.app.data.api.PartyDto
import de.pattaku.otakupulse.app.domain.Anime

/**
 * Fragt beim Super-Swipe, welche Partys benachrichtigt werden.
 *
 * Erscheint nur bei mehr als einer Party — bei genau einer wäre die Frage bloß
 * ein zusätzlicher Klick. Alle sind vorausgewählt, das ist der häufigste Fall.
 */
@Composable
fun SuperSwipeDialog(
    anime: Anime,
    partys: List<PartyDto>,
    onSenden: (List<Int>) -> Unit,
    onAbbrechen: () -> Unit,
) {
    var gewaehlt by remember { mutableStateOf(partys.map { it.id }.toSet()) }

    AlertDialog(
        onDismissRequest = onAbbrechen,
        title = { Text("An welche Party?") },
        text = {
            Column {
                Text(
                    "\"${anime.title}\" geht als Super-Swipe raus — alle dort " +
                        "bekommen eine Benachrichtigung.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))

                partys.forEach { party ->
                    val aktiv = party.id in gewaehlt
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = aktiv,
                                role = Role.Checkbox,
                                onValueChange = {
                                    gewaehlt = if (aktiv) gewaehlt - party.id else gewaehlt + party.id
                                },
                            )
                            .height(48.dp),  // Material-Mindestgröße für Tippziele
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = aktiv, onCheckedChange = null)
                        Spacer(Modifier.height(0.dp))
                        Column {
                            Text(party.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${party.members.size} Mitglieder · ${party.matches.size} Matches",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSenden(gewaehlt.toList()) },
                enabled = gewaehlt.isNotEmpty(),
            ) { Text("Senden") }
        },
        dismissButton = {
            // Kein echtes Abbrechen: der Titel bleibt gemerkt, nur ohne Benachrichtigung.
            TextButton(onClick = onAbbrechen) { Text("Nur merken") }
        },
    )
}

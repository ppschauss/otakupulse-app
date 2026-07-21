package de.pattaku.otakupulse.app.ui.party

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import de.pattaku.otakupulse.app.data.api.PartyDto

@Composable
fun PartyScreen(viewModel: PartyViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Party", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Wischt ihr beide denselben Titel nach rechts, entsteht ein Match. " +
                    "Nach oben wischen schickt allen eine Benachrichtigung.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        state.fehler?.let { fehler ->
            item {
                Text(fehler, color = MaterialTheme.colorScheme.error)
            }
        }

        if (state.laden) {
            item { Box(Modifier.fillMaxWidth(), Alignment.Center) { CircularProgressIndicator() } }
        }

        items(state.parties, key = { it.id }) { party -> PartyKarte(party) }

        item {
            Spacer(Modifier.height(8.dp))
            Card {
                Column(Modifier.padding(14.dp)) {
                    Text("Beitreten", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it.uppercase() },
                            label = { Text("Code") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.beitreten(code); code = "" },
                            enabled = code.length >= 4,
                        ) { Text("Los") }
                    }
                }
            }
        }

        item {
            Card {
                Column(Modifier.padding(14.dp)) {
                    Text("Neue Party", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { viewModel.anlegen(name); name = "" }) { Text("Anlegen") }
                    }
                }
            }
        }

        item {
            OutlinedButton(onClick = viewModel::aktualisieren, modifier = Modifier.fillMaxWidth()) {
                Text("Aktualisieren")
            }
        }
    }
}

@Composable
private fun PartyKarte(party: PartyDto) {
    Card {
        Column(Modifier.padding(14.dp)) {
            Text(party.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))

            // Der Code ist das, was man Freunden vorliest — deshalb gross und breit gesperrt.
            Text(
                party.joinCode,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.height(6.dp))
            Text(
                party.members.joinToString(", ") { if (it.isMe) "${it.displayName} (du)" else it.displayName },
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(Modifier.height(12.dp))
            if (party.matches.isEmpty()) {
                Text(
                    "Noch keine Matches. Wischt beide denselben Titel nach rechts.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Start,
                )
            } else {
                Text("Matches (${party.matches.size})", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(party.matches, key = { it.id }) { anime ->
                        Column(Modifier.width(84.dp)) {
                            AsyncImage(
                                model = anime.coverImageUrl,
                                contentDescription = anime.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 84.dp, height = 118.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                anime.title,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 2,
                            )
                        }
                    }
                }
            }
        }
    }
}

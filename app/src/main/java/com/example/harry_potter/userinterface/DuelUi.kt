package com.example.harry_potter.userinterface

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.harry_potter.data.BattleCatalog
import com.example.harry_potter.data.BattleEngine
import com.example.harry_potter.data.BattleResultEntity
import com.example.harry_potter.data.BattleSpell
import com.example.harry_potter.data.Difficulty
import com.example.harry_potter.data.Duelist
import com.example.harry_potter.data.GesturePoint
import com.example.harry_potter.viewmodel.BattleViewModel
import com.example.harry_potter.viewmodel.DuelPhase
import com.example.harry_potter.viewmodel.DuelUiState

@Composable
fun DuelScreen(viewModel: BattleViewModel, userId: Long) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val history by remember(userId) { viewModel.history(userId) }.collectAsStateWithLifecycle(emptyList())
    when (val phase = state.phase) {
        DuelPhase.Setup -> DuelSetup(
            state = state,
            history = history,
            onToggleSpell = viewModel::toggleSpell,
            onDifficulty = viewModel::setDifficulty,
            onStart = { viewModel.startDuel(userId) }
        )
        DuelPhase.PlayerTurn, DuelPhase.OpponentTurn -> DuelArena(state, viewModel::gestureFor, viewModel::castGesture)
        is DuelPhase.Finished -> DuelResult(state, phase.won, viewModel::rematch, viewModel::backToSetup)
    }
}

@Composable
private fun DuelSetup(
    state: DuelUiState,
    history: List<BattleResultEntity>,
    onToggleSpell: (String) -> Unit,
    onDifficulty: (Difficulty) -> Unit,
    onStart: () -> Unit
) {
    val selectedCount = state.selected.size
    val deckReady = BattleEngine.isValidDeck(state.selectedSpells)
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Duelo de Feiticeiros", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                DuelHistory(history)
            }
            item { Text("Oponente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(Difficulty.entries) { difficulty ->
                Row(
                    Modifier.fillMaxWidth().clickable { onDifficulty(difficulty) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = state.difficulty == difficulty, onClick = { onDifficulty(difficulty) })
                    Column {
                        Text("${difficulty.label}: ${difficulty.opponentName}", fontWeight = FontWeight.Bold)
                        Text("${difficulty.turnSeconds} s para desenhar por turno", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                Text(
                    "Seu grimório ($selectedCount/${BattleEngine.DECK_SIZE})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("Escolha ${BattleEngine.DECK_SIZE} feitiços. Pelo menos um precisa causar dano.", style = MaterialTheme.typography.bodySmall)
            }
            items(BattleCatalog.spells, key = { it.key }) { spell ->
                val checked = spell.key in state.selected
                val enabled = checked || selectedCount < BattleEngine.DECK_SIZE
                Card(Modifier.fillMaxWidth().clickable(enabled = enabled) { onToggleSpell(spell.key) }) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = checked, onCheckedChange = { onToggleSpell(spell.key) }, enabled = enabled)
                        Column {
                            Text(spell.name, fontWeight = FontWeight.Bold)
                            Text(spell.description(), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        Button(onClick = onStart, enabled = deckReady, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(if (deckReady) "Começar duelo" else "Escolha $selectedCount/${BattleEngine.DECK_SIZE} feitiços")
        }
    }
}

@Composable
private fun DuelHistory(history: List<BattleResultEntity>) {
    val wins = history.count { it.won }
    Text("Vitórias $wins · Derrotas ${history.size - wins}", color = MaterialTheme.colorScheme.primary)
    history.take(5).forEach { result ->
        Text(
            "${if (result.won) "Vitória" else "Derrota"} contra ${result.opponentName} (${result.difficulty}) em ${result.turns} turnos",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun DuelArena(
    state: DuelUiState,
    gestureFor: (BattleSpell) -> List<GesturePoint>,
    onCast: (List<GesturePoint>) -> Unit
) {
    var points by remember { mutableStateOf(emptyList<GesturePoint>()) }
    var previewSpell by remember { mutableStateOf<BattleSpell?>(null) }
    val isPlayerTurn = state.phase == DuelPhase.PlayerTurn
    LaunchedEffect(state.turn, state.phase) { points = emptyList() }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DuelistPanel(state.opponent)
        DuelistPanel(state.player)
        if (isPlayerTurn) {
            val total = state.difficulty.turnSeconds
            val urgent = state.secondsLeft <= 3
            Text("Tempo: ${state.secondsLeft} s", color = if (urgent) MaterialTheme.colorScheme.error else Color.Unspecified)
            LinearProgressIndicator(
                progress = { state.secondsLeft / total.toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = if (urgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
        state.message?.let { Text(it, fontWeight = FontWeight.Bold) }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(state.player.deck, key = { it.key }) { spell ->
                AssistChip(onClick = { previewSpell = spell }, label = { Text(spell.name) })
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            GestureCanvas(
                points = points,
                onPointsChange = { points = it },
                onStrokeComplete = onCast,
                modifier = Modifier.fillMaxSize()
            )
            if (!isPlayerTurn) {
                Text(
                    "Vez de ${state.opponent.name}...",
                    modifier = Modifier.align(Alignment.Center)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = .85f))
                        .padding(12.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        DuelLog(state.log.takeLast(4))
    }

    previewSpell?.let { spell ->
        AlertDialog(
            onDismissRequest = { previewSpell = null },
            confirmButton = { TextButton(onClick = { previewSpell = null }) { Text("Fechar") } },
            title = { Text(spell.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(spell.description())
                    GesturePreview(gestureFor(spell))
                }
            }
        )
    }
}

@Composable
private fun DuelistPanel(duelist: Duelist) {
    val statuses = listOfNotNull(
        "Escudo".takeIf { duelist.shielded },
        "Atordoado".takeIf { duelist.stunned },
        "Enfraquecido".takeIf { duelist.weakened },
        "Fortalecido".takeIf { duelist.empowered }
    )
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(duelist.name, fontWeight = FontWeight.Bold)
                Text("${duelist.hp}/${BattleEngine.MAX_HP} HP")
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { duelist.hp / BattleEngine.MAX_HP.toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = if (duelist.hp <= 30) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            if (statuses.isNotEmpty()) {
                Text(statuses.joinToString(" · "), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun DuelLog(lines: List<String>) {
    Column {
        lines.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun DuelResult(state: DuelUiState, won: Boolean, onRematch: () -> Unit, onChangeDeck: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Text(
            if (won) "Vitória!" else "Derrota",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = if (won) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        Text("Contra ${state.opponent.name} em ${state.turn} turnos", textAlign = TextAlign.Center)
        Text("Você: ${state.player.hp} HP · ${state.opponent.name}: ${state.opponent.hp} HP")
        DuelLog(state.log.takeLast(4))
        Button(onClick = onRematch, modifier = Modifier.fillMaxWidth()) { Text("Revanche") }
        OutlinedButton(onClick = onChangeDeck, modifier = Modifier.fillMaxWidth()) { Text("Trocar feitiços") }
    }
}

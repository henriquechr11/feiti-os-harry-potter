package com.example.harry_potter.userinterface

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.harry_potter.data.FavoriteEntity
import com.example.harry_potter.data.Spell
import com.example.harry_potter.viewmodel.AuthUiState
import com.example.harry_potter.viewmodel.BattleViewModel
import com.example.harry_potter.viewmodel.SpellLoadState
import com.example.harry_potter.viewmodel.SpellViewModel
import kotlinx.serialization.Serializable

@Serializable data object SpellListRoute
@Serializable data object DrawRoute
@Serializable data object FavoritesRoute
@Serializable data object DuelRoute
@Serializable data class DetailRoute(val spellId: String)

@Composable
fun SpellbookApp(viewModel: SpellViewModel, battleViewModel: BattleViewModel) {
    val auth by viewModel.auth.collectAsStateWithLifecycle()
    val userId = auth.userId
    if (userId == null) {
        AuthScreen(auth, viewModel::login, viewModel::register)
    } else {
        MainSpellbook(viewModel, battleViewModel, userId)
    }
}

@Composable
private fun AuthScreen(
    state: AuthUiState,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String) -> Unit
) {
    var isRegister by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var email by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var password by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Varinha Mágica", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text("Entre para guardar seus feitiços favoritos.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("E-mail") }, singleLine = true)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Senha") },
            visualTransformation = PasswordVisualTransformation(), singleLine = true
        )
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 10.dp)) }
        Spacer(Modifier.height(18.dp))
        Button(
            modifier = Modifier.fillMaxWidth(), enabled = !state.isWorking,
            onClick = { if (isRegister) onRegister(email, password) else onLogin(email, password) }
        ) { Text(if (state.isWorking) "Aguarde..." else if (isRegister) "Criar conta" else "Entrar") }
        TextButton(onClick = { isRegister = !isRegister }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(if (isRegister) "Já tenho uma conta" else "Ainda não tenho conta")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainSpellbook(viewModel: SpellViewModel, battleViewModel: BattleViewModel, userId: Long) {
    val navController = rememberNavController()
    val currentRoute by navController.currentBackStackEntryFlow.collectAsState(initial = navController.currentBackStackEntry)
    val route = currentRoute?.destination?.route
    val isDetail = route?.contains("DetailRoute") == true
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isDetail) "Detalhe do feitiço" else "Varinha Mágica") },
                navigationIcon = { if (isDetail) TextButton(onClick = { navController.popBackStack() }) { Text("Voltar") } },
                actions = {
                    if (!isDetail) TextButton(onClick = { battleViewModel.reset(); viewModel.signOut() }) { Text("Sair") }
                }
            )
        },
        bottomBar = { if (!isDetail) SpellBottomBar(navController, route) }
    ) { padding ->
        NavHost(navController, startDestination = SpellListRoute, modifier = Modifier.padding(padding)) {
            composable<SpellListRoute> { SpellListScreen(viewModel) { navController.navigate(DetailRoute(it)) } }
            composable<DrawRoute> { DrawScreen(viewModel) { navController.navigate(DetailRoute(it)) } }
            composable<FavoritesRoute> { FavoriteScreen(viewModel) { navController.navigate(DetailRoute(it)) } }
            composable<DuelRoute> { DuelScreen(battleViewModel, userId) }
            composable<DetailRoute> {
                val detail = it.toRoute<DetailRoute>()
                SpellDetailScreen(viewModel, detail.spellId)
            }
        }
    }
}

@Composable
private fun SpellBottomBar(navController: NavHostController, currentRoute: String?) {
    val entries = listOf("Feitiços" to SpellListRoute, "Desenhar" to DrawRoute, "Favoritos" to FavoritesRoute, "Duelo" to DuelRoute)
    NavigationBar {
        entries.forEach { (label, destination) ->
            val selected = currentRoute?.contains(destination::class.simpleName.orEmpty()) == true
            NavigationBarItem(selected = selected, onClick = {
                navController.navigate(destination) { launchSingleTop = true; popUpTo(navController.graph.startDestinationId) { saveState = true }; restoreState = true }
            }, icon = { Text(if (selected) "•" else "○") }, label = { Text(label) })
        }
    }
}

@Composable
private fun SpellListScreen(viewModel: SpellViewModel, onOpenDetail: (String) -> Unit) {
    val state by viewModel.spells.collectAsStateWithLifecycle()
    val currentState = state
    when (currentState) {
        SpellLoadState.Loading -> LoadingPanel("Buscando feitiços...")
        is SpellLoadState.Error -> ErrorPanel(currentState.message, viewModel::loadSpells)
        is SpellLoadState.Success -> SpellList(currentState.spells, onOpenDetail)
    }
}

@Composable
private fun SpellList(spells: List<Spell>, onOpenDetail: (String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Feitiços selecionados", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        items(spells, key = { it.id }) { spell -> SpellCard(spell) { onOpenDetail(spell.id) } }
    }
}

@Composable
private fun SpellCard(spell: Spell, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp)) {
            Text(spell.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            spell.incantation?.takeIf { it.isNotBlank() }?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            Text(spell.category ?: "Tipo não informado", style = MaterialTheme.typography.labelMedium)
            Text(spell.effect ?: "Descrição não informada", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun FavoriteScreen(viewModel: SpellViewModel, onOpenDetail: (String) -> Unit) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    if (favorites.isEmpty()) {
        EmptyPanel("Ainda não há favoritos. Abra um feitiço e toque em Salvar favorito.")
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("Seus favoritos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            items(favorites, key = { it.spellId }) { favorite ->
                SpellCard(favorite.toSpell()) { onOpenDetail(favorite.spellId) }
            }
        }
    }
}

@Composable
private fun SpellDetailScreen(viewModel: SpellViewModel, spellId: String) {
    val state by viewModel.spells.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val spell = (state as? SpellLoadState.Success)?.spells?.firstOrNull { it.id == spellId }
        ?: favorites.firstOrNull { it.spellId == spellId }?.toSpell()
    if (spell == null) {
        EmptyPanel("Feitiço indisponível. Volte e tente atualizar a lista.")
        return
    }
    val isFavorite = favorites.any { it.spellId == spell.id }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text(spell.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            spell.incantation?.let { Text(it, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary) }
            Text(spell.category ?: "Tipo não informado")
            HorizontalDivider()
            Text(spell.effect ?: "Descrição não informada", style = MaterialTheme.typography.bodyLarge)
            spell.hand?.let { Text("Movimento descrito na fonte: $it", style = MaterialTheme.typography.bodyMedium) }
        }
        item {
            Text("Gesto do aplicativo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            GesturePreview(viewModel.gestureForSpell(spell.name, spell.incantation))
            Text("Desenhe esse traço de uma vez na aba Desenhar e retire o dedo para reconhecer.")
        }
        item {
            Button(onClick = { viewModel.toggleFavorite(spell, isFavorite) }) {
                Text(if (isFavorite) "Remover dos favoritos" else "Salvar favorito")
            }
        }
    }
}

private fun FavoriteEntity.toSpell() = Spell(spellId, spellName, incantation, category, effect, hand)

@Composable private fun LoadingPanel(text: String) = Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { CircularProgressIndicator(); Spacer(Modifier.height(16.dp)); Text(text) }
@Composable private fun EmptyPanel(text: String) = Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text(text) }
@Composable private fun ErrorPanel(text: String, retry: () -> Unit) = Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text(text); Spacer(Modifier.height(16.dp)); Button(onClick = retry) { Text("Tentar novamente") } }

package com.example.harry_potter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.harry_potter.data.BattleCatalog
import com.example.harry_potter.data.BattleEngine
import com.example.harry_potter.data.BattleRepository
import com.example.harry_potter.data.BattleResultEntity
import com.example.harry_potter.data.BattleSpell
import com.example.harry_potter.data.Difficulty
import com.example.harry_potter.data.DollarOneRecognizer
import com.example.harry_potter.data.Duelist
import com.example.harry_potter.data.GesturePoint
import com.example.harry_potter.data.TemplateRepository
import com.example.harry_potter.data.normalizedSpellName
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed interface DuelPhase {
    data object Setup : DuelPhase
    data object PlayerTurn : DuelPhase
    data object OpponentTurn : DuelPhase
    data class Finished(val won: Boolean) : DuelPhase
}

data class DuelUiState(
    val phase: DuelPhase = DuelPhase.Setup,
    val difficulty: Difficulty = Difficulty.EASY,
    val selected: List<String> = emptyList(),
    val player: Duelist = Duelist(PLAYER_NAME, emptyList()),
    val opponent: Duelist = Duelist(Difficulty.EASY.opponentName, emptyList()),
    val secondsLeft: Int = 0,
    val turn: Int = 0,
    val log: List<String> = emptyList(),
    val message: String? = null
) {
    val selectedSpells: List<BattleSpell> get() = selected.mapNotNull(BattleCatalog::byKey)
}

private const val PLAYER_NAME = "Você"

class BattleViewModel(
    private val battleRepository: BattleRepository,
    templateRepository: TemplateRepository
) : ViewModel() {
    private val gestureTemplates = templateRepository.all()
    private val recognizer = DollarOneRecognizer(gestureTemplates)
    private val random = Random.Default
    private var userId: Long? = null
    private var timerJob: Job? = null
    private var opponentJob: Job? = null

    private val _state = MutableStateFlow(DuelUiState())
    val state: StateFlow<DuelUiState> = _state.asStateFlow()

    fun history(userId: Long): Flow<List<BattleResultEntity>> = battleRepository.observeHistory(userId)

    fun gestureFor(spell: BattleSpell): List<GesturePoint> = gestureTemplates
        .firstOrNull { it.spellName.normalizedSpellName() == spell.key }?.points.orEmpty()

    fun toggleSpell(key: String) = _state.update { state ->
        when {
            key in state.selected -> state.copy(selected = state.selected - key)
            state.selected.size < BattleEngine.DECK_SIZE -> state.copy(selected = state.selected + key)
            else -> state
        }
    }

    fun setDifficulty(difficulty: Difficulty) = _state.update { it.copy(difficulty = difficulty) }

    fun startDuel(userId: Long) {
        val current = _state.value
        val deck = current.selectedSpells
        if (!BattleEngine.isValidDeck(deck)) return
        cancelJobs()
        this.userId = userId
        val opponentName = current.difficulty.opponentName
        _state.value = current.copy(
            player = Duelist(PLAYER_NAME, deck),
            opponent = Duelist(opponentName, BattleEngine.randomDeck(random)),
            turn = 0,
            log = listOf("O duelo contra $opponentName começou!"),
            message = null
        )
        beginPlayerTurn()
    }

    fun rematch() {
        userId?.let(::startDuel)
    }

    fun backToSetup() {
        cancelJobs()
        _state.update { it.copy(phase = DuelPhase.Setup, message = null) }
    }

    fun reset() {
        cancelJobs()
        userId = null
        _state.value = DuelUiState()
    }

    fun castGesture(points: List<GesturePoint>) {
        val current = _state.value
        if (current.phase != DuelPhase.PlayerTurn) return
        val recognition = recognizer.recognize(points)
        val spellName = recognition.spellName ?: run {
            _state.update { it.copy(message = recognition.message) }
            return
        }
        val spell = current.player.deck.firstOrNull { it.key == spellName.normalizedSpellName() } ?: run {
            _state.update { it.copy(message = "$spellName não está no seu grimório. Tente outro!") }
            return
        }
        timerJob?.cancel()
        val outcome = BattleEngine.cast(current.player, current.opponent, spell, recognition.score)
        _state.update { it.copy(player = outcome.caster, opponent = outcome.target, log = it.log + outcome.log) }
        if (!finishIfOver(playerCast = true)) beginOpponentTurn()
    }

    private fun beginPlayerTurn() {
        val current = _state.value
        if (current.player.stunned) {
            _state.update {
                it.copy(
                    turn = it.turn + 1,
                    player = it.player.copy(stunned = false, stunResistant = true),
                    log = it.log + "Você está atordoado e perdeu a vez."
                )
            }
            beginOpponentTurn()
            return
        }
        _state.update {
            it.copy(
                phase = DuelPhase.PlayerTurn,
                turn = it.turn + 1,
                secondsLeft = it.difficulty.turnSeconds,
                message = "Sua vez! Desenhe um feitiço do seu grimório."
            )
        }
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_state.value.secondsLeft > 0) {
                delay(1_000)
                _state.update { it.copy(secondsLeft = it.secondsLeft - 1) }
            }
            _state.update { it.copy(log = it.log + "Você hesitou e perdeu a vez!") }
            beginOpponentTurn()
        }
    }

    private fun beginOpponentTurn() {
        _state.update {
            it.copy(phase = DuelPhase.OpponentTurn, secondsLeft = 0, message = "Vez de ${it.opponent.name}...")
        }
        opponentJob?.cancel()
        opponentJob = viewModelScope.launch {
            delay(OPPONENT_THINK_MS)
            val current = _state.value
            val bot = current.opponent
            when {
                bot.stunned -> _state.update {
                    it.copy(
                        opponent = bot.copy(stunned = false, stunResistant = true),
                        log = it.log + "${bot.name} está atordoado e perdeu a vez."
                    )
                }
                random.nextFloat() < current.difficulty.botFailChance -> _state.update {
                    it.copy(opponent = bot.copy(stunResistant = false), log = it.log + "${bot.name} errou o feitiço!")
                }
                else -> {
                    val difficulty = current.difficulty
                    val spell = BattleEngine.chooseBotSpell(bot, current.player, difficulty, random)
                    val score = difficulty.botMinScore + random.nextFloat() * (difficulty.botMaxScore - difficulty.botMinScore)
                    val outcome = BattleEngine.cast(bot, current.player, spell, score)
                    _state.update { it.copy(opponent = outcome.caster, player = outcome.target, log = it.log + outcome.log) }
                    if (finishIfOver(playerCast = false)) return@launch
                }
            }
            beginPlayerTurn()
        }
    }

    /** The target falling decides the duel first; a caster only loses to its own curse if the target survives. */
    private fun finishIfOver(playerCast: Boolean): Boolean {
        val current = _state.value
        val playerDown = current.player.hp <= 0
        val opponentDown = current.opponent.hp <= 0
        if (!playerDown && !opponentDown) return false
        val won = if (playerCast) opponentDown else !playerDown
        val ending = if (won) "${current.opponent.name} foi derrotado!" else "Você foi derrotado por ${current.opponent.name}."
        _state.update { it.copy(phase = DuelPhase.Finished(won), secondsLeft = 0, message = null, log = it.log + ending) }
        val owner = userId ?: return true
        viewModelScope.launch {
            battleRepository.saveResult(owner, current.opponent.name, current.difficulty.label, won, current.turn)
        }
        return true
    }

    private fun cancelJobs() {
        timerJob?.cancel()
        opponentJob?.cancel()
    }

    class Factory(
        private val battleRepository: BattleRepository,
        private val templateRepository: TemplateRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BattleViewModel(battleRepository, templateRepository) as T
    }

    private companion object {
        const val OPPONENT_THINK_MS = 1_200L
    }
}

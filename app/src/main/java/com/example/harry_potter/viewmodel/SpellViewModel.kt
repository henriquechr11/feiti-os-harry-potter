package com.example.harry_potter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.harry_potter.data.AuthRepository
import com.example.harry_potter.data.DollarOneRecognizer
import com.example.harry_potter.data.FavoriteEntity
import com.example.harry_potter.data.RecognitionResult
import com.example.harry_potter.data.SessionManager
import com.example.harry_potter.data.Spell
import com.example.harry_potter.data.SpellRepository
import com.example.harry_potter.data.TemplateRepository
import com.example.harry_potter.data.GesturePoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SpellLoadState {
    data object Loading : SpellLoadState
    data class Success(val spells: List<Spell>) : SpellLoadState
    data class Error(val message: String) : SpellLoadState
}

data class AuthUiState(
    val userId: Long? = null,
    val isWorking: Boolean = false,
    val error: String? = null
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SpellViewModel(
    private val authRepository: AuthRepository,
    private val spellRepository: SpellRepository,
    templateRepository: TemplateRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    private val gestureTemplates = templateRepository.all()
    private val recognizer = DollarOneRecognizer(gestureTemplates)
    private val _auth = MutableStateFlow(AuthUiState(userId = sessionManager.currentUserId()))
    val auth: StateFlow<AuthUiState> = _auth.asStateFlow()

    private val _spells = MutableStateFlow<SpellLoadState>(SpellLoadState.Loading)
    val spells: StateFlow<SpellLoadState> = _spells.asStateFlow()

    val favorites: StateFlow<List<FavoriteEntity>> = _auth.flatMapLatest { state ->
        state.userId?.let(spellRepository::observeFavorites) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (_auth.value.userId != null) loadSpells()
    }

    fun loadSpells() {
        viewModelScope.launch {
            _spells.value = SpellLoadState.Loading
            runCatching { spellRepository.fetchFeaturedSpells() }
                .onSuccess { _spells.value = SpellLoadState.Success(it) }
                .onFailure { _spells.value = SpellLoadState.Error("Não foi possível buscar os feitiços. Verifique sua conexão e tente novamente.") }
        }
    }

    fun register(email: String, password: String) = submitAuth { authRepository.register(email, password) }

    fun login(email: String, password: String) = submitAuth { authRepository.login(email, password) }

    private fun submitAuth(action: suspend () -> Result<Long>) {
        viewModelScope.launch {
            _auth.value = _auth.value.copy(isWorking = true, error = null)
            action().onSuccess { id ->
                _auth.value = AuthUiState(userId = id)
                loadSpells()
            }.onFailure { error ->
                _auth.value = AuthUiState(error = error.message ?: "Não foi possível concluir a operação.")
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
        _auth.value = AuthUiState()
        _spells.value = SpellLoadState.Loading
    }

    fun toggleFavorite(spell: Spell, isFavorite: Boolean) {
        val userId = _auth.value.userId ?: return
        viewModelScope.launch { spellRepository.toggleFavorite(userId, spell, isFavorite) }
    }

    fun recognize(points: List<GesturePoint>): RecognitionResult = recognizer.recognize(points)

    fun spellForName(name: String): Spell? = (_spells.value as? SpellLoadState.Success)
        ?.spells?.firstOrNull {
            it.name.equals(name, ignoreCase = true) || it.incantation.equals(name, ignoreCase = true)
        }

    fun gestureForSpell(name: String, incantation: String?): List<GesturePoint> = gestureTemplates
        .firstOrNull {
            it.spellName.equals(incantation, ignoreCase = true) ||
                it.spellName.equals(name, ignoreCase = true)
        }?.points.orEmpty()

    class Factory(
        private val authRepository: AuthRepository,
        private val spellRepository: SpellRepository,
        private val templateRepository: TemplateRepository,
        private val sessionManager: SessionManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SpellViewModel(
            authRepository, spellRepository, templateRepository, sessionManager
        ) as T
    }
}

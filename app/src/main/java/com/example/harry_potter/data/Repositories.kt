package com.example.harry_potter.data

import android.content.Context
import com.example.harry_potter.network.HogwartsApi
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest

class SessionManager(context: Context) {
    private val preferences = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    fun currentUserId(): Long? = preferences.getLong(KEY_USER_ID, NO_USER).takeIf { it != NO_USER }

    fun saveUserId(userId: Long) {
        preferences.edit().putLong(KEY_USER_ID, userId).apply()
    }

    fun clear() {
        preferences.edit().remove(KEY_USER_ID).apply()
    }

    private companion object {
        const val KEY_USER_ID = "user_id"
        const val NO_USER = -1L
    }
}

class AuthRepository(
    private val userDao: UserDao,
    private val sessionManager: SessionManager
) {
    suspend fun register(email: String, password: String): Result<Long> {
        val cleanedEmail = email.trim().lowercase()
        if (!EMAIL_REGEX.matches(cleanedEmail)) return Result.failure(IllegalArgumentException("Informe um e-mail válido."))
        if (password.length < 6) return Result.failure(IllegalArgumentException("A senha deve ter pelo menos 6 caracteres."))
        if (userDao.findByEmail(cleanedEmail) != null) return Result.failure(IllegalArgumentException("Este e-mail já foi cadastrado."))

        val userId = userDao.insert(UserEntity(email = cleanedEmail, passwordHash = password.sha256()))
        sessionManager.saveUserId(userId)
        return Result.success(userId)
    }

    suspend fun login(email: String, password: String): Result<Long> {
        val user = userDao.findByEmail(email.trim().lowercase())
        return if (user != null && user.passwordHash == password.sha256()) {
            sessionManager.saveUserId(user.id)
            Result.success(user.id)
        } else {
            Result.failure(IllegalArgumentException("E-mail ou senha inválidos."))
        }
    }

    fun signOut() = sessionManager.clear()

    private companion object {
        val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}

class SpellRepository(
    private val api: HogwartsApi,
    private val favoriteDao: FavoriteDao
) {
    suspend fun fetchFeaturedSpells(): List<Spell> {
        val allSpells = api.getSpells().data.map {
            Spell(it.id, it.name, it.incantation, it.category, it.effect, it.hand)
        }
        // Every item in this curated list has one local, unique drawing. The
        // matching API record enriches it when that source has the same name or
        // incantation, but never determines whether it is shown in the app.
        return LOCAL_SPELLS.map { local ->
            val apiSpell = allSpells.firstOrNull { remote ->
                remote.name.normalizedSpellName() == local.key ||
                    remote.incantation?.normalizedSpellName() == local.key
            }
            Spell(
                id = "spell-${local.key}",
                name = local.name,
                incantation = local.incantation,
                category = apiSpell?.category ?: local.category,
                effect = apiSpell?.effect ?: local.effect,
                hand = apiSpell?.hand
            )
        }
    }

    fun observeFavorites(userId: Long): Flow<List<FavoriteEntity>> = favoriteDao.observeForUser(userId)

    fun isFavorite(userId: Long, spellId: String): Flow<Boolean> = favoriteDao.isFavorite(userId, spellId)

    suspend fun toggleFavorite(userId: Long, spell: Spell, isFavorite: Boolean) {
        if (isFavorite) {
            favoriteDao.delete(userId, spell.id)
        } else {
            favoriteDao.insert(
                FavoriteEntity(userId, spell.id, spell.name, spell.incantation, spell.category, spell.effect, spell.hand)
            )
        }
    }

    companion object {
        private val LOCAL_SPELLS = listOf(
            LocalSpell("accio", "Accio", "Accio", "Charm", "Summons an object."),
            LocalSpell("aguamenti", "Aguamenti", "Aguamenti", "Charm", "Conjures water."),
            LocalSpell("alohomora", "Alohomora", "Alohomora", "Charm", "Unlocks doors and objects."),
            LocalSpell("avadakedavra", "Avada Kedavra", "Avada Kedavra", "Curse", "The Killing Curse."),
            LocalSpell("bombarda", "Bombarda", "Bombarda", "Charm", "Creates an explosion."),
            LocalSpell("confundo", "Confundo", "Confundo", "Charm", "Confuses the target."),
            LocalSpell("crucio", "Crucio", "Crucio", "Curse", "Inflicts intense pain."),
            LocalSpell("diffindo", "Diffindo", "Diffindo", "Charm", "Cuts an object precisely."),
            LocalSpell("engorgio", "Engorgio", "Engorgio", "Charm", "Makes an object grow."),
            LocalSpell("episkey", "Episkey", "Episkey", "Healing spell", "Heals minor injuries."),
            LocalSpell("expelliarmus", "Expelliarmus", "Expelliarmus", "Charm", "Disarms an opponent."),
            LocalSpell("expectopatronum", "Expecto Patronum", "Expecto Patronum", "Charm", "Conjures a Patronus to repel Dementors."),
            LocalSpell("finiteincantatem", "Finite Incantatem", "Finite Incantatem", "Counter-spell", "Ends an ongoing spell."),
            LocalSpell("impedimenta", "Impedimenta", "Impedimenta", "Jinx", "Slows or stops a target."),
            LocalSpell("imperio", "Imperio", "Imperio", "Curse", "Controls the target's actions."),
            LocalSpell("lumos", "Lumos", "Lumos", "Charm", "Creates light at the wand tip."),
            LocalSpell("nox", "Nox", "Nox", "Charm", "Extinguishes wand light."),
            LocalSpell("obliviate", "Obliviate", "Obliviate", "Charm", "Erases or changes memories."),
            LocalSpell("petrificustotalus", "Petrificus Totalus", "Petrificus Totalus", "Jinx", "Petrifies the target's body."),
            LocalSpell("protego", "Protego", "Protego", "Charm", "Creates a protective shield."),
            LocalSpell("reducto", "Reducto", "Reducto", "Curse", "Blasts solid objects apart."),
            LocalSpell("riddikulus", "Riddikulus", "Riddikulus", "Charm", "Defeats a Boggart with laughter."),
            LocalSpell("sectumsempra", "Sectumsempra", "Sectumsempra", "Curse", "Causes deep slashing wounds."),
            LocalSpell("stupefy", "Stupefy", "Stupefy", "Jinx", "Stuns the target."),
            LocalSpell("wingardiumleviosa", "Wingardium Leviosa", "Wingardium Leviosa", "Charm", "Makes an object levitate.")
        )
    }
}

private data class LocalSpell(
    val key: String,
    val name: String,
    val incantation: String,
    val category: String,
    val effect: String
)

class BattleRepository(private val battleDao: BattleDao) {
    suspend fun saveResult(userId: Long, opponentName: String, difficulty: String, won: Boolean, turns: Int) {
        battleDao.insert(
            BattleResultEntity(
                userId = userId,
                opponentName = opponentName,
                difficulty = difficulty,
                won = won,
                turns = turns,
                playedAt = System.currentTimeMillis()
            )
        )
    }

    fun observeHistory(userId: Long): Flow<List<BattleResultEntity>> = battleDao.observeForUser(userId)
}

class TemplateRepository(context: Context) {
    private val templates: List<GestureTemplate>

    init {
        val json = context.assets.open("gesture_templates.json").bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<TemplateJson>>() {}.type
        templates = Gson().fromJson<List<TemplateJson>>(json, type).map {
            GestureTemplate(it.spellName, it.points.map { point -> GesturePoint(point.x, point.y) })
        }
    }

    fun all(): List<GestureTemplate> = templates

    private data class TemplateJson(val spellName: String, val points: List<PointJson>)
    private data class PointJson(val x: Float, val y: Float)
}

fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
    .digest(toByteArray())
    .joinToString("") { "%02x".format(it) }

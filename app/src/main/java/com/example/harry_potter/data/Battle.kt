package com.example.harry_potter.data

import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

/** Combat values of a spell. A spell may combine several effects (e.g. damage + stun). */
data class BattleSpell(
    val key: String,
    val name: String,
    val damage: Int = 0,
    val heal: Int = 0,
    val shield: Boolean = false,
    val stun: Boolean = false,
    val weaken: Boolean = false,
    val empower: Boolean = false,
    val breakShield: Boolean = false,
    val cleanse: Boolean = false,
    val selfCost: Int = 0
) {
    val isOffensive: Boolean get() = damage > 0 || stun || weaken

    fun description(): String = listOfNotNull(
        "Dano $damage".takeIf { damage > 0 },
        "Cura $heal".takeIf { heal > 0 },
        "Escudo".takeIf { shield },
        "Atordoa".takeIf { stun },
        "Enfraquece".takeIf { weaken },
        "Fortalece".takeIf { empower },
        "Quebra escudo".takeIf { breakShield },
        "Limpa efeitos".takeIf { cleanse },
        "Custa $selfCost HP".takeIf { selfCost > 0 }
    ).joinToString(" · ")
}

object BattleCatalog {
    val spells = listOf(
        BattleSpell("accio", "Accio", damage = 10),
        BattleSpell("aguamenti", "Aguamenti", damage = 12),
        BattleSpell("alohomora", "Alohomora", damage = 6, breakShield = true),
        BattleSpell("avadakedavra", "Avada Kedavra", damage = 40, selfCost = 15),
        BattleSpell("bombarda", "Bombarda", damage = 20),
        BattleSpell("confundo", "Confundo", weaken = true),
        BattleSpell("crucio", "Crucio", damage = 25, selfCost = 8),
        BattleSpell("diffindo", "Diffindo", damage = 16),
        BattleSpell("engorgio", "Engorgio", empower = true),
        BattleSpell("episkey", "Episkey", heal = 20),
        BattleSpell("expelliarmus", "Expelliarmus", stun = true),
        BattleSpell("expectopatronum", "Expecto Patronum", heal = 12, shield = true),
        BattleSpell("finiteincantatem", "Finite Incantatem", cleanse = true),
        BattleSpell("impedimenta", "Impedimenta", damage = 8, weaken = true),
        BattleSpell("imperio", "Imperio", stun = true, selfCost = 8),
        BattleSpell("lumos", "Lumos", weaken = true),
        BattleSpell("nox", "Nox", shield = true),
        BattleSpell("obliviate", "Obliviate", damage = 6, cleanse = true),
        BattleSpell("petrificustotalus", "Petrificus Totalus", damage = 5, stun = true),
        BattleSpell("protego", "Protego", shield = true),
        BattleSpell("reducto", "Reducto", damage = 18, breakShield = true),
        BattleSpell("riddikulus", "Riddikulus", heal = 10),
        BattleSpell("sectumsempra", "Sectumsempra", damage = 30, selfCost = 10),
        BattleSpell("stupefy", "Stupefy", damage = 14),
        BattleSpell("wingardiumleviosa", "Wingardium Leviosa", damage = 10)
    )

    fun byKey(key: String): BattleSpell? = spells.firstOrNull { it.key == key }

    fun byName(name: String): BattleSpell? = byKey(name.normalizedSpellName())
}

enum class Difficulty(
    val label: String,
    val opponentName: String,
    val turnSeconds: Int,
    val botFailChance: Float,
    val botMinScore: Float,
    val botMaxScore: Float
) {
    EASY("Fácil", "Draco Malfoy", 15, .30f, .70f, .85f),
    MEDIUM("Médio", "Bellatrix Lestrange", 10, .15f, .75f, .92f),
    HARD("Difícil", "Lord Voldemort", 7, .05f, .82f, .98f)
}

data class Duelist(
    val name: String,
    val deck: List<BattleSpell>,
    val hp: Int = BattleEngine.MAX_HP,
    val shielded: Boolean = false,
    val stunned: Boolean = false,
    val stunResistant: Boolean = false,
    val weakened: Boolean = false,
    val empowered: Boolean = false
)

data class CastOutcome(val caster: Duelist, val target: Duelist, val log: String)

object BattleEngine {
    const val MAX_HP = 100
    const val DECK_SIZE = 7

    fun precisionMultiplier(score: Float): Float = when {
        score >= .90f -> 1.25f
        score >= .80f -> 1f
        else -> .75f
    }

    fun isValidDeck(deck: List<BattleSpell>): Boolean =
        deck.size == DECK_SIZE && deck.distinctBy { it.key }.size == DECK_SIZE && deck.any { it.damage > 0 }

    /** Order: cleanse → break shield → offensive part (blocked by a shield) → own effects → curse cost. */
    fun cast(caster: Duelist, target: Duelist, spell: BattleSpell, score: Float): CastOutcome {
        val multiplier = precisionMultiplier(score)
        var self = caster.copy(stunResistant = false)
        var enemy = target
        val parts = mutableListOf<String>()


        if (spell.cleanse) {
            self = self.copy(weakened = false)
            enemy = enemy.copy(shielded = false, empowered = false)
            parts += "dissipou os efeitos mágicos"
        }
        if (spell.breakShield && enemy.shielded) {
            enemy = enemy.copy(shielded = false)
            parts += "quebrou o escudo de ${enemy.name}"
        }
        if (spell.isOffensive) {
            if (enemy.shielded) {
                enemy = enemy.copy(shielded = false)
                parts += "${enemy.name} bloqueou com o escudo"
            } else {
                if (spell.damage > 0) {
                    var damage = spell.damage * multiplier
                    if (self.empowered) damage *= 1.5f
                    if (self.weakened) damage *= .5f
                    val amount = damage.roundToInt().coerceAtLeast(1)
                    enemy = enemy.copy(hp = (enemy.hp - amount).coerceAtLeast(0))
                    parts += "causou $amount de dano"
                }
                if (spell.stun) {
                    if (enemy.stunResistant) {
                        parts += "${enemy.name} resistiu ao atordoamento"
                    } else {
                        enemy = enemy.copy(stunned = true)
                        parts += "atordoou ${enemy.name}"
                    }
                }
                if (spell.weaken) {
                    enemy = enemy.copy(weakened = true)
                    parts += "enfraqueceu ${enemy.name}"
                }
            }
            if (spell.damage > 0) self = self.copy(empowered = false, weakened = false)
        }
        if (spell.heal > 0) {
            val healed = min(MAX_HP, self.hp + (spell.heal * multiplier).roundToInt()) - self.hp
            self = self.copy(hp = self.hp + healed)
            parts += "recuperou $healed HP"
        }
        if (spell.shield) {
            self = self.copy(shielded = true)
            parts += "ergueu um escudo"
        }
        if (spell.empower) {
            self = self.copy(empowered = true)
            parts += "fortaleceu o próximo ataque"
        }
        if (spell.selfCost > 0) {
            self = self.copy(hp = (self.hp - spell.selfCost).coerceAtLeast(0))
            parts += "pagou ${spell.selfCost} HP pela maldição"
        }

        val quality = when {
            score >= .90f -> " (Perfeito!)"
            score < .80f -> " (fraco)"
            else -> ""
        }
        return CastOutcome(self, enemy, "${caster.name} lançou ${spell.name}$quality: ${parts.joinToString(", ")}.")
    }

    fun randomDeck(random: Random): List<BattleSpell> =
        generateSequence { BattleCatalog.spells.shuffled(random).take(DECK_SIZE) }.first(::isValidDeck)

    fun chooseBotSpell(bot: Duelist, player: Duelist, difficulty: Difficulty, random: Random): BattleSpell {
        // Never cast a curse whose cost would knock the bot out.
        val usable = bot.deck.filter { it.selfCost < bot.hp }.ifEmpty { bot.deck }
        val heals = usable.filter { it.heal > 0 }
        if (bot.hp <= 35 && heals.isNotEmpty() && random.nextFloat() < .7f) return heals.random(random)
        if (player.shielded) usable.filter { it.breakShield || it.cleanse }.takeIf { it.isNotEmpty() }?.let { return it.random(random) }
        if (player.empowered && !bot.shielded) usable.filter { it.shield }.takeIf { it.isNotEmpty() }?.let { return it.random(random) }
        val attacks = usable.filter { it.isOffensive }.ifEmpty { return usable.random(random) }
        return if (difficulty == Difficulty.HARD && random.nextFloat() < .6f) {
            attacks.maxBy { it.damage }
        } else {
            attacks.random(random)
        }
    }
}

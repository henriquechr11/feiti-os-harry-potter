package com.example.harry_potter

import com.example.harry_potter.data.BattleCatalog
import com.example.harry_potter.data.BattleEngine
import com.example.harry_potter.data.Difficulty
import com.example.harry_potter.data.Duelist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

class BattleEngineTest {
    private fun spell(key: String) = BattleCatalog.byKey(key)!!
    private val caster = Duelist("A", emptyList())
    private val target = Duelist("B", emptyList())

    @Test
    fun shieldBlocksDamageAndStunAndIsConsumed() {
        val shielded = target.copy(shielded = true)
        val outcome = BattleEngine.cast(caster, shielded, spell("petrificustotalus"), .85f)
        assertEquals(BattleEngine.MAX_HP, outcome.target.hp)
        assertFalse(outcome.target.stunned)
        assertFalse(outcome.target.shielded)
    }

    @Test
    fun breakShieldHitsThroughTheShield() {
        val outcome = BattleEngine.cast(caster, target.copy(shielded = true), spell("reducto"), .85f)
        assertEquals(BattleEngine.MAX_HP - 18, outcome.target.hp)
    }

    @Test
    fun precisionChangesDamage() {
        val bombarda = spell("bombarda")
        assertEquals(75, BattleEngine.cast(caster, target, bombarda, .95f).target.hp)
        assertEquals(80, BattleEngine.cast(caster, target, bombarda, .85f).target.hp)
        assertEquals(85, BattleEngine.cast(caster, target, bombarda, .72f).target.hp)
    }

    @Test
    fun empowerAndWeakenAreConsumedByTheNextAttack() {
        val empowered = BattleEngine.cast(caster.copy(empowered = true), target, spell("bombarda"), .85f)
        assertEquals(70, empowered.target.hp)
        assertFalse(empowered.caster.empowered)

        val weakened = BattleEngine.cast(caster.copy(weakened = true), target, spell("bombarda"), .85f)
        assertEquals(90, weakened.target.hp)
        assertFalse(weakened.caster.weakened)
    }

    @Test
    fun curseCostsTheCasterHp() {
        val outcome = BattleEngine.cast(caster, target, spell("avadakedavra"), .85f)
        assertEquals(60, outcome.target.hp)
        assertEquals(85, outcome.caster.hp)
    }

    @Test
    fun recentlyStunnedDuelistResistsAnotherStun() {
        val outcome = BattleEngine.cast(caster, target.copy(stunResistant = true), spell("expelliarmus"), .85f)
        assertFalse(outcome.target.stunned)
        assertTrue(BattleEngine.cast(caster, target, spell("expelliarmus"), .85f).target.stunned)
    }

    @Test
    fun healNeverExceedsMaxHp() {
        val outcome = BattleEngine.cast(caster.copy(hp = 95), target, spell("episkey"), .95f)
        assertEquals(BattleEngine.MAX_HP, outcome.caster.hp)
    }

    @Test
    fun deckNeedsSevenDistinctSpellsAndOneDamageSpell() {
        val noDamage = listOf("confundo", "engorgio", "episkey", "expelliarmus", "finiteincantatem", "lumos", "protego").map(::spell)
        assertFalse(BattleEngine.isValidDeck(noDamage))
        assertTrue(BattleEngine.isValidDeck(noDamage.dropLast(1) + spell("bombarda")))
        assertFalse(BattleEngine.isValidDeck(noDamage.take(6)))
        assertTrue(BattleEngine.isValidDeck(BattleEngine.randomDeck(Random(7))))
    }

    @Test
    fun botHealsWhenLowOnHealth() {
        val deck = listOf("bombarda", "diffindo", "stupefy", "accio", "aguamenti", "reducto", "episkey").map(::spell)
        val bot = Duelist("Bot", deck, hp = 20)
        val healed = (0 until 50).count { seed ->
            BattleEngine.chooseBotSpell(bot, target, Difficulty.MEDIUM, Random(seed)).heal > 0
        }
        assertTrue("bot curou só $healed de 50 vezes", healed >= 25)
    }

    @Test
    fun everyGestureTemplateHasABattleSpell() {
        val json = File("src/main/assets/gesture_templates.json").readText()
        val names = Regex("\"spellName\"\\s*:\\s*\"([^\"]+)\"").findAll(json).map { it.groupValues[1] }.toList()
        assertEquals(BattleCatalog.spells.size, names.size)
        names.forEach { assertNotNull("sem efeito de batalha: $it", BattleCatalog.byName(it)) }
    }
}

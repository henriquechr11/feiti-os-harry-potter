package com.example.harry_potter.data

data class Spell(
    val id: String,
    val name: String,
    val incantation: String?,
    val category: String?,
    val effect: String?,
    val hand: String?
)

data class GesturePoint(val x: Float, val y: Float)

data class GestureTemplate(
    val spellName: String,
    val points: List<GesturePoint>
)

data class RecognitionResult(
    val spellName: String?,
    val score: Float,
    val message: String
)

fun String.normalizedSpellName(): String = trim().lowercase().replace(Regex("[^a-z0-9]"), "")

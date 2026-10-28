package com.example.harry_potter

import com.example.harry_potter.data.DollarOneRecognizer
import com.example.harry_potter.data.GesturePoint
import com.example.harry_potter.data.GestureTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DollarOneRecognizerTest {
    private val v = GestureTemplate("Accio", listOf(
        GesturePoint(10f, 10f), GesturePoint(20f, 30f), GesturePoint(30f, 50f), GesturePoint(40f, 70f),
        GesturePoint(50f, 90f), GesturePoint(60f, 70f), GesturePoint(70f, 50f), GesturePoint(80f, 30f), GesturePoint(90f, 10f)
    ))
    private val l = GestureTemplate("Alohomora", listOf(
        GesturePoint(10f, 10f), GesturePoint(10f, 30f), GesturePoint(10f, 50f), GesturePoint(10f, 70f),
        GesturePoint(10f, 90f), GesturePoint(30f, 90f), GesturePoint(50f, 90f), GesturePoint(70f, 90f), GesturePoint(90f, 90f)
    ))

    @Test
    fun recognizesMatchingSingleStroke() {
        val result = DollarOneRecognizer(listOf(v, l)).recognize(v.points)
        assertEquals("Accio", result.spellName)
    }

    @Test
    fun rejectsVeryShortStroke() {
        val result = DollarOneRecognizer(listOf(v, l)).recognize(listOf(GesturePoint(1f, 1f), GesturePoint(2f, 2f)))
        assertNull(result.spellName)
    }
}

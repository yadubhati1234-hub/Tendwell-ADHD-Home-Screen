package app.tendwell.home

import org.junit.Assert.assertEquals
import org.junit.Test

class EffectivePausedTest {
    private val saved = linkedSetOf("a", "b", "c", "d", "e")

    @Test
    fun proKeepsEveryPausedApp() {
        assertEquals(saved, effectivePaused(saved, isPro = true))
    }

    @Test
    fun freeKeepsTheFirstThreeInSavedOrder() {
        assertEquals(listOf("a", "b", "c"), effectivePaused(saved, isPro = false).toList())
        assertEquals(setOf("x"), effectivePaused(setOf("x"), isPro = false))
    }
}

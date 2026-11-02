package app.tendwell.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PauseSuggestionsTest {
    @Test
    fun knownTimeSinksAreSuggested() {
        assertTrue(PauseSuggestions.isSuggested("com.instagram.android"))
        assertTrue(PauseSuggestions.isSuggested("com.google.android.youtube"))
        assertFalse(PauseSuggestions.isSuggested("com.google.android.dialer"))
    }

    @Test
    fun splitKeepsSuggestedInRankOrderAndTheRestInListOrder() {
        val apps = listOf("com.android.camera", "com.reddit.frontpage", "com.android.chrome", "com.instagram.android")
        val (suggested, other) = PauseSuggestions.split(apps) { it }
        assertEquals(listOf("com.instagram.android", "com.reddit.frontpage"), suggested)
        assertEquals(listOf("com.android.camera", "com.android.chrome"), other)
    }
}

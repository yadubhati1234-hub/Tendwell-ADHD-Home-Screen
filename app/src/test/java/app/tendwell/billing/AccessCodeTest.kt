package app.tendwell.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessCodeTest {
    @Test
    fun normalizeDropsDashesAndSpacesAndUppercases() {
        assertEquals("ABCD12EF", AccessCode.normalize(" abcd-12 ef "))
    }

    @Test
    fun sha256MatchesKnownVector() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", AccessCode.sha256("abc"))
    }

    @Test
    fun matchesTheCodeHowEverItIsTyped() {
        val hash = AccessCode.sha256("TESTCODE1234")
        assertTrue(AccessCode.matches("TESTCODE1234", hash))
        assertTrue(AccessCode.matches("test-code-1234", hash))
        assertTrue(AccessCode.matches("TEST CODE 1234", hash))
    }

    @Test
    fun rejectsOtherCodes() {
        val hash = AccessCode.sha256("TESTCODE1234")
        assertFalse(AccessCode.matches("TESTCODE1235", hash))
        assertFalse(AccessCode.matches("", hash))
        assertFalse(AccessCode.matches("---", hash))
        assertFalse(AccessCode.matches("TESTCODE1234"))
    }
}

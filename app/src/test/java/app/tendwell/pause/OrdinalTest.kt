package app.tendwell.pause

import org.junit.Assert.assertEquals
import org.junit.Test

class OrdinalTest {
    @Test
    fun ordinals() {
        assertEquals(listOf("1st", "2nd", "3rd", "4th", "7th"), listOf(1, 2, 3, 4, 7).map(::ordinal))
        assertEquals(listOf("11th", "12th", "13th", "21st", "22nd", "101st", "112th"), listOf(11, 12, 13, 21, 22, 101, 112).map(::ordinal))
    }
}

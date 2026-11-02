package app.tendwell.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenLogFormatTest {
    private val insta = "com.instagram.android/.MainActivity#0"
    private val tiktok = "com.zhiliaoapp.musically/.Main#0"

    @Test
    fun lineRoundTrips() {
        val e = OpenEvent(1_700_000_000_000, PauseOutcome.TurnedBack, insta)
        assertEquals(e, OpenLogFormat.parse(OpenLogFormat.line(e)))
    }

    @Test
    fun badLinesAreIgnored() {
        assertNull(OpenLogFormat.parse(""))
        assertNull(OpenLogFormat.parse("abc,o,$insta"))
        assertNull(OpenLogFormat.parse("123,x,$insta"))
        assertNull(OpenLogFormat.parse("123,o,"))
    }

    @Test
    fun countsOnlyOpensOfThatAppInTheWindow() {
        val events = listOf(
            OpenEvent(100, PauseOutcome.Opened, insta),
            OpenEvent(200, PauseOutcome.TurnedBack, insta),
            OpenEvent(300, PauseOutcome.Opened, insta),
            OpenEvent(400, PauseOutcome.Opened, tiktok),
            OpenEvent(50, PauseOutcome.Opened, insta),
        )
        assertEquals(2, OpenLogFormat.opensBetween(events, insta, 100, 1000))
        assertEquals(1, OpenLogFormat.opensBetween(events, tiktok, 0, 1000))
    }
}

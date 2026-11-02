package app.tendwell.focus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class FocusHoursTest {
    // Tuesday 3 November 2026.
    private val tue = LocalDate.of(2026, 11, 3)
    private fun at(date: LocalDate, h: Int, m: Int = 0) = date.atTime(h, m)

    private val work = FocusBlock("work", "Work hours", true, FocusHours.WEEKDAYS, 9 * 60, 17 * 60, 30)
    private val late = FocusBlock("late", "Late", true, FocusHours.ALL_DAYS, 22 * 60, 1 * 60, 120)

    @Test
    fun workHoursRunOnWeekdaysOnly() {
        assertEquals(work, FocusHours.activeAt(listOf(work), at(tue, 10, 24)))
        assertNull(FocusHours.activeAt(listOf(work), at(tue, 17, 0)))
        assertNull(FocusHours.activeAt(listOf(work), at(tue, 8, 59)))
        assertNull(FocusHours.activeAt(listOf(work), at(LocalDate.of(2026, 11, 7), 10))) // Saturday
        assertNull(FocusHours.activeAt(listOf(work.copy(on = false)), at(tue, 10)))
    }

    @Test
    fun blocksPastMidnightCountForTheNextMorning() {
        assertEquals(late, FocusHours.activeAt(listOf(late), at(tue, 23, 30)))
        assertEquals(late, FocusHours.activeAt(listOf(late), at(tue, 0, 30)))
        assertNull(FocusHours.activeAt(listOf(late), at(tue, 1, 0)))
        // Friday-only late block still covers early Saturday, not early Friday.
        val fri = late.copy(days = 1 shl 4)
        assertEquals(fri, FocusHours.activeAt(listOf(fri), at(LocalDate.of(2026, 11, 7), 0, 30)))
        assertNull(FocusHours.activeAt(listOf(fri), at(LocalDate.of(2026, 11, 6), 0, 30)))
    }

    @Test
    fun pauseLengthNeedsPro() {
        assertEquals(30, FocusHours.pauseSeconds(listOf(work), at(tue, 10), isPro = true))
        assertEquals(10, FocusHours.pauseSeconds(listOf(work), at(tue, 10), isPro = false))
        assertEquals(10, FocusHours.pauseSeconds(listOf(work), at(tue, 18), isPro = true))
        val both = listOf(work, work.copy(id = "x", pauseSeconds = 60))
        assertEquals(60, FocusHours.pauseSeconds(both, at(tue, 10), isPro = true))
    }

    @Test
    fun statusMatchesTheMockup() {
        val eve = FocusBlock("evenings", "Evenings", true, FocusHours.ALL_DAYS, 20 * 60, 23 * 60, 30)
        assertEquals(FocusStatus.On(work, 17 * 60, 6 * 60 + 36), FocusHours.status(listOf(work, eve), at(tue, 10, 24)))
        assertEquals(FocusStatus.Next(eve, 20 * 60, 9 * 60 + 36), FocusHours.status(listOf(work.copy(on = false), eve), at(tue, 10, 24)))
        assertEquals(FocusStatus.Off, FocusHours.status(listOf(work), at(tue, 18)))
        assertEquals("6h 36m", FocusHours.durationLabel(396))
        assertEquals("2h", FocusHours.durationLabel(120))
        assertEquals("45m", FocusHours.durationLabel(45))
    }

    @Test
    fun segmentsSplitAtMidnight() {
        assertEquals(listOf(0 to 60, 9 * 60 to 17 * 60, 22 * 60 to 1440), FocusHours.segmentsOn(listOf(work, late), tue))
    }

    @Test
    fun dayLabels() {
        assertEquals("Every day", FocusHours.daysLabel(FocusHours.ALL_DAYS))
        assertEquals("Weekdays", FocusHours.daysLabel(FocusHours.WEEKDAYS))
        assertEquals("Weekends", FocusHours.daysLabel(FocusHours.WEEKENDS))
        assertEquals("No days picked", FocusHours.daysLabel(0))
        assertEquals("Mon–Wed, Sat", FocusHours.daysLabel(0b0100111))
        assertEquals("Mon, Tue, Fri", FocusHours.daysLabel(0b0010011))
        assertEquals("Weekdays · 9:00 → 17:00", FocusHours.summary(work))
        assertEquals("30 s", FocusHours.pauseLabel(30))
        assertEquals("2 min", FocusHours.pauseLabel(120))
    }

    @Test
    fun savedFormRoundTrips() {
        val blocks = listOf(work, late.copy(name = "Late | night"))
        assertEquals(blocks, FocusHours.decode(FocusHours.encode(blocks)))
        assertEquals(FocusHours.defaults(), FocusHours.decode(null))
        assertTrue(FocusHours.decode("").isEmpty())
        assertTrue(FocusHours.decode("bad line").isEmpty())
    }
}

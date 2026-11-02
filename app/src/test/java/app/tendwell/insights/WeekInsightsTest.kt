package app.tendwell.insights

import app.tendwell.data.OpenEvent
import app.tendwell.data.PauseOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

class WeekInsightsTest {
    private val zone = ZoneOffset.UTC
    private val week = LocalDate.of(2026, 10, 26) // Monday
    private fun at(date: LocalDate, h: Int, m: Int = 0) = date.atTime(h, m).toInstant(zone).toEpochMilli()
    private fun open(date: LocalDate, h: Int, app: String = "insta") = OpenEvent(at(date, h), PauseOutcome.Opened, app)
    private fun back(date: LocalDate, h: Int, app: String = "insta") = OpenEvent(at(date, h), PauseOutcome.TurnedBack, app)

    @Test
    fun weekStartsOnMonday() {
        assertEquals(week, WeekInsights.weekStart(LocalDate.of(2026, 11, 1))) // Sunday
        assertEquals(LocalDate.of(2026, 11, 2), WeekInsights.weekStart(LocalDate.of(2026, 11, 3)))
    }

    @Test
    fun slotsRunInThreeHourSteps() {
        assertEquals(listOf(5, 5, 0, 0, 1, 4, 5, 5), listOf(0, 5, 6, 8, 9, 20, 21, 23).map(WeekInsights::slotOf))
    }

    @Test
    fun countsTheWeekAndComparesWithTheOneBefore() {
        val events = listOf(
            open(week.minusDays(3), 10), back(week.minusDays(3), 11), open(week.minusDays(2), 22),
            open(week, 9), open(week, 22), back(week, 23),
            open(week.plusDays(3), 22, "tiktok"), back(week.plusDays(3), 1, "tiktok"), back(week.plusDays(6), 23, "tiktok"),
            open(week.plusDays(7), 10), // next week, ignored
        )
        val s = WeekInsights.stats(events, week, zone)
        assertEquals(3, s.opens)
        assertEquals(3, s.turnedBack)
        assertEquals(listOf(2, 0, 0, 1, 0, 0, 0), s.opensPerDay)
        assertEquals(listOf(0, 1, 0, 0, 0, 2), s.opensPerSlot)
        assertEquals(listOf(AppWeek("insta", 2, 1), AppWeek("tiktok", 1, 2)), s.byApp)
        assertEquals(2, s.prevOpens)
        assertEquals(1, s.prevTurnedBack)
        assertEquals("2 more than last week", WeekInsights.turnedBackChange(s))
        assertEquals("50% more than last week", WeekInsights.opensChange(s))
        assertEquals(5, WeekInsights.driftSlot(s))
        assertEquals("You drift most between 21:00 and 6:00.", WeekInsights.driftSentence(5))
        assertEquals("You drift most between 9:00 and 12:00.", WeekInsights.driftSentence(1))
        assertFalse(s.isEmpty)
    }

    @Test
    fun noPreviousWeekMeansNoComparison() {
        val s = WeekInsights.stats(listOf(open(week, 9)), week, zone)
        assertNull(WeekInsights.turnedBackChange(s))
        assertNull(WeekInsights.opensChange(s))
        assertTrue(WeekInsights.stats(emptyList(), week, zone).isEmpty)
    }

    @Test
    fun calmestDaysOnlyCountDaysSoFar() {
        val s = WeekInsights.stats(listOf(open(week, 9), open(week, 10), open(week.plusDays(1), 9)), week, zone)
        // On Wednesday, Monday (2) and Tuesday (1) are finished; Wednesday is not.
        assertEquals("Calmest day: Tuesday.", WeekInsights.calmestDays(s, week.plusDays(2), Locale.US))
        // On Tuesday only Monday is finished: nothing to compare.
        assertNull(WeekInsights.calmestDays(s, week.plusDays(1), Locale.US))
        val full = s.copy(opensPerDay = listOf(12, 9, 11, 7, 8, 10, 7), opens = 64)
        assertEquals("Calmest days: Thursday and Sunday.", WeekInsights.calmestDays(full, week.plusDays(7), Locale.US))
    }

    @Test
    fun weekNames() {
        val today = LocalDate.of(2026, 11, 3)
        assertEquals("This week", WeekInsights.weekName(LocalDate.of(2026, 11, 2), today))
        assertEquals("Last week", WeekInsights.weekName(week, today))
        assertEquals("2 weeks ago", WeekInsights.weekName(week.minusDays(7), today))
        assertEquals("26 Oct – 1 Nov", WeekInsights.weekRange(week, Locale.US))
    }
}

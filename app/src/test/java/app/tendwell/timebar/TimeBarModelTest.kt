package app.tendwell.timebar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeBarModelTest {
    private fun hm(h: Int, m: Int = 0) = h * 60 + m
    private val sampleDay = listOf(
        DayEvent("Gym", hm(18), hm(19)),
        DayEvent("Stand-up", hm(11), hm(11, 15)),
        DayEvent("Lunch with Sam", hm(13), hm(14)),
    )

    @Test
    fun sampleMorningMatchesTheDesign() {
        val bar = TimeBarModel(hm(7), hm(23), hm(10, 24), sampleDay)
        assertEquals("36 min until Stand-up", bar.primaryLabel)
        assertEquals("12h 36m until bed", bar.secondaryLabel)
        assertEquals(0.2125f, bar.nowFraction, 0.0001f)
        assertEquals(listOf("Stand-up", "Lunch with Sam", "Gym"), bar.events.map { it.title })
        assertEquals(listOf("7:00", "12:00", "17:00", "Bed 23:00"), bar.axis.map { it.second })
        assertEquals(0.3125f, bar.axis[1].first, 0.0001f)
    }

    @Test
    fun duringAnEventShowsWhenItEnds() {
        val bar = TimeBarModel(hm(7), hm(23), hm(11, 5), sampleDay)
        assertEquals("Stand-up until 11:15", bar.primaryLabel)
        assertEquals("Lunch with Sam", bar.next?.title)
    }

    @Test
    fun withoutEventsTheBarCountsDownToBed() {
        val bar = TimeBarModel(hm(7), hm(23), hm(21, 30), emptyList())
        assertEquals("1h 30m until bed", bar.primaryLabel)
        assertNull(bar.secondaryLabel)
    }

    @Test
    fun pastBedtimeIsSaidGently() {
        val bar = TimeBarModel(hm(7), hm(23), hm(23, 20), emptyList())
        assertTrue(bar.pastBedtime)
        assertEquals("Past bedtime", bar.primaryLabel)
        assertEquals(1f, bar.nowFraction, 0f)
    }

    @Test
    fun bedtimeAfterMidnightWorks() {
        val evening = TimeBarModel(hm(7), hm(0, 30), hm(23, 0), emptyList())
        assertEquals("1h 30m until bed", evening.primaryLabel)
        val afterMidnight = TimeBarModel(hm(7), hm(0, 30), hm(0, 10), emptyList())
        assertEquals("20 min until bed", afterMidnight.primaryLabel)
        assertEquals("Bed 0:30", afterMidnight.axis.last().second)
    }

    @Test
    fun earlyMorningSitsAtTheStartOfTheBar() {
        val bar = TimeBarModel(hm(7), hm(23), hm(5, 0), emptyList())
        assertEquals(0f, bar.nowFraction, 0f)
        assertEquals("18h until bed", bar.primaryLabel)
    }

    @Test
    fun formatting() {
        assertEquals("7:05", TimeBarModel.formatClock(hm(7, 5)))
        assertEquals("0:30", TimeBarModel.formatClock(hm(24, 30)))
        assertEquals("45 min", TimeBarModel.formatDuration(45))
        assertEquals("2h", TimeBarModel.formatDuration(120))
        assertEquals("12h 36m", TimeBarModel.formatDuration(756))
    }
}

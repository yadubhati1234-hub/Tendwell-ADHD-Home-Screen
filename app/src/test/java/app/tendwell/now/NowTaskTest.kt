package app.tendwell.now

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NowTaskTest {
    private val min = NowTask.MINUTE
    private val t0 = 1_000_000_000L

    @Test
    fun startRunsForTheChosenMinutes() {
        val task = NowTask.start("  Answer two emails ", 25, t0)
        assertEquals("Answer two emails", task.title)
        assertTrue(task.running)
        assertEquals(25 * min, task.remainingMs(t0))
        assertEquals(18 * min, task.remainingMs(t0 + 7 * min))
        assertEquals(0.72f, task.fractionLeft(t0 + 7 * min), 0.001f)
    }

    @Test
    fun pauseFreezesAndResumeContinues() {
        val paused = NowTask.start("Read", 25, t0).pause(t0 + 5 * min)
        assertFalse(paused.running)
        assertEquals(20 * min, paused.remainingMs(t0 + 60 * min))
        val resumed = paused.resume(t0 + 60 * min)
        assertEquals(t0 + 80 * min, resumed.endAtMs)
        assertEquals(15 * min, resumed.remainingMs(t0 + 65 * min))
    }

    @Test
    fun addFiveWorksWhileRunningPausedAndAfterTimeIsUp() {
        val running = NowTask.start("Read", 10, t0).addMinutes(t0, 5)
        assertEquals(15 * min, running.remainingMs(t0))
        assertEquals(15 * min, running.totalMs)

        val paused = NowTask.start("Read", 10, t0).pause(t0 + 2 * min).addMinutes(t0 + 3 * min, 5)
        assertEquals(13 * min, paused.remainingMs(t0 + 30 * min))

        val late = NowTask.start("Read", 10, t0)
        assertTrue(late.timeIsUp(t0 + 12 * min))
        val extended = late.addMinutes(t0 + 12 * min, 5)
        assertEquals(5 * min, extended.remainingMs(t0 + 12 * min))
        assertFalse(extended.timeIsUp(t0 + 12 * min))
    }

    @Test
    fun completeRecordsMinutesSpent() {
        val done = NowTask.start("Read", 25, t0).complete(t0 + 18 * min + 20_000)
        assertEquals(NowPhase.Done, done.phase)
        assertFalse(done.running)
        assertEquals(18, done.spentMinutes)
        assertFalse(done.timeIsUp(t0 + 60 * min))
    }

    @Test
    fun breakIsFiveMinutesInTheBreakPhase() {
        val brk = NowTask.start("Read", 25, t0).complete(t0 + 25 * min).startBreak(t0 + 26 * min)
        assertEquals(NowPhase.Break, brk.phase)
        assertEquals(5 * min, brk.remainingMs(t0 + 26 * min))
        assertEquals(t0 + 26 * min, brk.startedAtMs)
    }

    @Test
    fun taskWithoutTimerHasNoCountdown() {
        val task = NowTask.withoutTimer("Tidy the desk")
        assertFalse(task.hasTimer)
        assertFalse(task.timeIsUp(t0))
        assertEquals(task, task.resume(t0))
        val timed = task.startTimer(t0, 15)
        assertEquals(15 * min, timed.remainingMs(t0))
        assertEquals("Tidy the desk", timed.title)
    }
}

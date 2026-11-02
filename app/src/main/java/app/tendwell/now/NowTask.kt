package app.tendwell.now

import kotlin.math.max
import kotlin.math.roundToInt

enum class NowPhase { Task, Break, Done }

/**
 * The one task for right now, and its timer. Pure data with pure transitions,
 * so the timer logic is unit-tested without Android. All times are epoch millis.
 */
data class NowTask(
    val title: String,
    val phase: NowPhase = NowPhase.Task,
    /** Length of the current timer. 0 means the task has no timer. */
    val totalMs: Long = 0,
    /** When the current timer was started, for the "10:17 → 10:42" range. */
    val startedAtMs: Long = 0,
    /** When the timer runs out. Only meaningful while running. */
    val endAtMs: Long = 0,
    /** Time left when paused. Only meaningful while paused. */
    val pausedRemainingMs: Long = 0,
    val running: Boolean = false,
    /** Minutes spent on the task, filled in when it is marked done. */
    val spentMinutes: Int = 0,
) {
    val hasTimer: Boolean get() = totalMs > 0

    fun remainingMs(now: Long): Long = when {
        !hasTimer -> 0
        running -> max(0, endAtMs - now)
        else -> pausedRemainingMs
    }

    /** The timer has run out but the user has not said done yet. */
    fun timeIsUp(now: Long): Boolean = hasTimer && phase != NowPhase.Done && remainingMs(now) == 0L

    /** Fraction of the timer still left, 0..1. */
    fun fractionLeft(now: Long): Float = if (!hasTimer) 0f else remainingMs(now).toFloat() / totalMs

    /** End time to show: the real end while running, or a projected end if resumed now. */
    fun projectedEndMs(now: Long): Long = if (running) endAtMs else now + pausedRemainingMs

    fun pause(now: Long): NowTask =
        if (!running) this else copy(running = false, pausedRemainingMs = remainingMs(now))

    fun resume(now: Long): NowTask =
        if (running || !hasTimer || phase == NowPhase.Done) this
        else copy(running = true, endAtMs = now + pausedRemainingMs)

    fun addMinutes(now: Long, minutes: Int): NowTask {
        if (!hasTimer || phase == NowPhase.Done) return this
        val extra = minutes * MINUTE
        return if (running) {
            copy(totalMs = totalMs + extra, endAtMs = max(endAtMs, now) + extra)
        } else {
            copy(totalMs = totalMs + extra, pausedRemainingMs = pausedRemainingMs + extra)
        }
    }

    /** Starts a timer on a task that was saved without one. */
    fun startTimer(now: Long, minutes: Int): NowTask =
        copy(phase = NowPhase.Task, totalMs = minutes * MINUTE, startedAtMs = now, endAtMs = now + minutes * MINUTE, running = true, pausedRemainingMs = 0)

    fun complete(now: Long): NowTask {
        val spent = if (hasTimer) ((totalMs - remainingMs(now)).toDouble() / MINUTE).roundToInt().coerceAtLeast(1) else 0
        return copy(phase = NowPhase.Done, running = false, spentMinutes = spent, pausedRemainingMs = 0)
    }

    fun startBreak(now: Long, minutes: Int = BREAK_MINUTES): NowTask =
        copy(phase = NowPhase.Break, totalMs = minutes * MINUTE, startedAtMs = now, endAtMs = now + minutes * MINUTE, running = true, pausedRemainingMs = 0)

    companion object {
        const val MINUTE = 60_000L
        const val BREAK_MINUTES = 5
        val DURATIONS = listOf(10, 15, 25, 45, 60)
        const val DEFAULT_MINUTES = 25

        fun start(title: String, minutes: Int, now: Long) =
            NowTask(title = title.trim()).startTimer(now, minutes)

        fun withoutTimer(title: String) = NowTask(title = title.trim())
    }
}

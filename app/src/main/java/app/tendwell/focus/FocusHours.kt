package app.tendwell.focus

import app.tendwell.data.Prefs
import app.tendwell.timebar.TimeBarModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * One set of focus hours, like "Work hours, weekdays 9:00 → 17:00". While it
 * runs, paused apps wait [pauseSeconds] instead of the usual 10 seconds.
 * An end at or before the start means the block runs past midnight.
 */
data class FocusBlock(
    val id: String,
    val name: String,
    val on: Boolean,
    /** Bit 0 is Monday, bit 6 is Sunday. */
    val days: Int,
    val startMin: Int,
    val endMin: Int,
    val pauseSeconds: Int,
) {
    fun hasDay(day: DayOfWeek): Boolean = days and (1 shl (day.value - 1)) != 0
    val crossesMidnight: Boolean get() = endMin <= startMin
}

/** Where focus hours stand right now, for the status card. */
sealed interface FocusStatus {
    data class On(val block: FocusBlock, val untilMin: Int, val minutesLeft: Int) : FocusStatus
    data class Next(val block: FocusBlock, val startMin: Int, val minutesUntil: Int) : FocusStatus
    data object Off : FocusStatus
}

object FocusHours {
    const val ALL_DAYS = 0b1111111
    const val WEEKDAYS = 0b0011111
    const val WEEKENDS = 0b1100000
    val PAUSE_OPTIONS = listOf(30, 60, 120)
    private const val DAY = 24 * 60

    fun defaults(): List<FocusBlock> = listOf(
        FocusBlock("work", "Work hours", false, WEEKDAYS, 9 * 60, 17 * 60, 30),
        FocusBlock("evenings", "Evenings", false, ALL_DAYS, 20 * 60, 23 * 60, 30),
    )

    /** One-tap suggestions shown under the user's own blocks. */
    val presets: List<FocusBlock> = listOf(
        FocusBlock("deep", "Deep work mornings", true, WEEKDAYS, 8 * 60, 12 * 60, 30),
        FocusBlock("wind", "Wind down for bed", true, ALL_DAYS, 21 * 60, 23 * 60, 30),
    )

    /** The block running at [at], or null. When two overlap, the longer pause wins. */
    fun activeAt(blocks: List<FocusBlock>, at: LocalDateTime): FocusBlock? {
        val t = at.hour * 60 + at.minute
        val today = at.dayOfWeek
        val yesterday = today.minus(1)
        return blocks.filter { b ->
            b.on && b.startMin != b.endMin && if (!b.crossesMidnight) {
                b.hasDay(today) && t >= b.startMin && t < b.endMin
            } else {
                (b.hasDay(today) && t >= b.startMin) || (b.hasDay(yesterday) && t < b.endMin)
            }
        }.maxByOrNull { it.pauseSeconds }
    }

    /** How long the pause screen waits right now. Focus hours only apply with Pro. */
    fun pauseSeconds(blocks: List<FocusBlock>, at: LocalDateTime, isPro: Boolean): Int =
        if (isPro) activeAt(blocks, at)?.pauseSeconds ?: Prefs.PAUSE_SECONDS else Prefs.PAUSE_SECONDS

    /** Minutes of [date] covered by focus hours, as [start, end) pairs on a 0..1440 line. */
    fun segmentsOn(blocks: List<FocusBlock>, date: LocalDate): List<Pair<Int, Int>> {
        val day = date.dayOfWeek
        return blocks.filter { it.on && it.startMin != it.endMin }.flatMap { b ->
            if (!b.crossesMidnight) {
                if (b.hasDay(day)) listOf(b.startMin to b.endMin) else emptyList()
            } else {
                listOfNotNull(
                    if (b.hasDay(day.minus(1)) && b.endMin > 0) 0 to b.endMin else null,
                    if (b.hasDay(day)) b.startMin to DAY else null,
                )
            }
        }.sortedBy { it.first }
    }

    fun status(blocks: List<FocusBlock>, now: LocalDateTime): FocusStatus {
        val t = now.hour * 60 + now.minute
        activeAt(blocks, now)?.let { b ->
            val left = if (t < b.endMin) b.endMin - t else b.endMin + DAY - t
            return FocusStatus.On(b, b.endMin, left)
        }
        val next = blocks
            .filter { it.on && it.startMin != it.endMin && it.hasDay(now.dayOfWeek) && it.startMin > t }
            .minByOrNull { it.startMin }
        return if (next != null) FocusStatus.Next(next, next.startMin, next.startMin - t) else FocusStatus.Off
    }

    /** "Every day", "Weekdays", "Weekends", or runs like "Mon–Wed, Sat". */
    fun daysLabel(days: Int): String {
        val picked = (0 until 7).map { days and (1 shl it) != 0 }
        return when {
            days and ALL_DAYS == ALL_DAYS -> "Every day"
            days and ALL_DAYS == WEEKDAYS -> "Weekdays"
            days and ALL_DAYS == WEEKENDS -> "Weekends"
            picked.none { it } -> "No days picked"
            else -> {
                val parts = mutableListOf<String>()
                var i = 0
                while (i < 7) {
                    if (!picked[i]) { i++; continue }
                    var j = i
                    while (j + 1 < 7 && picked[j + 1]) j++
                    if (j - i >= 2) parts += "${SHORT[i]}–${SHORT[j]}" else (i..j).forEach { parts += SHORT[it] }
                    i = j + 1
                }
                parts.joinToString(", ")
            }
        }
    }

    fun range(b: FocusBlock): String = "${TimeBarModel.formatClock(b.startMin)} → ${TimeBarModel.formatClock(b.endMin % DAY)}"

    fun summary(b: FocusBlock): String = "${daysLabel(b.days)} · ${range(b)}"

    fun pauseLabel(seconds: Int): String = if (seconds < 60) "$seconds s" else "${seconds / 60} min"

    /** "6h 36m", "2h", "45m". */
    fun durationLabel(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h == 0 -> "${m}m"
            m == 0 -> "${h}h"
            else -> "${h}h ${m}m"
        }
    }

    private val SHORT = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    /** Saved form: one block per line, "id|on|days|start|end|seconds|name". */
    fun encode(blocks: List<FocusBlock>): String = blocks.joinToString("\n") { b ->
        listOf(b.id, if (b.on) "1" else "0", b.days, b.startMin, b.endMin, b.pauseSeconds, b.name.replace('\n', ' ')).joinToString("|")
    }

    /** Reads the saved form; null (nothing saved yet) gives the defaults. Bad lines are skipped. */
    fun decode(raw: String?): List<FocusBlock> {
        if (raw == null) return defaults()
        return raw.split('\n').mapNotNull { line ->
            val p = line.split('|', limit = 7)
            if (p.size != 7) return@mapNotNull null
            val days = p[2].toIntOrNull() ?: return@mapNotNull null
            val start = p[3].toIntOrNull()?.takeIf { it in 0 until DAY } ?: return@mapNotNull null
            val end = p[4].toIntOrNull()?.takeIf { it in 0 until DAY } ?: return@mapNotNull null
            val secs = p[5].toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
            FocusBlock(p[0], p[6], p[1] == "1", days and ALL_DAYS, start, end, secs)
        }
    }
}

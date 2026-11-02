package app.tendwell.insights

import app.tendwell.data.OpenEvent
import app.tendwell.data.PauseOutcome
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

data class AppWeek(val appKey: String, val opens: Int, val turnedBack: Int)

/** One Monday-to-Sunday week of pause-screen outcomes, plus the week before for comparison. */
data class WeekStats(
    val weekStart: LocalDate,
    val opens: Int,
    val turnedBack: Int,
    /** Opens per day, Monday first. */
    val opensPerDay: List<Int>,
    /** Opens per time-of-day slot, see [WeekInsights.SLOT_LABELS]. */
    val opensPerSlot: List<Int>,
    /** Apps by opens, most first. */
    val byApp: List<AppWeek>,
    val prevOpens: Int,
    val prevTurnedBack: Int,
    val hasPrevWeek: Boolean,
) {
    val isEmpty: Boolean get() = opens == 0 && turnedBack == 0
}

/** Pure maths and wording for the weekly insights screen. */
object WeekInsights {
    /** Three-hour slots from 6:00; the last one runs through the night to 6:00. */
    val SLOT_LABELS = listOf("6–9", "9–12", "12–15", "15–18", "18–21", "21–6")

    fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun slotOf(hour: Int): Int = if (hour < 6) 5 else ((hour - 6) / 3).coerceAtMost(5)

    fun stats(events: List<OpenEvent>, weekStart: LocalDate, zone: ZoneId): WeekStats {
        val start = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = weekStart.plusDays(7).atStartOfDay(zone).toInstant().toEpochMilli()
        val prevStart = weekStart.minusDays(7).atStartOfDay(zone).toInstant().toEpochMilli()
        val week = events.filter { it.epochMs in start until end }
        val prev = events.filter { it.epochMs in prevStart until start }
        val opened = week.filter { it.outcome == PauseOutcome.Opened }
        val perDay = IntArray(7)
        val perSlot = IntArray(6)
        opened.forEach { e ->
            val t = Instant.ofEpochMilli(e.epochMs).atZone(zone)
            perDay[t.dayOfWeek.value - 1]++
            perSlot[slotOf(t.hour)]++
        }
        val byApp = week.groupBy { it.appKey }.map { (key, list) ->
            AppWeek(key, list.count { it.outcome == PauseOutcome.Opened }, list.count { it.outcome == PauseOutcome.TurnedBack })
        }.sortedWith(compareByDescending<AppWeek> { it.opens }.thenByDescending { it.turnedBack })
        return WeekStats(
            weekStart = weekStart,
            opens = opened.size,
            turnedBack = week.count { it.outcome == PauseOutcome.TurnedBack },
            opensPerDay = perDay.toList(),
            opensPerSlot = perSlot.toList(),
            byApp = byApp,
            prevOpens = prev.count { it.outcome == PauseOutcome.Opened },
            prevTurnedBack = prev.count { it.outcome == PauseOutcome.TurnedBack },
            hasPrevWeek = prev.isNotEmpty(),
        )
    }

    /** "6 more than last week", or null when there is no last week to compare with. */
    fun turnedBackChange(s: WeekStats): String? {
        if (!s.hasPrevWeek) return null
        val d = s.turnedBack - s.prevTurnedBack
        return when {
            d > 0 -> "$d more than last week"
            d < 0 -> "${-d} fewer than last week"
            else -> "Same as last week"
        }
    }

    /** "12% fewer than last week". Fewer opens is the good direction. */
    fun opensChange(s: WeekStats): String? {
        if (!s.hasPrevWeek || s.prevOpens == 0) return null
        val pct = ((s.opens - s.prevOpens) * 100.0 / s.prevOpens).roundToInt()
        return when {
            pct < 0 -> "${-pct}% fewer than last week"
            pct > 0 -> "$pct% more than last week"
            else -> "Same as last week"
        }
    }

    /** "Calmest days: Thursday and Sunday." Only finished days count, so today is left out. */
    fun calmestDays(s: WeekStats, today: LocalDate, locale: Locale = Locale.getDefault()): String? {
        if (s.opens == 0) return null
        val daysDone = ChronoUnit.DAYS.between(s.weekStart, today).toInt().coerceIn(0, 7)
        if (daysDone < 2) return null
        val days = (0 until daysDone).map { it to s.opensPerDay[it] }
        val least = days.minOf { it.second }
        val names = days.filter { it.second == least }.map { (i, _) ->
            DayOfWeek.of(i + 1).getDisplayName(TextStyle.FULL, locale)
        }
        if (names.size == days.size) return null
        val list = if (names.size == 1) names[0] else names.dropLast(1).joinToString(", ") + " and " + names.last()
        return if (names.size == 1) "Calmest day: $list." else "Calmest days: $list."
    }

    /** Index of the busiest slot, or null when nothing was opened. */
    fun driftSlot(s: WeekStats): Int? {
        val max = s.opensPerSlot.maxOrNull() ?: return null
        if (max == 0) return null
        return s.opensPerSlot.lastIndexOf(max)
    }

    /** "You drift most between 21:00 and 6:00." */
    fun driftSentence(slot: Int): String {
        val from = 6 + slot * 3
        val to = if (slot == 5) "6:00" else "${from + 3}:00"
        return "You drift most between $from:00 and $to."
    }

    /** "This week", "Last week" or "2 weeks ago". */
    fun weekName(weekStart: LocalDate, today: LocalDate): String =
        when (val back = ChronoUnit.WEEKS.between(weekStart, weekStart(today)).toInt()) {
            0 -> "This week"
            1 -> "Last week"
            else -> "$back weeks ago"
        }

    /** "26 Oct – 1 Nov". */
    fun weekRange(weekStart: LocalDate, locale: Locale = Locale.getDefault()): String {
        val f = DateTimeFormatter.ofPattern("d MMM", locale)
        return "${weekStart.format(f)} – ${weekStart.plusDays(6).format(f)}"
    }
}

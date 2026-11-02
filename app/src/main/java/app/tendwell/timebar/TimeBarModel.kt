package app.tendwell.timebar

/** A calendar event today, in minutes from midnight. */
data class DayEvent(val title: String, val startMin: Int, val endMin: Int)

/**
 * The time bar's maths: where "now", past time and events sit on a bar that
 * runs from the start of the day to bedtime, and the labels above it.
 * Bedtimes after midnight (for example 00:30) are handled by moving them to
 * the next day.
 */
class TimeBarModel(
    dayStartMin: Int,
    bedtimeMin: Int,
    nowMin: Int,
    events: List<DayEvent>,
) {
    val dayStart: Int = dayStartMin
    val bedtime: Int = if (bedtimeMin <= dayStartMin) bedtimeMin + DAY else bedtimeMin
    /** "Now" on the same scale: just after midnight but before a late bedtime counts as tonight. */
    val now: Int = if (nowMin < dayStartMin && nowMin + DAY < bedtime) nowMin + DAY else nowMin

    val events: List<DayEvent> = events
        .filter { it.endMin > it.startMin }
        .sortedBy { it.startMin }

    private val span: Int get() = bedtime - dayStart

    fun fraction(min: Int): Float = ((min - dayStart).toFloat() / span).coerceIn(0f, 1f)

    val nowFraction: Float get() = fraction(now)

    val current: DayEvent? get() = events.firstOrNull { it.startMin <= now && now < it.endMin }
    val next: DayEvent? get() = events.firstOrNull { it.startMin > now }

    val minutesUntilBed: Int get() = bedtime - now

    val pastBedtime: Boolean get() = now >= bedtime

    /** Main label, for example "36 min until Stand-up". */
    val primaryLabel: String
        get() {
            current?.let { return "${it.title} until ${formatClock(it.endMin)}" }
            next?.let { return "${formatDuration(it.startMin - now)} until ${it.title}" }
            return bedLabel
        }

    /** Secondary label shown on the right, or null when the main label already is the bedtime countdown. */
    val secondaryLabel: String?
        get() = if (current == null && next == null) null else bedLabel

    private val bedLabel: String
        get() = if (pastBedtime) "Past bedtime" else "${formatDuration(minutesUntilBed)} until bed"

    /** Hour marks under the bar: the start, two round hours in between, and bedtime. */
    val axis: List<Pair<Float, String>>
        get() {
            val hours = span / 60
            val step = Math.round(hours / 3f).coerceAtLeast(1)
            val startHour = dayStart / 60
            val marks = mutableListOf(0f to formatClock(dayStart))
            for (k in 1..2) {
                val m = (startHour + k * step) * 60
                if (m < bedtime - 90) marks += fraction(m) to formatClock(m)
            }
            marks += 1f to "Bed ${formatClock(bedtime)}"
            return marks
        }

    /** Spoken description for screen readers. */
    val description: String
        get() = buildString {
            append("Your day from ${formatClock(dayStart)} to bedtime at ${formatClock(bedtime)}. ")
            append("It is ${formatClock(now)}. ")
            val upcoming = events.filter { it.endMin > now }
            if (upcoming.isNotEmpty()) {
                append("Coming up: ")
                append(upcoming.joinToString(", ") { "${it.title} at ${formatClock(it.startMin)}" })
                append(".")
            }
        }

    companion object {
        const val DAY = 24 * 60

        /** 24-hour time without a leading zero on the hour, like "7:00" or "13:05". */
        fun formatClock(min: Int): String {
            val m = ((min % DAY) + DAY) % DAY
            return "${m / 60}:${(m % 60).toString().padStart(2, '0')}"
        }

        /** "36 min", "2h", "12h 36m". */
        fun formatDuration(minutes: Int): String {
            val m = minutes.coerceAtLeast(0)
            return when {
                m < 60 -> "$m min"
                m % 60 == 0 -> "${m / 60}h"
                else -> "${m / 60}h ${m % 60}m"
            }
        }
    }
}

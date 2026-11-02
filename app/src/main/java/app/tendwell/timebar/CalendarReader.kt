package app.tendwell.timebar

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Reads today's timed events from the phone's calendars. Nothing leaves the
 * phone. Returns an empty list if calendar access has not been granted.
 */
object CalendarReader {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun todaysEvents(context: Context, today: LocalDate = LocalDate.now()): List<DayEvent> {
        if (!hasPermission(context)) return emptyList()
        val zone = ZoneId.systemDefault()
        val dayStart = today.atStartOfDay(zone)
        val begin = dayStart.toInstant().toEpochMilli()
        val end = dayStart.plusDays(1).toInstant().toEpochMilli()

        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, begin)
            ContentUris.appendId(it, end)
        }.build()
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
        )
        val selection = "${CalendarContract.Instances.ALL_DAY} = 0 AND " +
            "${CalendarContract.Instances.VISIBLE} = 1 AND " +
            "${CalendarContract.Instances.SELF_ATTENDEE_STATUS} != ${CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED}"

        val events = mutableListOf<DayEvent>()
        try {
            context.contentResolver.query(uri, projection, selection, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
                while (c.moveToNext()) {
                    val title = c.getString(0)?.trim().orEmpty().ifEmpty { "Event" }
                    val startMin = minutesIntoDay(c.getLong(1), today, zone)
                    val endMin = minutesIntoDay(c.getLong(2), today, zone)
                    events += DayEvent(title, startMin, endMin)
                }
            }
        } catch (e: SecurityException) {
            return emptyList()
        }
        return events
    }

    /** Minutes from midnight; clamped to the day so events crossing midnight still draw. */
    private fun minutesIntoDay(epochMs: Long, today: LocalDate, zone: ZoneId): Int {
        val t = Instant.ofEpochMilli(epochMs).atZone(zone)
        return when {
            t.toLocalDate().isBefore(today) -> 0
            t.toLocalDate().isAfter(today) -> 24 * 60
            else -> t.hour * 60 + t.minute
        }
    }
}

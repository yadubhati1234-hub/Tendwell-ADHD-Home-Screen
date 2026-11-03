package app.tendwell.screenshots

import android.content.ComponentName
import android.os.Process
import app.tendwell.apps.AppEntry
import app.tendwell.billing.PlanOffer
import app.tendwell.billing.ProPlan
import app.tendwell.checkin.CheckInPick
import app.tendwell.data.OneThing
import app.tendwell.data.OpenEvent
import app.tendwell.data.PauseOutcome
import app.tendwell.focus.FocusHours
import app.tendwell.now.NowTask
import app.tendwell.timebar.DayEvent
import app.tendwell.timebar.TimeBarModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Made-up content for the screenshot tests. App names are generic on purpose
 * (Videos, Social, Clips), like the store screenshots, but the package names
 * are real ones so the pause step's suggestions pick them up.
 */
object FakeData {
    private fun app(label: String, pkg: String, work: Boolean = false) = AppEntry(
        key = "$pkg/$pkg.Main#${if (work) 10 else 0}",
        label = label,
        component = ComponentName(pkg, "$pkg.Main"),
        user = Process.myUserHandle(),
        isWorkProfile = work,
    )

    val phone = app("Phone", "com.google.android.dialer")
    val messages = app("Messages", "com.google.android.apps.messaging")
    val calendar = app("Calendar", "com.google.android.calendar")
    val maps = app("Maps", "com.google.android.apps.maps")
    val notes = app("Notes", "com.google.android.keep")
    val camera = app("Camera", "com.google.android.GoogleCamera")
    val bank = app("Bank", "com.example.bank")
    val mail = app("Mail", "com.google.android.gm")
    val workChat = app("Chat", "com.example.chat", work = true)
    val videos = app("Videos", "com.google.android.youtube")
    val social = app("Social", "com.instagram.android")
    val clips = app("Clips", "com.zhiliaoapp.musically")
    val forum = app("Forum", "com.reddit.frontpage")
    val news = app("News", "com.google.android.apps.magazines")

    val allApps: List<AppEntry> = listOf(
        phone, messages, calendar, maps, notes, camera, bank, mail, workChat, videos, social, clips, forum, news,
    ).sortedBy { it.label.lowercase() }

    val essentials = listOf(phone, messages, calendar, maps, notes)
    val pausedKeys = setOf(videos.key, social.key, clips.key)

    /** Tuesday 3 November 2026, 10:42, the fixed "now" for screens that take a time. */
    val now: LocalDateTime = LocalDateTime.of(2026, 11, 3, 10, 42)
    val today: LocalDate = now.toLocalDate()
    private val nowMin = now.hour * 60 + now.minute

    val events = listOf(
        DayEvent("Team call", 11 * 60, 11 * 60 + 30),
        DayEvent("Lunch with Sam", 13 * 60, 14 * 60),
        DayEvent("Dentist", 16 * 60 + 30, 17 * 60 + 15),
    )

    fun timeBar(withEvents: Boolean = true) = TimeBarModel(
        dayStartMin = 7 * 60,
        bedtimeMin = 23 * 60,
        nowMin = nowMin,
        events = if (withEvents) events else emptyList(),
    )

    val oneThing = OneThing("Send the invoice", today, done = false)

    /** Timers run on the real clock, so they start relative to the moment the test runs. */
    private fun realNow() = System.currentTimeMillis()
    fun runningTask() = NowTask.start("Write the report", 25, realNow() - 7 * NowTask.MINUTE)
    fun pausedTask() = runningTask().let { it.copy(running = false, pausedRemainingMs = it.remainingMs(realNow())) }
    fun timeUpTask() = NowTask.start("Write the report", 25, realNow() - 26 * NowTask.MINUTE)
    fun doneTask() = runningTask().complete(realNow())
    fun breakTask() = doneTask().startBreak(realNow() - 2 * NowTask.MINUTE)
    fun noTimerTask() = NowTask.withoutTimer("Tidy the desk")

    val checkInPicks = listOf(
        CheckInPick("Send the invoice", "from yesterday"),
        CheckInPick("Write the report"),
        CheckInPick("Book the dentist"),
    )

    val offers = mapOf(
        ProPlan.Yearly to PlanOffer(ProPlan.Yearly, "$24.99", 24_990_000, "USD", trialDays = 7, offerToken = "yearly"),
        ProPlan.Monthly to PlanOffer(ProPlan.Monthly, "$3.99", 3_990_000, "USD", trialDays = 7, offerToken = "monthly"),
        ProPlan.Lifetime to PlanOffer(ProPlan.Lifetime, "$49.99", 49_990_000, "USD", trialDays = 0, offerToken = null),
    )

    /** Work hours on (it's a Tuesday morning, so they're running), evenings off. */
    val focusBlocks = FocusHours.defaults().map { if (it.id == "work") it.copy(on = true) else it }

    /** The week shown on the insights screen: Monday 26 October to Sunday 1 November. */
    val insightsWeek: LocalDate = LocalDate.of(2026, 10, 26)

    /** Two weeks of pause outcomes, the week before slightly busier, mostly in the evening. */
    fun insightEvents(zone: ZoneId = ZoneId.systemDefault()): List<OpenEvent> {
        val apps = listOf(videos.key, social.key, clips.key)
        val hours = listOf(8, 13, 16, 21, 22, 23, 10)
        val out = mutableListOf<OpenEvent>()
        for (dayOffset in -7 until 7) {
            val date = insightsWeek.plusDays(dayOffset.toLong())
            val count = if (dayOffset < 0) 6 + (dayOffset + 7) % 3 else 2 + (dayOffset * 5) % 5
            repeat(count) { i ->
                val hour = hours[(i * 3 + dayOffset + 7) % hours.size]
                val at = date.atTime(hour, (i * 7) % 60).atZone(zone).toInstant().toEpochMilli()
                val outcome = if ((i + dayOffset + 7) % 3 == 0) PauseOutcome.TurnedBack else PauseOutcome.Opened
                out += OpenEvent(at, outcome, apps[(i + dayOffset + 7) % apps.size])
            }
        }
        return out
    }

    fun label(key: String): String = allApps.firstOrNull { it.key == key }?.label ?: key.substringBefore('/')
}

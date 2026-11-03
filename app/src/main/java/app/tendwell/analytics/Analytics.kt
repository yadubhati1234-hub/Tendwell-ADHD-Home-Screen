package app.tendwell.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Anonymous feature events sent to Google Analytics for Firebase.
 *
 * Every event name and parameter is fixed in this file, so task text, check-in answers,
 * calendar data and app names can never reach analytics. The privacy policy (docs/privacy) and
 * the Play Data safety form promise that, so keep it true when adding events. Firebase also
 * sends its own automatic events (first open, sessions, in-app purchases).
 *
 * Collection is off in debug builds (manifest placeholder `firebaseCollection`).
 */
class Analytics(context: Context) {
    // Created on first use, so nothing here runs before Home's first frame.
    private val firebase by lazy { FirebaseAnalytics.getInstance(context.applicationContext) }

    fun onboardingDone() = log("onboarding_done")
    fun checkInSet() = log("check_in_set")
    fun checkInSkipped() = log("check_in_skipped")
    fun nowStarted(minutes: Int) = log("now_started", "minutes" to minutes.toLong())
    fun nowSavedWithoutTimer() = log("now_saved_no_timer")
    fun nowDone() = log("now_done")
    fun breakStarted() = log("break_started")
    fun pauseShown(seconds: Int) = log("pause_shown", "seconds" to seconds.toLong())
    fun pauseTurnedBack() = log("pause_turned_back")
    fun pauseOpenedAnyway() = log("pause_opened_anyway")
    fun paywallShown(from: PaywallFrom) = log("paywall_shown", "from" to from.id)
    fun insightsOpened() = log("insights_opened")
    fun focusHoursOpened() = log("focus_hours_opened")

    private var crashReportsLinked = false

    /**
     * The random ID Firebase links this install's usage events to. People send it to have their
     * data deleted (docs/delete-data). Null when collection is off, as in debug builds, or when
     * Firebase doesn't answer within a few seconds.
     */
    suspend fun appInstanceId(): String? = withTimeoutOrNull(5_000) {
        suspendCancellableCoroutine<String?> { cont ->
            runCatching {
                firebase.appInstanceId
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
            }.onFailure { cont.resume(null) }
        }
    }

    /**
     * Tags crash reports with the same ID as the usage events, so the one ID people copy from
     * Settings is enough to delete both (Crashlytics deletes crash reports by user ID). Runs once
     * per process, after Home is up.
     */
    suspend fun linkCrashReports() {
        if (crashReportsLinked) return
        val id = appInstanceId() ?: return
        runCatching { FirebaseCrashlytics.getInstance().setUserId(id) }
        crashReportsLinked = true
    }

    private fun log(event: String, vararg params: Pair<String, Any>) {
        runCatching {
            val bundle = Bundle()
            for ((key, value) in params) {
                when (value) {
                    is Long -> bundle.putLong(key, value)
                    is String -> bundle.putString(key, value)
                }
            }
            firebase.logEvent(event, bundle)
        }
    }
}

/** Where the paywall was opened from, so the stats show which feature leads people to Pro. */
enum class PaywallFrom(val id: String) {
    /** The last onboarding step. */
    Onboarding("onboarding"),
    /** The Pro card on the onboarding step that picks paused apps. */
    PauseAppsStep("pause_apps_step"),
    /** Settings' Pro row. */
    Settings("settings"),
    /** Settings' focus hours row, without Pro. */
    FocusHours("focus_hours"),
    /** Settings' weekly insights row, without Pro. */
    Insights("insights"),
    /** The long-press sheet, when a 4th app is paused without Pro. */
    AppActions("app_actions"),
}

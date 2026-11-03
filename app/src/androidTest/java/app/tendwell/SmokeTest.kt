package app.tendwell

import android.Manifest
import android.graphics.Bitmap
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.printToLog
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import app.tendwell.data.Prefs
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Walks a fresh install through every main screen on a real emulator, the way
 * a new user would: onboarding, the check-in, Home, a Now task with its
 * timer and break, search, the long-press sheet, the pause, Settings, the
 * paywall, and (with Pro switched on) weekly insights and focus hours.
 *
 * Any crash fails the test. Each step also saves a screenshot, which CI
 * pushes to a shots/ branch so the real rendering can be checked.
 *
 * The emulator has no Play Store, so the paywall is expected to say Google
 * Play is unavailable; buying is tested by hand on a phone.
 *
 * The walk runs with animations on, like most phones. At the end it turns
 * them off (Android's "Remove animations", which many people with ADHD use)
 * and checks the bottom sheets still open.
 */
@RunWith(AndroidJUnit4::class)
class SmokeTest {
    private val compose = createAndroidComposeRule<MainActivity>()

    // Notifications are allowed up front so the permission prompt doesn't cover the timer.
    @get:Rule
    val rules: TestRule = RuleChain
        .outerRule(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                TestRule { base, _ -> base }
            },
        )
        // CI starts the emulator with animations off; most phones have them on.
        .around(object : ExternalResource() {
            override fun before() {
                setAnimations(true)
                // A slow emulator's own launcher can raise "isn't responding" dialogs over the app.
                shell("settings put global hide_error_dialogs 1")
                shell("settings put secure anr_show_background 0")
            }
        })
        .around(compose)

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private var shotNumber = 0

    @Test
    fun freshInstallThroughEveryMainScreen() {
        // 1. Welcome. The first frame can reach the screen a moment after it is composed.
        waitForText("Get started")
        Thread.sleep(1_500)
        snap("onboarding-welcome")
        clickText("Get started")

        // 2. Essential apps. Pick a few if nothing is picked yet.
        waitForText("Pick your essential apps")
        waitUntil("the app list to load", 20_000) { candidates.any(::hasAppRow) }
        if (exists(hasText("Pick at least one app"))) {
            candidates.filter(::hasAppRow).take(3).forEach { clickText(it) }
        }
        snap("onboarding-essentials")
        clickText("Continue")

        // 3. Apps to pause: pick one we can find again in search.
        waitForText("Which apps pull you in?")
        val pausedApp = pauseCandidates.first(::hasAppRow)
        clickText(pausedApp)
        snap("onboarding-pause-apps")
        clickText("Continue")

        // 4. Set as home screen: skip, so the system's role dialog doesn't open.
        waitForText("home screen", substring = true)
        snap("onboarding-set-home")
        clickText("Not now")

        // 5. Paywall. With no Play Store it should settle on an error, not hang or crash.
        waitUntil("the paywall", 10_000) { exists(hasContentDescription("Not now")) }
        waitUntil("Google Play to answer", 20_000) { !exists(hasText("Loading plans from Google Play…")) }
        snap("onboarding-paywall")
        clickDescription("Not now")

        // 6. The daily check-in opens the first time Home shows.
        waitForText("Set my one thing")
        snap("check-in")
        compose.onNode(hasContentDescription("Your one thing")).performTextInput("Send the invoice")
        clickText("Set my one thing")

        // 7. Home with the one thing.
        waitForText("Send the invoice")
        snap("home")

        // 8. A Now task with a timer.
        clickText("Set a Now task")
        waitForText("Save without a timer")
        compose.onNode(hasContentDescription("Task for now")).performTextInput("Write the report")
        snap("now-sheet")
        clickText("Start timer")

        // 9. The focus timer: pause, resume, +5 min, done, a break, end the break.
        waitUntil("the timer", 10_000) { exists(hasContentDescription("Pause timer")) }
        snap("timer")
        clickDescription("Pause timer")
        waitUntil("the timer to pause", 5_000) { exists(hasContentDescription("Resume timer")) }
        snap("timer-paused")
        clickDescription("Resume timer")
        clickText("+5 min")
        clickText("Done")
        waitForText("Done. That counts.")
        snap("timer-done")
        clickText("min break", substring = true)
        waitForText("End")
        snap("timer-break")
        clickText("End")
        waitForText("Send the invoice")
        snap("home-after-timer")

        // 10. Search, and the long-press sheet for the paused app.
        clickText("Search apps")
        waitUntil("the search field", 5_000) { exists(hasContentDescription("Search apps") and hasSetTextAction()) }
        compose.onNode(hasContentDescription("Search apps") and hasSetTextAction()).performTextInput(pausedApp)
        // A result row reads as one item to TalkBack ("YouTube, opens after a 10 second pause").
        val result = hasContentDescription(pausedApp, substring = true) and !hasSetTextAction()
        waitUntil("$pausedApp in the results", 5_000) { exists(result) }
        snap("search")
        compose.onAllNodes(result).onFirst().performTouchInput { longClick() }
        waitForText("Stop pausing")
        snap("app-actions")
        Espresso.pressBack()
        waitUntil("the sheet to close", 5_000) { !exists(hasText("Stop pausing")) }

        // 11. The pause before the paused app, then back to the task.
        compose.onAllNodes(result).onFirst().performClick()
        waitForText("Back to my task")
        snap("pause")
        waitForText("anyway", substring = true)
        Thread.sleep(11_000)
        waitForText("Open $pausedApp anyway")
        snap("pause-over")
        clickText("Back to my task")
        waitForText("Send the invoice")

        // 12. Settings and the paywall from there.
        clickDescription("Settings")
        waitForText("Bedtime")
        snap("settings")
        compose.onAllNodes(hasText("See Tendwell Pro")).onFirst().performScrollTo().performClick()
        waitUntil("the paywall", 10_000) { exists(hasContentDescription("Not now")) }
        waitUntil("Google Play to answer", 20_000) { !exists(hasText("Loading plans from Google Play…")) }
        snap("paywall")
        clickDescription("Not now")
        waitForText("Bedtime")

        // 13. Pro screens, with Pro switched on the way an access code does it.
        runBlocking { Prefs(instrumentation.targetContext).setAccessCodePro() }
        waitForText("Tendwell Pro is on")
        snap("settings-pro")
        compose.onAllNodes(hasText("Weekly insights")).onFirst().performScrollTo().performClick()
        waitForText("Your week")
        snap("insights")
        Espresso.pressBack()
        waitForText("Bedtime")
        compose.onAllNodes(hasText("Focus hours")).onFirst().performScrollTo().performClick()
        waitForText("During these hours, your paused apps wait longer before they open.")
        snap("focus-hours")
        Espresso.pressBack()
        waitForText("Bedtime")
        Espresso.pressBack()
        waitForText("Send the invoice")
        snap("home-end")

        // 14. With animations off, the long-press sheet and the Now sheet still open.
        setAnimations(false)
        Thread.sleep(1_000)
        val homeApp = candidates.plus(listOf("Maps", "YT Music")).first(::hasAppRow)
        compose.onAllNodes(hasText(homeApp) and !hasSetTextAction()).onFirst().performTouchInput { longClick() }
        waitUntil("the long-press sheet with animations off", 10_000) { exists(hasText("App info")) }
        snap("no-animations-app-actions")
        Espresso.pressBack()
        waitUntil("the sheet to close", 5_000) { !exists(hasText("App info")) }
        clickText("Now task", substring = true)
        waitUntil("the Now sheet with animations off", 10_000) { exists(hasText("Save without a timer")) }
        snap("no-animations-now-sheet")
        Espresso.pressBack()
    }

    /** Apps that are on every emulator image, in the order we try them. */
    private val candidates = listOf("Settings", "Camera", "Clock", "Contacts", "Phone", "Messages", "Files", "Chrome")
    private val pauseCandidates = listOf("YouTube", "Chrome", "Clock", "Contacts", "Files", "Camera")

    private fun hasAppRow(label: String) = exists(hasText(label) and !hasSetTextAction())

    private fun exists(matcher: SemanticsMatcher) = compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()

    private fun waitUntil(what: String, timeoutMs: Long, condition: () -> Boolean) {
        try {
            compose.waitUntil(timeoutMs, condition)
        } catch (e: Throwable) {
            snap("FAILED-waiting-for-${what.replace(Regex("[^A-Za-z0-9]+"), "-")}")
            // What was on screen, for the logcat CI saves.
            runCatching { compose.onAllNodes(isRoot()).printToLog("SmokeTest") }
            throw AssertionError("Timed out after ${timeoutMs / 1000} s waiting for $what", e)
        }
    }

    private fun waitForText(text: String, substring: Boolean = false) =
        waitUntil("\"$text\"", 10_000) { exists(hasText(text, substring = substring)) }

    private fun clickText(text: String, substring: Boolean = false) {
        waitForText(text, substring)
        compose.onAllNodes(hasText(text, substring = substring) and !hasSetTextAction()).onFirst().performClick()
        compose.waitForIdle()
    }

    private fun clickDescription(description: String) {
        waitUntil("\"$description\"", 10_000) { exists(hasContentDescription(description)) }
        compose.onAllNodes(hasContentDescription(description)).onFirst().performClick()
        compose.waitForIdle()
    }

    /** Sets Android's three animation scales, like Developer options or "Remove animations" do. */
    private fun setAnimations(on: Boolean) {
        val scale = if (on) "1" else "0"
        for (name in listOf("window_animation_scale", "transition_animation_scale", "animator_duration_scale")) {
            shell("settings put global $name $scale")
        }
    }

    private fun shell(command: String) {
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).use { it.readBytes() }
    }

    /**
     * Saves a half-size screenshot to the app's files folder on the emulator
     * (CI pulls it with adb) and, when AGP passes one, to the additional test
     * output folder (AGP copies that to app/build/outputs/).
     */
    private fun snap(name: String) {
        runCatching { compose.waitForIdle() }
        Thread.sleep(400)
        val full = instrumentation.uiAutomation.takeScreenshot() ?: return
        val small = Bitmap.createScaledBitmap(full, full.width / 2, full.height / 2, true)
        shotNumber += 1
        val fileName = "%02d-%s.png".format(shotNumber, name)
        val dirs = listOfNotNull(
            File(instrumentation.targetContext.getExternalFilesDir(null), "smoke"),
            InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.let(::File),
        )
        for (dir in dirs) {
            runCatching {
                dir.mkdirs()
                FileOutputStream(File(dir, fileName)).use { small.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
    }
}

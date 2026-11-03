# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Status

Scaffolded on 2026-11-02. Done: core launcher (HOME intent, Home, search, long-press actions, Settings), time bar, Now card with timer and break, daily check-in, pause screen, onboarding, the Pro paywall with Play Billing, focus hours and weekly insights. Still to come: Firebase (Crashlytics and analytics). Room turned out not to be needed so far: settings live in DataStore and the open log is a small text file.

## What this is

ADHD Launcher is an Android home-screen launcher for adults with ADHD, published through Google Play Console. Everything runs on the device: no user account, no backend server.

v1 features:
- **Time bar** showing the day's calendar events.
- **Now card** for the current task, with a timer.
- **Daily check-in.**
- **Essential apps** shown as a plain text list rather than an icon grid.
- **Pause screen**: a 10-second pause before opening a distracting app. Free for 3 apps.
- **Pro** (Play Billing subscription): focus hours and weekly insights. Prices are $3.99 monthly, $24.99 yearly or $49.99 lifetime, with a 7-day trial.

## Stack (decided)

Kotlin with Jetpack Compose, as a native Android app. The original plan used Flutter, but that was dropped on 2026-11-02 over the risk of slow cold starts on a launcher. Don't reintroduce Flutter or a cross-platform layer.

Planned pieces: a local database (Room), Google Play Billing, Firebase Crashlytics, and an analytics SDK. Without analytics, the plan's metrics can't be measured.

## Platform constraints that shape the design

- **It's a launcher.** The main activity must handle the `HOME` intent category. Cold start and recomposition cost on the home screen matter more than in a normal app, so keep the home screen's startup path light.
- **The pause screen only intercepts launches made from this launcher.** Catching launches from notifications, recents or other launchers would need Usage Access (`PACKAGE_USAGE_STATS`). That's a product decision and it has Play policy implications, so don't add it silently.
- **Timer alerts** need the `POST_NOTIFICATIONS` runtime permission (Android 13+). Use inexact alarms or WorkManager rather than `SCHEDULE_EXACT_ALARM`, which Play restricts.
- **Billing can only be tested on a build uploaded to a Play testing track** (internal testing is enough). Check out the test SKUs there; they don't work in a debug sideload.
- **Anything collected on the device** (check-ins, usage, analytics, crash data) must match the Play Console Data safety form and the privacy policy. Update those whenever data collection changes. The data deletion page (`docs/delete-data/`) is linked from the Data safety form, so keep it in step too.

## Layout

Single Gradle module `:app`, package `app.tendwell` (debug builds use `app.tendwell.debug` so they install next to a Play build). Versions live in `gradle/libs.versions.toml`.

- `MainActivity` is the HOME activity and holds the tiny screen state (Home, Search, Settings) itself, with no navigation library, to keep cold start light. Pressing Home while Tendwell shows returns to Home.
- `apps/AppRepository` lists launchable apps through `LauncherApps` (main and work profiles) and refreshes on install/remove. Package visibility comes from the `<queries>` block in the manifest, not `QUERY_ALL_PACKAGES`.
- `data/Prefs` is a Preferences DataStore. Essential apps are saved as ordered keys `package/class#userSerial`, at most 7.
- `data/Prefs` also holds today's state (`DayState`): bedtime, calendar toggle, the one thing, last check-in date, recent tasks and the Now task. `day/DayViewModel` owns it.
- `now/NowTask` is the pure timer model (pause, resume, +5 min, done, break) and is unit-tested. `now/TimerAlarm` schedules the end-of-timer nudge with `setAndAllowWhileIdle` (inexact, no exact-alarm permission). `POST_NOTIFICATIONS` is asked for when the first timer starts.
- `timebar/TimeBarModel` is the pure maths and labels for the time bar (unit-tested, handles bedtimes after midnight); `timebar/CalendarReader` reads today's events only when the user turns calendar on in Settings (`READ_CALENDAR`).
- `checkin/` is the daily "one thing" screen, shown the first time Home opens each day (only after onboarding is done).
- `onboarding/` holds the 5 first-run steps (Welcome, Essentials, PauseApps, SetHome, then the paywall). `MainActivity` shows them until `Prefs.onboardingDone`; picks are saved as each step is left. SetHome asks for `RoleManager.ROLE_HOME` on Android 10+ and opens the Home settings below that. `PauseSuggestions` is the pure list of suggested time-sink packages (unit-tested).
- `billing/BillingRepository` wraps Play Billing 8 (callback API). Products: subscription `tendwell_pro` with base plans `monthly` and `annual` (each with a 7-day free-trial offer; the yearly plan is `annual` because a first `yearly` plan was activated with the wrong billing period and is deactivated, and Play never reuses a base plan ID), and one-time `tendwell_pro_lifetime`. Pro is cached in `Prefs.isPro` and re-checked 1.5 s after Home resumes, at most every 6 hours. `PlanText` is the pure paywall copy (unit-tested); `PaywallScreen` shows Play's prices, never hard-coded ones. `AccessCode` lets Google Play's reviewers turn Pro on from the paywall's "Have an access code?" link, because they can't buy or start trials. Only the code's SHA-256 is in the repo; the code itself is kept with the signing files in the project's private files and goes in Play Console's Sign in details. It sets `access_code_pro` in `Prefs`, which billing never clears.
- `focus/FocusHours` is the pure focus-hours model (blocks with days, start/end that may cross midnight, and a 30 s, 1 min or 2 min pause), its saved form in `Prefs` (`focus_blocks`) and the status line; unit-tested. During an active block a Pro user's paused apps wait the block's pause instead of 10 s.
- `insights/WeekInsights` turns the open log into Monday-to-Sunday stats (opens, turned back, per day, per time-of-day slot, per app, change from the week before); unit-tested. `InsightsScreen` can go back four weeks, which is what the 35-day log holds.
- Pro lifts the 3-app pause limit. Without Pro only the first 3 saved paused apps apply (`effectivePaused`); the rest stay saved for when Pro comes back.
- `home/`, `search/`, `settings/` hold the other screens; `search/AppSearch` is pure Kotlin (ranking, accent-insensitive matching, letter groups) and is unit-tested.
- `ui/components/Layout.kt` holds the tablet rules. When the window is at least 600 dp wide and wider than it is tall (`isWide`: a tablet or unfolded foldable held sideways, or a phone turned sideways), Home, Pause, the focus timer and Insights switch to two columns that match the store's tablet screenshots (`/mnt/project-files/store-listing/upload/4-tablet-7-inch/`). If those screens change on tablets, the tablet screenshots must be re-made. Other screens stay one column, centred at 600 dp by `CenteredPage` in `MainActivity`.
- `ui/theme/` has the design tokens (`Tokens.kt`, copied from the design canvas's `tokens()`), the bundled variable fonts Fraunces and Plus Jakarta Sans (`res/font/`, OFL texts in `assets/licenses/`) and `TendwellType`. Use `Tendwell.colors` and `TendwellType`, not Material defaults. `ui/components/Icons.kt` has the spec's line icons.
- The launcher icon is a placeholder (the spec's sprout icon on sage) until the real icon exists.

## Commands

- `./gradlew assembleDebug`, `./gradlew installDebug`
- `./gradlew lintDebug`, `./gradlew testDebugUnitTest`, `./gradlew testDebugUnitTest --tests "app.tendwell.search.AppSearchTest"`
- `./gradlew testDebugUnitTest --tests "app.tendwell.screenshots.*"` runs only the screenshot tests. They draw every screen with `FakeData` (Robolectric with native graphics, Roborazzi without its Gradle plugin) on a phone and a sideways tablet, light and dark, into `app/build/outputs/roborazzi/<phone|tablet>-<light|dark>/`. They record, they don't compare, so they only fail on a crash.
- `./gradlew connectedDebugAndroidTest` runs `SmokeTest` (`app/src/androidTest/`) on a device or emulator: a fresh install walked through onboarding, the check-in, the Now timer and break, search, the long-press sheet, the pause, Settings, the paywall and (with `Prefs.setAccessCodePro`) the Pro screens, with animations on, then the sheets again with animations off. It finds things by their on-screen text, so keep it in step when copy changes (labels shown `.uppercase()` won't match).
- `./gradlew bundleRelease` builds the AAB for Play. It is signed when `TENDWELL_KEYSTORE_PATH`, `TENDWELL_KEYSTORE_PASSWORD`, `TENDWELL_KEY_ALIAS` and `TENDWELL_KEY_PASSWORD` are set, or when an untracked `keystore.properties` (storeFile, storePassword, keyAlias, keyPassword) exists; otherwise it is unsigned. `VERSION_CODE` sets the version code.

CI (`.github/workflows/android.yml`) runs lint, unit tests, the debug APK and the release AAB on every push to `main` and `claude/**`, and uploads both as run artifacts. It signs the AAB with the upload key from the repository secrets `TENDWELL_UPLOAD_KEYSTORE_BASE64`, `TENDWELL_KEYSTORE_PASSWORD`, `TENDWELL_KEY_ALIAS`, `TENDWELL_KEY_PASSWORD`, and uses the run number as the version code. A second job, `emulator`, runs `SmokeTest` on an API 34 emulator.

Both jobs force-push their pictures to branches named `shots/jvm-<branch>` and `shots/emulator-<branch>` (slashes become dashes), because Claude's cloud sessions cannot download run artifacts. Fetch one with `git fetch origin shots/emulator-<branch>` and `git archive FETCH_HEAD | tar -x -C <dir>`. The emulator branch also holds `logcat.txt` and the test result XML; on a timeout the smoke test saves a `FAILED-…` screenshot and prints the screen's semantics tree to logcat under the tag `SmokeTest`. After changing a screen, look at its shots before calling it done.

Claude's cloud sessions cannot download the Android SDK (dl.google.com is blocked there), so they rely on CI to compile and test.

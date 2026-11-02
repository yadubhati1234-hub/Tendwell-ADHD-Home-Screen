# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Status

Scaffolded on 2026-11-02. Done: core launcher (HOME intent, Home, search, long-press actions, Settings), time bar, Now card with timer and break, daily check-in. Still to come: pause screen, focus hours, insights, onboarding, billing, Firebase and Room.

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
- **Anything collected on the device** (check-ins, usage, analytics, crash data) must match the Play Console Data safety form and the privacy policy. Update those whenever data collection changes.

## Layout

Single Gradle module `:app`, package `app.tendwell` (debug builds use `app.tendwell.debug` so they install next to a Play build). Versions live in `gradle/libs.versions.toml`.

- `MainActivity` is the HOME activity and holds the tiny screen state (Home, Search, Settings) itself, with no navigation library, to keep cold start light. Pressing Home while Tendwell shows returns to Home.
- `apps/AppRepository` lists launchable apps through `LauncherApps` (main and work profiles) and refreshes on install/remove. Package visibility comes from the `<queries>` block in the manifest, not `QUERY_ALL_PACKAGES`.
- `data/Prefs` is a Preferences DataStore. Essential apps are saved as ordered keys `package/class#userSerial`, at most 7.
- `data/Prefs` also holds today's state (`DayState`): bedtime, calendar toggle, the one thing, last check-in date, recent tasks and the Now task. `day/DayViewModel` owns it.
- `now/NowTask` is the pure timer model (pause, resume, +5 min, done, break) and is unit-tested. `now/TimerAlarm` schedules the end-of-timer nudge with `setAndAllowWhileIdle` (inexact, no exact-alarm permission). `POST_NOTIFICATIONS` is asked for when the first timer starts.
- `timebar/TimeBarModel` is the pure maths and labels for the time bar (unit-tested, handles bedtimes after midnight); `timebar/CalendarReader` reads today's events only when the user turns calendar on in Settings (`READ_CALENDAR`).
- `checkin/` is the daily "one thing" screen, shown the first time Home opens each day.
- `home/`, `search/`, `settings/` hold the other screens; `search/AppSearch` is pure Kotlin (ranking, accent-insensitive matching, letter groups) and is unit-tested.
- `ui/theme/` has the design tokens (`Tokens.kt`, copied from the design canvas's `tokens()`), the bundled variable fonts Fraunces and Plus Jakarta Sans (`res/font/`, OFL texts in `assets/licenses/`) and `TendwellType`. Use `Tendwell.colors` and `TendwellType`, not Material defaults. `ui/components/Icons.kt` has the spec's line icons.
- The launcher icon is a placeholder (the spec's sprout icon on sage) until the real icon exists.

## Commands

- `./gradlew assembleDebug`, `./gradlew installDebug`
- `./gradlew lintDebug`, `./gradlew testDebugUnitTest`, `./gradlew testDebugUnitTest --tests "app.tendwell.search.AppSearchTest"`
- `./gradlew bundleRelease` builds the AAB for Play. It is signed when `TENDWELL_KEYSTORE_PATH`, `TENDWELL_KEYSTORE_PASSWORD`, `TENDWELL_KEY_ALIAS` and `TENDWELL_KEY_PASSWORD` are set, or when an untracked `keystore.properties` (storeFile, storePassword, keyAlias, keyPassword) exists; otherwise it is unsigned. `VERSION_CODE` sets the version code.

CI (`.github/workflows/android.yml`) runs lint, unit tests, the debug APK and the release AAB on every push to `main` and `claude/**`, and uploads both as run artifacts. It signs the AAB with the upload key from the repository secrets `TENDWELL_UPLOAD_KEYSTORE_BASE64`, `TENDWELL_KEYSTORE_PASSWORD`, `TENDWELL_KEY_ALIAS`, `TENDWELL_KEY_PASSWORD`, and uses the run number as the version code.

Claude's cloud sessions cannot download the Android SDK (dl.google.com is blocked there), so they rely on CI to compile and test.

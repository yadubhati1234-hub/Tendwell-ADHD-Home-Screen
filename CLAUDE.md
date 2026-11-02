# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Status

The repository has no code yet, only a README. When the Android project is scaffolded, update this file with the real module layout, commands and conventions, and remove anything below that turned out different.

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

## Commands

Not available yet. Once the Gradle wrapper exists, the standard ones apply: `./gradlew assembleDebug`, `./gradlew installDebug`, `./gradlew lint`, `./gradlew test` (unit tests), `./gradlew connectedAndroidTest` (instrumented tests, needs a device or emulator), and `./gradlew test --tests "<fully.qualified.ClassName>"` to run a single test class. Replace this paragraph with the real commands and module names after scaffolding.

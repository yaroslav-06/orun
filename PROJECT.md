# Orun — Project Description

## What the app is

Orun is a minimal Android run-tracking app. It records a GPS trail while you run and persists it locally in a SQLite database via Room.

## User flow

1. Open app → single screen with a toolbar ("Orun") and a FAB (play icon).
2. Tap FAB → app requests `ACCESS_FINE_LOCATION` (and `ACCESS_BACKGROUND_LOCATION` on Android 10+) if not already granted.
3. Once permission is held, a `Run` row is inserted into the database (recording `startTime`), and `LocationTrackingService` is started as a foreground service.
4. A persistent notification ("Orun is tracking your run") appears with a **Stop** action button. FAB changes to a pause icon.
5. The service collects GPS fixes every 3 seconds via Google's `FusedLocationProviderClient`. Each fix that is ≥2 m from the previous saved point is stored as a `LocationPoint` row linked to the current run.
6. Tap FAB again (or the notification's Stop button) → service stops, location updates are removed.

## Architecture

| Layer           | Class                     | Responsibility                                                                         |
|-----------------|---------------------------|----------------------------------------------------------------------------------------|
| UI              | `MainActivity`            | Permission handling, FAB start/stop, notification channel setup                        |
| Service         | `LocationTrackingService` | Foreground service; owns `FusedLocationProviderClient`; filters and persists GPS fixes |
| Database entity | `Run`                     | One row per run session (`id`, `startTime`, `endTime`)                                 |
| Database entity | `LocationPoint`           | One row per GPS fix (`runId` FK, `latitude`, `longitude`, `altitude`, `timestamp`)     |
| DAO             | `RunDao`                  | `insertRun`, `insertPoint`, `getPointsForRun`                                          |
| Database        | `RunDatabase`             | Room singleton; holds `runs` and `location_points` tables                              |

## Permissions

- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` — precise GPS
- `ACCESS_BACKGROUND_LOCATION` (Android 10+) — GPS while app is in background
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_LOCATION` — required to run a location foreground service

## Key dependencies (from `libs.versions.toml`)

- `com.android.application` AGP 9.1.0 (with built-in Kotlin support)
- `com.google.devtools.ksp` 2.1.20-1.0.31 — annotation processor for Room
- `androidx.room` 2.7.0 — local SQLite ORM
- `com.google.android.gms:play-services-location` 21.3.0 — FusedLocationProviderClient
- `org.jetbrains.kotlinx:kotlinx-coroutines-android` 1.8.1 — async DB writes

## What is NOT yet implemented

- Displaying the recorded route on a map
- Run history / list of past runs
- Distance / pace / duration statistics
- Exporting GPS data (GPX, etc.)
- `Run.endTime` is stored in the schema but never written

## Class Diagram

See [`class-diagram.puml`](class-diagram.puml) (open with the PlantUML Integration plugin in Android Studio).

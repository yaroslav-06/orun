# Orun — Running Tracker

An Android app for tracking outdoor runs with GPS, real-time metrics, voice announcements, and personal records.

---

## Demo



https://github.com/user-attachments/assets/f8be1b86-a387-4e4a-baf3-d0ead328bb6b



---

## Features

- **GPS tracking** via Google Fused Location Provider with elevation gain/loss detection
- **Real-time stats** — distance, duration, current pace, overall pace, elevation
- **Run goals** — set a target distance or duration before a run; see live progress with estimated finish time/distance
- **Voice announcements** — configurable TTS callouts at every ⅛, ¼, ½, or 1 unit () interval; choose which stats to announce
- **Route map** — visualise your route on an OpenStreetMap map (osmdroid)
- **Run history** — browse past runs with full details and GPS route replay
- **Best efforts** — automatically tracks personal bests at standard distances (1 km, 5 km, 10 km, half-marathon, marathon, etc.)
- **Achievements** — personal records and aggregate statistics
- **Metric / Imperial** — switch units at any time, even mid-run
- **Material You** — dynamic colour theming on Android 12+

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | AndroidX, Material Design 3, ConstraintLayout, RecyclerView |
| Database | Room (KSP) |
| Location | Google Play Services — FusedLocationProviderClient |
| Map | osmdroid 6.1.20 (OpenStreetMap) |
| Async | Kotlin Coroutines |
| Voice | Android TextToSpeech |

## Architecture

- **Foreground service** (`LocationTrackingService`) keeps GPS running while the screen is off and exposes a `MutableStateFlow<RunStats>` consumed by the UI
- **Pace calculation** uses a sliding 200 m ring buffer for smooth real-time pace
- **Elevation filtering** applies a 2 m hysteresis threshold to suppress GPS altitude noise
- **Preference changes** (units, voiceover) are applied live during a run via a `SharedPreferences` listener — no restart needed

## Getting Started

1. Clone the repo
2. Open in Android Studio (Hedgehog or later)
3. Connect a device or start an emulator with Google Play Services
4. Run the `app` configuration

**Required permissions:** `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`, `FOREGROUND_SERVICE`

## Project Structure

```
app/src/main/java/com/yarick/orunner/
├── service/
│   └── LocationTrackingService.kt   # GPS tracking, pace, TTS announcements
├── data/
│   ├── Run.kt                       # Room entity
│   ├── LocationPoint.kt             # GPS point entity
│   ├── BestEffort.kt                # Personal best entity
│   └── RunDatabase.kt               # Room database
├── HomeFragment.kt                  # Run history list
├── MapFragment.kt                   # OSM route map
├── AchievementsFragment.kt          # Personal records
├── RunStatsActivity.kt              # Live run screen
├── RunGoalSetupActivity.kt          # Pre-run goal setup
├── RunDetailActivity.kt             # Past run detail
└── SettingsActivity.kt              # Units & voiceover settings
```

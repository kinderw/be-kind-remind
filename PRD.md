# Be-Kind-Remind — Android App (MVP v0.1)

A lightweight Android app that tells you **when to leave** so you arrive on time, with traffic‑aware reminders.

Many people are late to appointments because they underestimate travel time or forget to leave early enough. This app solves the problem by providing traffic-aware leave-by reminders, 
helping users know exactly when to leave and sending progressive notifications as departure approaches.
It simplifies planning and reduces stress caused by unexpected traffic or missed departures.

---

## 1) Objectives & Scope

**Goal (MVP):**

* Let users create two types of tasks:

  1. **Arrive By**: user sets an arrival time and destination; app computes when to leave (traffic‑aware) and sends staggered reminders.
  2. **Leave At**: user sets a fixed leave time; app sends staggered reminders prior to that time (traffic‑agnostic).
* Provide reliable, on‑time notifications (60/30/15/5 min, plus **Leave Now**), with quick action to start navigation.

**Non‑goals (MVP):**

* Calendar import/sync, Wear OS, geofencing departure detection, multi‑stop routes, background location tracking while idle, account sign‑in, in‑app maps navigation.

**Primary users:**

* Commuters and anyone who wants a simple, dependable leave‑by reminder with traffic awareness.

**Success metrics (for internal QA):**

* > 95% of exact alarms fire within 1 minute of scheduled time on devices with exact‑alarm access.
* For Arrive‑By tasks created ≥90 minutes in advance, leave‑by recomputation occurs at least every 15 minutes inside the 90‑minute window (subject to Doze constraints) and reschedules notifications if the ETA shifts by ≥5 minutes.

---

## 2) Core Use Cases & UX

### 2.1 Create Task (Arrive By)

* Input: Destination (search/autocomplete), Arrival time (local TZ), Origin (defaults to current location snapshot), Reminder cadence (default [60,30,15,5]), Buffer minutes (default 10), Traffic model (best_guess default; allow pessimistic/optimistic in Settings), Optional title.
* Output: Computed **Leave By** time; schedules notifications; shows a task detail card with live ETA badge.

### 2.2 Create Task (Leave At)

* Input: Optional title/destination, Leave time (local TZ), Reminder cadence (default [60,30,15,5]).
* Output: Schedules notifications; (optional) shows current ETA if destination provided (does not affect schedule).

### 2.3 Task List & Detail

* List: Cards showing Title, Mode, Next reminder, Leave‑By (computed) or Leave‑At, and live ETA badge (if Arrive‑By).
* Detail: Summary, destination chip, origin chip, buffer, traffic model, reminder cadence, **Start Navigation** button.

### 2.4 Notifications

* Channel: "Departure reminders" (importance: High).
* Content examples:

  * "Leave in 30 min for *Airport* — ETA 42 min (heavier than usual, +9m). Leave by 2:18 PM."
  * "Leave now for *Dentist*."
* Actions: **Start navigation** (Google Maps intent), **Snooze 5m**, **Dismiss**.

---

## 3) Functional Requirements

### 3.1 Arrive‑By Scheduling

* On save:

  1. Fetch ETA via Distance Matrix (departure_time=now, `duration_in_traffic` when available) using origin snapshot and destination.
  2. Compute `leaveBy = arrivalTime - (ETA + bufferMinutes)`.
  3. Schedule exact alarms at `leaveBy - 60m`, `-30m`, `-15m`, `-5m`, and at `leaveBy`.
* ETA refresh cadence:

  * If `now < leaveBy - 90m`: no polling (battery‑friendly).
  * `≤ 90m`: poll every 15m via WorkManager.
  * `≤ 20m`: promote to a ForegroundService; poll every 2–3m until leaveBy (or user departs via Start Navigation).
* Rescheduling rule:

  * If latest ETA shifts leaveBy by **≥5 minutes** or crosses a reminder boundary, cancel and reschedule future alarms; post a low‑priority "Leave time updated" notification.
* Offline/Errors:

  * If ETA fetch fails, keep previous ETA; log and show stale flag; do not reschedule unless new ETA available.

### 3.2 Leave‑At Scheduling

* On save: schedule exact alarms at `leaveTime - 60m`, `-30m`, `-15m`, `-5m`, and at `leaveTime`.
* If destination is present, show ETA in notifications (no rescheduling).

### 3.3 Task CRUD & Persistence

* Create/Edit/Delete tasks in Room database.
* Restore schedules on device reboot and app update via `BOOT_COMPLETED` receiver.

### 3.4 Permissions & Settings

* **Location (fine):** required only to snapshot current origin at task creation; no background location in MVP.
* **Notifications:** required on Android 13+ (`POST_NOTIFICATIONS`).
* **Exact Alarms:** request special access on Android 13+; show in‑app explainer and settings deep link.
* Settings page: default buffer, traffic model, cadence preset, battery optimization help.

### 3.5 Navigation

* Use `Intent.ACTION_VIEW` with `"google.navigation:q=<lat>,<lng>"` (or `placeId`) to open Google Maps.

---

## 4) Non‑Functional Requirements

* **Timing reliability:** Use `AlarmManager.setExactAndAllowWhileIdle()` for reminders. Avoid relying on frequent polling far from leave time.
* **Battery:** No continuous polling outside 90‑minute window. ForegroundService only inside 20 minutes of leaveBy.
* **Time zones/DST:** Store instants in UTC; convert for display. Persist associated time zone with each task.
* **Crash/ANR targets:** <0.5% crash‑free sessions target for internal beta.
* **Privacy:** Store destinations and location snapshots locally. Do not send PII to any third‑party beyond route/ETA lookups.

---

## 5) External Services & Keys

* **Google APIs (GCP project):**

  * Enable: *Distance Matrix API*, *Places API*, (optional) *Maps SDK for Android*.
  * Restrict: Android app (package name + SHA‑1). Limit to specific APIs.
  * Billing: required (Distance Matrix is a paid API with free tier).
* **Key storage:** Define `MAPS_API_KEY` in `local.properties`; inject via `BuildConfig` at compile time. Never commit keys.

---

## 6) Data Model (Room)

```text
Table TripTask
- id: Long (PK)
- mode: Enum { ARRIVE_BY, LEAVE_AT }
- title: String?
- originLat: Double?
- originLng: Double?
- originLabel: String? ("Current location" snapshot label or saved place)
- destinationPlaceId: String?
- destinationLat: Double
- destinationLng: Double
- timeUtc: Long (epoch millis)          // arrivalTime for ARRIVE_BY; leaveTime for LEAVE_AT
- timeZoneId: String                    // e.g., "America/New_York"
- reminderOffsetsMin: String            // CSV: "60,30,15,5"
- bufferMin: Int                        // default 10 for ARRIVE_BY; 0 for LEAVE_AT
- trafficModel: Enum { BEST_GUESS, OPTIMISTIC, PESSIMISTIC }
- lastKnownEtaMin: Int?                 // ARRIVE_BY only
- status: Enum { SCHEDULED, COMPLETED, DISMISSED, CANCELED }
- createdAtUtc: Long
- updatedAtUtc: Long
```

Aux table `ScheduledAlarm`

```text
- taskId: Long (FK)
- fireAtUtc: Long
- type: Enum { MINUS_60, MINUS_30, MINUS_15, MINUS_5, LEAVE_NOW }
- requestCode: Int  // for AlarmManager PendingIntent IDs
```

---

## 7) App Architecture & Components

* **Language/Stack:** Kotlin, Jetpack (ViewModel, LiveData/Flow, Room, DataStore), Material 3.
* **Networking:** OkHttp + Retrofit + Moshi (or Kotlinx Serialization).
* **Background:** WorkManager (periodic ETA refresh), AlarmManager (exact reminders), ForegroundService (tight ETA window).
* **DI (optional for MVP):** Hilt; if omitted, keep simple singletons.
* **Navigation:** Jetpack Navigation component.

**Key modules/classes (MVP):**

* `DistanceMatrixClient` — Retrofit interface + request builder.
* `PlacesAutocomplete` — use Places SDK for Android UI widget or custom with REST; MVP can use SDK Autocomplete activity.
* `SchedulingEngine` — computes leaveBy, writes `ScheduledAlarm` rows, and schedules/cancels OS alarms.
* `EtaRefreshWorker` — WorkManager job to refresh ETA for tasks inside 90‑minute window.
* `DepartureTickerService` — ForegroundService polling every 2–3 minutes inside 20‑minute window.
* `NotificationHelper` — channels, builders, actions (Snooze, Start nav, Dismiss).
* `BootRestoreReceiver` — reschedules alarms on reboot/app update.
* `TaskRepository` — Room DAO wrapper.
* UI: `TaskListFragment`, `TaskEditorFragment`, `TaskDetailFragment`, `SettingsFragment`.

---

## 8) API Shapes

### 8.1 Distance Matrix (REST)

* Endpoint: `https://maps.googleapis.com/maps/api/distancematrix/json`
* Params:

  * `origins=lat,lng` or `place_id:<id>`
  * `destinations=lat,lng` or `place_id:<id>`
  * `departure_time=now` (or epoch seconds)
  * `traffic_model=best_guess|optimistic|pessimistic`
  * `mode=driving` (MVP)
  * `key=<API_KEY>`
* Response: use `rows[0].elements[0].duration_in_traffic.value` (seconds), fallback to `duration.value`.

### 8.2 Places (Android SDK)

* Autocomplete intent: returns `Place` with `id`, `latLng`, `name`.

---

## 9) Scheduling Algorithm (Pseudo)

```kotlin
fun scheduleArriveBy(task: TripTask) {
  val etaMin = fetchEtaMinutes(task.origin, task.destination, trafficModel)
  val leaveByUtc = task.arrivalUtc - (etaMin + task.bufferMin).minutes
  val reminders = parseOffsets(task.reminderOffsetsMin)
  for (offset in reminders + listOf(0)) { // 0 => leave now
    val fireAt = leaveByUtc - offset.minutes
    alarmManager.setExactAndAllowWhileIdle(..., fireAt, pendingIntent(task.id, offset))
    saveScheduledAlarm(task.id, fireAt, mapOffsetToType(offset))
  }
}

fun onEtaRefreshTick(taskId) {
  val task = repo.getTask(taskId)
  val newEta = fetchEtaMinutes(...)
  val oldLeaveBy = computeLeaveBy(task, task.lastKnownEtaMin)
  val newLeaveBy = computeLeaveBy(task, newEta)
  if (abs(newLeaveBy - oldLeaveBy) >= 5.minutes) {
    cancelFutureAlarms(taskId, now())
    rescheduleFrom(newLeaveBy, remainingOffsets(...))
    notifyUpdate(taskId, newLeaveBy)
  }
  repo.updateLastKnownEta(taskId, newEta)
}
```

---

## 10) Implementation Plan (Order of Work)

1. **Repo Scaffold**

   * Create Android app (Kotlin, minSdk 26+, targetSdk 34/35).
   * Add dependencies: Room, DataStore, WorkManager, Material, Retrofit/OkHttp, Places SDK.
   * Set up build variants, `local.properties` for `MAPS_API_KEY` → `BuildConfig.MAPS_API_KEY`.

2. **Manifest & Permissions**

   * Declare: `ACCESS_FINE_LOCATION`, `POST_NOTIFICATIONS` (13+), `RECEIVE_BOOT_COMPLETED`.
   * For 13+: request exact alarms special access via `<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM"/>`.
   * Add `BootRestoreReceiver`, `DepartureTickerService` (foreground), `EtaRefreshWorker` provider.

3. **Data Layer**

   * Define `TripTask` & `ScheduledAlarm` entities, DAOs, Room database.
   * Implement `TaskRepository`.

4. **Notification System**

   * Create channel(s), styles, action intents, snooze handling, deep links.

5. **Distance/Places Integration**

   * Initialize Places SDK.
   * Implement `DistanceMatrixClient` with Retrofit.

6. **Scheduling Engine**

   * Implement computation of leaveBy + creation of `PendingIntent`s + `AlarmManager` scheduling.
   * Implement cancel/reschedule logic; persist `ScheduledAlarm` rows.

7. **Background Workers**

   * `EtaRefreshWorker`: periodic polling policy (15m min interval inside 90m window).
   * `DepartureTickerService`: foreground, 2–3m polling inside 20m window; stop when leaving or time passes.

8. **UI**

   * Task List → Task Editor (Arrive‑By / Leave‑At tabs) → Task Detail.
   * Places Autocomplete integration.
   * Settings page.

9. **Boot Restore**

   * Implement receiver to restore alarms from DB on reboot or app update.

10. **Testing**

* Unit tests for time math and rescheduling decisions.
* Instrumentation: create tasks, toggle Doze (`adb shell cmd deviceidle force-idle`), simulate reboot, verify alarms.

11. **Polish & Beta**

* Empty states, error toasts/snackbars, privacy copy, Settings help for exact alarms & battery.
* Prepare internal beta build.

---

## 11) Acceptance Criteria (MVP)

* Create Arrive‑By task → immediately shows computed Leave‑By; schedules five notifications (60/30/15/5/now).
* If traffic increases by ≥5min after creation (simulated), future alarms reschedule accordingly and an update notification is posted.
* Leave‑At task schedules five notifications independent of traffic; optional ETA display in notification if destination present.
* Notifications include actions: Start navigation opens Google Maps to destination; Snooze adds a 5‑minute one‑shot alarm; Dismiss cancels future notifications for that task instance.
* After reboot, all alarms are restored from the Room database.
* No background location permission is required.

---

## 12) Edge Cases

* Crossing midnight or DST boundary (store UTC, display local).
* No network at creation: allow task creation, compute Leave‑By once network returns (first ETA refresh window), warn user.
* Origin unset and location denied: require user to pick a saved origin place; block Arrive‑By otherwise.
* Distance Matrix returns `ZERO_RESULTS` or `NOT_FOUND`: prompt to adjust origin/destination.
* Leave‑By already in the past when computed: show warning and schedule only immediate "Leave now".

---

## 13) Privacy & Security

* Location snapshot used only for computing origin at task creation; no continuous tracking.
* Store all data locally; allow user to delete tasks and history.
* API keys restricted by package + SHA‑1; never bundled in source control.

---

## 14) Telemetry (Optional in MVP)

* Local analytics only (e.g., count of created tasks, fired reminders) for debugging; no remote analytics in MVP unless opted‑in.

---

## 15) Risks & Mitigations

* **Exact alarms denied on Android 13+** → graceful degradation: schedule inexact alarms + user education deep link.
* **Doze delaying WorkManager** → rely on exact alarms for the actual reminders; polling is advisory, not critical.
* **API quota/costs** → throttle polling; only poll inside 90m, cache ETAs per (origin,destination,time bucket).

---

## 16) Deliverables

* Android app module `app/` with all components above.
* `README.md` setup instructions (GCP key, SHA‑1, enabling APIs).
* Unit tests for time math; basic instrumentation tests.

---

## 17) Tasks for Code Generation (Codex Checklist)

1. **Project Setup**

   * Create Kotlin Android project (minSdk 26, targetSdk 34/35) with Material3.
   * Add Gradle deps: `androidx.core:core-ktx`, `appcompat`, `material`, `lifecycle-runtime-ktx`, `navigation`, `room-ktx`, `work-runtime-ktx`, `play-services-places`, `okhttp`, `retrofit`, `converter-moshi`, `kotlinx-datetime` (or ThreeTenABP), `gms:play-services-location` (for current location snapshot).
   * Read `MAPS_API_KEY` from `local.properties` → `BuildConfig.MAPS_API_KEY`.

2. **Manifest & Channels**

   * Add permissions and receivers/services.
   * Create notification channel "departure_reminders" on first app start.

3. **DB Layer**

   * Define entities, DAOs, Room database, Migration(1) initial.

4. **Networking**

   * Retrofit service `DistanceMatrixService#getMatrix(...)` + models.
   * Places SDK init in `Application`.

5. **Scheduling Engine**

   * Utility for exact alarms with stable `PendingIntent` request codes.
   * Write/clear `ScheduledAlarm` rows.

6. **Workers/Service**

   * `EtaRefreshWorker` with constraints (network required), 15m interval.
   * `DepartureTickerService` (foreground) with ongoing notification and 2–3m ticks.

7. **UI**

   * Compose or Views (choose one; MVP can use Views): Task List, Task Editor (two tabs), Task Detail, Settings.
   * Integrate Places Autocomplete intent for destination.

8. **Actions**

   * Notification actions for Start Nav, Snooze (create one‑shot +5m alarm), Dismiss.

9. **Boot Restore**

   * Implement receiver to reload tasks and reschedule alarms.

10. **Tests & QA Scripts**

* Unit tests for (leaveBy math, reschedule threshold logic, DST cases).
* Manual QA checklist (below).

---

## 18) Manual QA Checklist

* [ ] Arrive‑By: compute leaveBy and schedule 5 reminders.
* [ ] Traffic jump +7m triggers reschedule and update notification.
* [ ] Leave‑At: reminders fire at 60/30/15/5/now.
* [ ] Snooze adds +5m one‑shot reminder.
* [ ] Start Navigation opens Google Maps to destination.
* [ ] Reboot device: alarms restored.
* [ ] Deny exact alarms: app warns and still posts inexact reminders.
* [ ] Deny location: Arrive‑By requires manual origin selection.

---

## 19) Future Backlog (Post‑MVP)

* Calendar event import (read‑only) with location parsing.
* Geofencing to auto‑dismiss once user departs.
* Multi‑mode (walking, transit), car‑park buffer, school‑zone avoidance.
* Wear OS companion.
* i18n and localization; accessibility audit.

---

## 20) Appendix — Example Gradle & Manifest Snippets

**Gradle (module build.gradle.kts) — dependencies (names illustrative):**

```kotlin
dependencies {
  implementation("androidx.core:core-ktx:1.13.1")
  implementation("com.google.android.material:material:1.12.0")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
  implementation("androidx.work:work-runtime-ktx:2.9.0")
  implementation("androidx.room:room-ktx:2.6.1"); kapt("androidx.room:room-compiler:2.6.1")
  implementation("com.squareup.okhttp3:okhttp:4.12.0")
  implementation("com.squareup.retrofit2:retrofit:2.11.0")
  implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
  implementation("com.google.android.libraries.places:places:3.5.0")
}
```

**Manifest — key bits:**

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />

<application ...>
  <service
    android:name=".bg.DepartureTickerService"
    android:exported="false"
    android:foregroundServiceType="location" />

  <receiver
    android:name=".bg.BootRestoreReceiver"
    android:exported="true">
    <intent-filter>
      <action android:name="android.intent.action.BOOT_COMPLETED" />
      <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
    </intent-filter>
  </receiver>
</application>
```

## 21) Additional Constraints & Operational Policies

* **API Quotas & Backoff**: Implement token-bucket throttling per app instance; exponential backoff (base 2, jitter 0–500ms) on HTTP 429/5xx; cap to 3 retries.
* **Caching**: Cache last ETA per `(origin,destination)` with a 10‑minute TTL outside the 90‑minute window; inside the 90‑minute window rely on scheduled polls only.
* **Doze & OEM Killers**: Provide in‑app education and deep link to battery optimization settings; no background location; rely on `setExactAndAllowWhileIdle` for reminders.
* **Time Semantics**: Store all instants in UTC; keep `timeZoneId` with each task; explicitly handle DST gaps/overlaps (warn if `leaveBy` falls into a skipped or duplicated wall‑time).
* **Accessibility**: Minimum 4.5:1 text contrast; TalkBack labels for buttons; focus order defined; large‑text tested up to 1.3x.
* **Security**: No logging of precise lat/lng in release builds; redact PII in crash logs; keystore‑backed signing configs.

## 22) Interfaces — Detailed Contracts

### 22.1 Notifications & Intents

* **Channel**: `departure_reminders` (IMPORTANCE_HIGH).
* **Actions**:

  * `ACTION_START_NAV`: extras `{taskId: Long}` → launches `Intent.ACTION_VIEW` with `google.navigation:q=<lat>,<lng>`.
  * `ACTION_SNOOZE_5M`: extras `{taskId: Long}` → schedules one‑shot alarm `now+5m`.
  * `ACTION_DISMISS`: extras `{taskId: Long}` → cancels all future alarms for the task instance.
* **Deep links**: Tapping content opens `TaskDetailFragment` with `taskId`.

### 22.2 Broadcast Receivers

* `BootRestoreReceiver` listens to `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`; calls `SchedulingEngine.restoreAll()`.

### 22.3 Retrofit DTOs (minimal)

```kotlin
data class DistanceMatrixResponse(
  val rows: List<Row>
) { data class Row(val elements: List<Element>)
  data class Element(
    val status: String,
    val duration: DMValue?,
    val duration_in_traffic: DMValue?
  )
  data class DMValue(val value: Long) // seconds
}
```

## 23) CI / Test Plan (Expanded)

* **CI Jobs** (GitHub Actions example):

  * `assembleDebug` & `lintVitalRelease` on PR.
  * Run unit tests: `./gradlew test`.
  * Run instrumented tests on Firebase Test Lab (Pixel 6 / Android 14, Pixel 4a / Android 12). Artifacts: screenshots, logcat, coverage.
* **Static Analysis**: Ktlint + Detekt; dependency check (OWASP) on release.
* **Fixtures**: JSON samples for `duration_in_traffic`, `ZERO_RESULTS`, `OVER_QUERY_LIMIT`.
* **Deterministic Time Tests**: Inject `Clock` to simulate DST change (spring forward / fall back) and midnight crossing.

## 24) Definition of Done (Expanded)

* Code + tests merged with green CI, ≥80% line coverage for `SchedulingEngine` and `NotificationHelper`.
* User‑facing copy reviewed; accessibility checks pass (TalkBack + large text).
* `README.md` updated with setup (GCP APIs, SHA‑1, key restriction steps, billing enablement).
* Database migration notes included when schema changes.
* Privacy statement added to `README.md`.
* Release notes created for v0.1.

## 25) Secrets, Billing & Key Management

* `MAPS_API_KEY` only from `local.properties`/CI secrets; **never** in source.
* GCP key restricted to package & SHA‑1; only Distance Matrix + Places enabled.
* Billing project linked; monthly quota alert at 80% usage.

## 26) Permissions UX Flows

* **Exact Alarms**: On Android 13+, show rationale screen → settings deep link; continue with inexact alarms if denied and mark task with a warning badge.
* **Notifications**: Request at first task creation; gracefully proceed without scheduling if denied and surface a persistent in‑app banner.
* **Location**: Request only on Arrive‑By creation when origin = current location; fallback to manual origin picker.

## 27) Release Checklist (v0.1)

* [ ] App name & icon set to “Be‑Kind‑Remind”.
* [ ] VersionCode/VersionName set (e.g., 1/0.1.0).
* [ ] ProGuard/R8 rules verified; Retrofit/OkHttp models kept as needed.
* [ ] Crash reporting disabled or privacy‑safe configuration.
* [ ] Manual QA checklist fully executed and signed off.
* [ ] Upload internal testing build (Play Console or side‑load) and sanity pass on a second device.

## 28) Defaults & Constants (Single Source of Truth)

| Key                | Default                           | Notes                                                      |
| ------------------ | --------------------------------- | ---------------------------------------------------------- |
| Reminder cadence   | `[60,30,15,5]` minutes            | Applies to both modes; append `0` internally for Leave Now |
| Buffer minutes     | `10` (Arrive‑By), `0` (Leave‑At)  | User‑configurable in Settings                              |
| Traffic model      | `best_guess`                      | Optional `optimistic` / `pessimistic` in Settings          |
| ETA poll cadence   | `15m` when within 90m of leaveBy  | WorkManager periodic                                       |
| Tight poll cadence | `2–3m` when within 20m of leaveBy | ForegroundService                                          |
| Snooze             | `5m`                              | One‑shot exact alarm                                       |
| Time storage       | `UTC`                             | `timeZoneId` persisted per task                            |
| Min SDK            | 26                                | Target 34/35                                               |
| Navigation intent  | `google.navigation:q=<lat>,<lng>` | Use placeId if present                                     |

Expose these through a single Kotlin object:

```kotlin
object Defaults {
  val REMINDER_OFFSETS = listOf(60,30,15,5)
  const val ARRIVE_BY_BUFFER_MIN = 10
  const val LEAVE_AT_BUFFER_MIN = 0
  const val ETA_POLL_MINUTES = 15
  const val TIGHT_POLL_MINUTES = 3
  const val SNOOZE_MINUTES = 5
}
```

## 29) Feature Flags (BuildConfig)

Set via Gradle `buildConfigField` for `debug`/`release`:

* `USE_COMPOSE: Boolean = false`
* `ENABLE_TELEMETRY: Boolean = false`
* `ETA_DEBUG_TOASTS: Boolean = false`

Gradle snippet:

```kotlin
buildTypes {
  debug { buildConfigField("boolean", "USE_COMPOSE", "false")
          buildConfigField("boolean", "ENABLE_TELEMETRY", "false")
          buildConfigField("boolean", "ETA_DEBUG_TOASTS", "true") }
  release { buildConfigField("boolean", "USE_COMPOSE", "false")
            buildConfigField("boolean", "ENABLE_TELEMETRY", "false")
            buildConfigField("boolean", "ETA_DEBUG_TOASTS", "false") }
}
```

## 30) UI Wireframes & View IDs (ASCII)

```
[ Task List ]                             [ Settings ]
┌───────────────────────────────┐        ┌──────────────────────────┐
│  + New Task  (id: fab_add)    │        │ Default buffer  [10]     │(id: input_default_buffer)
├───────────────────────────────┤        │ Traffic model [best...]  │(id: spinner_traffic)
│ ▸ Airport @ 3:30p (ARRIVE_BY) │(id: card_task_item)
│   Leave by 2:18p  ETA 42m     │        │ Reminder cadence         │(id: input_cadence)
│   Next: 30m reminder          │        │ Exact alarm help [Open]  │(id: btn_alarm_help)
├───────────────────────────────┤        └──────────────────────────┘
│ ▸ Dentist @ 4:00p (LEAVE_AT)  │
└───────────────────────────────┘

[ Task Editor ] (tabs: ARRIVE_BY / LEAVE_AT)
┌──────────────────────────────────────────────┐
│ Title                (id: input_title)       │
│ Destination         (id: input_destination)  │ (Places autocomplete)
│ Origin (optional)   (id: input_origin)       │
│ Time (local)        (id: input_time)         │ (Time picker)
│ Buffer (min)        (id: input_buffer)       │
│ Reminders           (id: input_offsets)      │ CSV
│ Save                (id: btn_save_task)      │
└──────────────────────────────────────────────┘
```

## 31) Localization & Time Formatting

* Respect system 12/24h time format via `DateFormat.is24HourFormat(context)`.
* All user‑visible copy in `strings.xml`; avoid hard‑coded units (use `%d min`).
* English only for MVP; extract string resources for future i18n.

## 32) Error & Status Copy (strings)

```xml
<string name="err_eta_refresh_failed">Couldn\'t refresh traffic. Using last ETA.</string>
<string name="note_leave_time_updated">Leave time updated: %1$s</string>
<string name="warn_exact_alarm_denied">Exact alarms are off. Reminders may be delayed.</string>
<string name="warn_location_denied">Location is off. Pick an origin to compute ETA.</string>
<string name="notif_leave_in">Leave in %1$d min for %2$s</string>
<string name="notif_leave_now">Leave now for %1$s</string>
<string name="action_start_nav">Start navigation</string>
<string name="action_snooze">Snooze 5 min</string>
<string name="action_dismiss">Dismiss</string>
```

## 33) CI Workflow (GitHub Actions)

Create `.github/workflows/android.yml`:

```yaml
name: Android CI
on:
  pull_request:
    branches: [ main ]
  push:
    branches: [ main ]
jobs:
  build-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
      - name: Gradle cache
        uses: gradle/gradle-build-action@v3
      - name: Inject MAPS_API_KEY (empty for CI)
        run: |
          echo "MAPS_API_KEY=dummy-ci-key" >> local.properties
      - name: Lint & assemble
        run: ./gradlew lintVitalRelease assembleDebug --stacktrace
      - name: Unit tests
        run: ./gradlew test --stacktrace
  static-analysis:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: gradle/gradle-build-action@v3
      - name: Detekt
        run: ./gradlew detekt
      - name: Ktlint
        run: ./gradlew ktlintCheck
```

## 34) Test Fixtures (Place under `app/src/test/resources/fixtures/`)

* `dm_success_with_traffic.json` — includes `duration_in_traffic`.
* `dm_zero_results.json` — `status: ZERO_RESULTS`.
* `dm_over_query_limit.json` — simulate quota error.

Example (trimmed) `dm_success_with_traffic.json`:

```json
{
  "rows": [
    { "elements": [ { "status": "OK",
        "duration": { "value": 2400 },
        "duration_in_traffic": { "value": 2520 }
    } ] }
  ]
}
```

## 35) Suggested Repo File Tree (generated by Codex)

```
app/
  src/main/java/com/bekindremind/
    data/db/
      TripTask.kt
      ScheduledAlarm.kt
      AppDatabase.kt
      TaskDao.kt
    data/net/
      DistanceMatrixService.kt
      DistanceMatrixClient.kt
    domain/
      SchedulingEngine.kt
    bg/
      EtaRefreshWorker.kt
      DepartureTickerService.kt
      BootRestoreReceiver.kt
    ui/
      list/TaskListFragment.kt
      edit/TaskEditorFragment.kt
      detail/TaskDetailFragment.kt
      settings/SettingsFragment.kt
    util/NotificationHelper.kt
    App.kt
  src/main/res/values/strings.xml
  src/test/resources/fixtures/*.json
.github/workflows/android.yml
README.md
PRD.md
```

---

**This PRD is intended to be placed at the repo root as `PRD.md`.** It enumerates the MVP scope, concrete components, and an ordered plan so your code generation workflow can scaffold and implement the app incrementally.

# Implementation Tracker

Last updated: 2026-04-20 (UTC)

## Decisions (Locked)
- UI framework: Compose
- ETA strategy: real API integration (no mock mode)
- Exact alarms permission denial: block scheduling flow
- Reminder cadence: editable in v1
- Snooze: included in v1

## Plan Status
1. ✅ Finalize MVP implementation decisions
2. ✅ Foundation slice
3. 🔄 Core scheduling logic and rescheduling rules
4. 🔄 Minimal UI wiring
5. 🔄 Google API integration
6. ⏳ Settings + action polish + reliability hardening
7. ⏳ Validation against PRD criteria

## Completed in this pass
- Added Android/Gradle project scaffold targeting SDK 35.
- Added Compose entry activity and exact-alarm permission blocker UX.
- Added Room entities, DAOs, database, repository.
- Added scheduling engine skeleton with exact alarm scheduling/cancel path and permission enforcement.
- Added notification helper, alarm receiver, and boot restore receiver with actual restore logic.
- Added real Distance Matrix API client + `GoogleEtaProvider` integration.
- Added periodic `EtaRefreshWorker` registration and worker implementation.
- Added `DepartureTickerService` foreground service skeleton.
- Added demo task scheduling buttons in UI to validate end-to-end flows quickly.

## Next up
- Replace demo buttons with task editor/list/detail screens.
- Implement full notification actions (snooze/start navigation/dismiss).
- Add automated unit tests for scheduling calculations and ETA shift rescheduling.
- Address any branch merge conflicts by rebasing onto target and resolving line-by-line.

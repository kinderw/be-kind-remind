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
2. 🔄 Foundation slice
3. ⏳ Core scheduling logic and rescheduling rules
4. ⏳ Minimal UI wiring
5. ⏳ Google API integration
6. ⏳ Settings + action polish + reliability hardening
7. ⏳ Validation against PRD criteria

## Completed in this pass
- Added Android/Gradle project scaffold targeting SDK 35.
- Added Compose entry activity.
- Added Room entities, DAOs, database, repository.
- Added scheduling engine skeleton with exact alarm scheduling/cancel path.
- Added notification helper with high-importance channel.
- Added alarm and boot receivers and manifest wiring.

## Next up
- Implement ETA refresh worker + foreground ticker service.
- Implement exact-alarm permission gate and blocker UX.
- Integrate real Google Distance Matrix + Places clients.
- Wire task creation/editing UI to repository and scheduling engine.

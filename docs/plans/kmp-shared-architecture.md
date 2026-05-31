# CircleKeep KMP Shared Architecture Plan

## Goal
CircleKeep remains Android-first for now, but feature work should prepare the
project for a Kotlin Multiplatform shared core that can later power Android and
Apple clients with the same privacy model.

## Principles
- Local-first stays non-negotiable: no account, backend, analytics, ads, or
  network dependency by default.
- Shared code should own relationship behavior, not platform APIs.
- Platform code should adapt device capabilities into small shared contracts.
- Data contracts should use stable, portable types: strings, numbers, booleans,
  lists, maps, and epoch-millis timestamps.
- Cross-platform concepts should use explicit keys rather than localized labels.

## Shared Candidates
- Contact type definitions and validation.
- Reminder cadence rules.
- Due, overdue, upcoming, and snoozed grouping.
- Person memory fields and interaction log rules.
- Backup/export DTOs and compatibility defaults.
- Import conflict rules.
- Plain Kotlin tests for domain behavior.

## Platform Adapters
- Android Room database and migrations.
- Android contacts and call-log readers.
- Android notifications and notification actions.
- Android file picker/export surfaces.
- Jetpack Compose UI.
- Future Apple persistence, Contacts framework access, local notifications, and
  SwiftUI or Compose Multiplatform UI.

## Migration Path
1. Keep new domain logic free of Android imports inside the current app module.
2. Introduce portable DTO/domain types beside existing Room entities when a
   feature needs more shared logic.
3. Move reminder calculation, contact type rules, and backup compatibility into
   a `shared` module once the boundary is stable.
4. Keep Android Room entities as persistence mappers rather than the only source
   of business behavior.
5. Add an Apple client only after the shared module can run domain tests without
   Android dependencies.

## Current Notes
- `ContactReminderCalculator` is already close to shared logic because it does
  not call Android APIs.
- `NetworkBackupCodec` is Android/JVM-specific today because it uses
  `org.json`; future serialization work should move toward KMP-ready DTOs.
- Room annotations on entities are acceptable for the current Android app, but
  new behavior should avoid depending on those annotations directly.

# Repository Guidelines For Agents

CircleKeep is an Android app today, but it must be developed as a future
Kotlin Multiplatform product. The long-term target is a shared Kotlin core
that can support Android and Apple clients without changing the privacy model.

## KMP-First Rule

When adding or changing behavior, keep business logic shared-compatible by
default.

- Keep relationship rhythm, reminder calculation, contact type behavior,
  backup models, import rules, and validation logic free of Android framework
  dependencies whenever practical.
- Isolate Android-only APIs such as Room, `Context`, `ContentResolver`,
  contacts, call logs, notifications, WorkManager/AlarmManager, and Compose UI
  behind platform-specific app boundaries.
- Prefer portable Kotlin types for shared candidates: `String`, `Int`, `Long`,
  `Boolean`, lists, maps, and explicit epoch-millis timestamps.
- Use stable string keys for cross-platform concepts such as contact types.
- Do not introduce JVM/Android-only libraries into code that could reasonably
  move to a future `shared` module.
- If new serialization is added, prefer a multiplatform-ready path such as
  kotlinx.serialization over Android-only JSON APIs.
- Write domain tests as plain Kotlin/JVM tests where possible so they can later
  move into common tests.
- If platform-specific behavior is unavoidable, document the boundary and keep
  the shared-facing contract small.

## Current Architecture Boundary

The current app module may keep Android implementations for persistence,
permissions, notifications, and device integrations. New feature design should
still separate:

- shared candidates: models, cadence rules, due/upcoming grouping, backup DTOs,
  and privacy-preserving import/export rules;
- Android adapters: Room schema, Android contacts/call-log access,
  notification actions, file pickers, and Compose screens.

## Privacy Rule

CircleKeep is local-first. Do not add account, backend, analytics, cloud sync,
remote config, ads, or network access without an explicit design discussion and
README update.

## Documentation Rule

Any feature that changes permissions, storage, backup/import, notifications, or
platform assumptions must update README/CONTRIBUTING docs and relevant plan
files in the same change.

# Repository Guidelines For Agents

CircleKeep is an Android app today, but new development must treat it as a
future Kotlin Multiplatform product. The Android app is the first client, not
the architectural center. The long-term target is a shared Kotlin core that can
support Android and Apple clients without changing the privacy model.

## Shared-First Rule

Every new feature or behavior change must be designed as if its business logic
belongs in a shared Kotlin core first.

- Keep relationship rhythm, reminder calculation, contact type behavior,
  backup models, import/export rules, validation, grouping, sorting, and
  privacy-policy decisions free of Android framework dependencies.
- Isolate Android-only APIs such as Room, `Context`, `ContentResolver`,
  contacts, call logs, notifications, WorkManager/AlarmManager, file pickers,
  and Compose UI behind platform-specific app boundaries.
- Until a real `shared` Gradle module exists, place portable logic in
  Android-free Kotlin packages/files inside the app module so it can move later
  with minimal rewrite.
- Prefer portable Kotlin types for shared candidates: `String`, `Int`, `Long`,
  `Boolean`, lists, maps, and explicit epoch-millis timestamps.
- Use stable string keys for cross-platform concepts such as contact types.
- Do not introduce JVM/Android-only libraries into code that could reasonably
  move to a future `shared` module.
- If new serialization is added, prefer a multiplatform-ready path such as
  kotlinx.serialization over Android-only JSON APIs.
- Write domain tests as plain Kotlin/JVM tests where possible so they can later
  move into common tests.
- A feature that changes shared-candidate behavior is not complete until the
  portable behavior is covered by focused tests.

## Refactor-Before-Feature Rule

The existing codebase was written Android-first. That is legacy context, not a
pattern to continue.

- Before adding behavior to an Android-specific file, identify whether the new
  behavior or nearby modified behavior is a shared candidate.
- If a feature would extend mixed Android/domain logic, first extract the
  shared-facing logic into Android-free Kotlin, then build the feature on that
  boundary.
- Leave touched files more shared-compatible than they were found. Prefer small
  migration steps during normal feature work over large delayed rewrites.
- ViewModels, Compose screens, Room entities/DAOs, device readers,
  notification receivers, and Android services should coordinate, persist,
  render, or adapt platform capabilities. They should not own business rules.
- If platform-specific behavior is unavoidable, document the boundary and keep
  the shared-facing contract small.

## Current Architecture Boundary

The current app module may keep Android implementations for persistence,
permissions, notifications, and device integrations. New feature design must
still separate:

- shared candidates: models, cadence rules, due/upcoming grouping, backup DTOs,
  import/export compatibility, validation, and privacy-preserving rules;
- Android adapters: Room schema, Android contacts/call-log access,
  notification actions, file pickers, permissions, and Compose screens.

## Feature Slice Rule

New features should be built as clear vertical slices instead of spreading
business logic across UI and platform files.

- Separate each feature into shared-candidate logic, Android adapters,
  ViewModel/state handling, Compose UI, and tests.
- Start with the portable behavior and data contract before wiring Android UI
  or persistence details.
- Do not hide business rules in click handlers, composables, Room queries, or
  one-off ViewModel transformations.
- Keep UI text, layout state, and navigation separate from domain decisions.
- When a feature touches backup/import, reminders, contact types, or
  relationship state, define the shared-facing behavior before adding screens.

## State Management Rule

ViewModels should coordinate state; they should not become the domain layer.

- Prefer immutable UI state data classes and explicit event/update functions.
- Keep filtering, grouping, validation, date math, and reminder decisions in
  portable domain code whenever practical.
- Compose screens should render state and send events. They should not own
  business rules or perform repository work directly.
- Avoid storing the same derived state in multiple places. Prefer one source of
  truth and derive display data deliberately.
- Long-running work must use lifecycle-aware Android boundaries while keeping
  the underlying business operation platform-neutral.

## Time And Date Rule

Reminder behavior depends on time, so time must stay testable and portable.

- Prefer explicit epoch-millis timestamps for shared-facing models and rules.
- Do not scatter direct `System.currentTimeMillis()` calls through business
  logic. Pass "now" into domain functions or isolate it behind a small boundary.
- Keep reminder windows, snooze rules, overdue/upcoming grouping, and date
  presets in deterministic functions with focused tests.
- Avoid locale-specific or device-timezone assumptions in shared candidates
  unless the behavior is explicitly documented.

## Data Safety Rule

CircleKeep stores personal relationship data, so data loss and silent behavior
changes are high-severity bugs.

- Room migrations must be explicit and non-destructive unless a destructive
  change has been deliberately designed and documented.
- Backup/import changes must define backward compatibility, defaults for new
  fields, and conflict behavior.
- Delete, merge, import, restore, and migration paths need focused regression
  tests when changed.
- New persisted fields should have clear privacy impact, backup behavior, and
  migration behavior.
- Do not silently discard user-entered notes, memory fields, tags, contact
  types, reminder state, or interaction history.

## File Size And Readability Rule

Code files must stay small enough to scan and safely change. Do not keep adding
"one more thing" to files that are already hard to understand.

- Treat 250 lines as a soft ceiling for domain/data files, 300 lines for
  repositories and ViewModels, and 350 lines for Compose screen files.
- New Kotlin files should stay under the relevant soft ceiling unless there is
  a clear reason, such as an explicit Room migration sequence or focused test
  fixture.
- Do not add new behavior to a file already over its soft ceiling unless the
  change also extracts a cohesive type, mapper, helper, state holder, or
  composable into a focused file, or the change documents why extraction would
  be riskier.
- Split by responsibility: domain services/rules, DTOs, mappers, repository
  adapters, ViewModel state/events, screen routes, and reusable Compose
  components should not collapse into one file.
- Avoid generic dumping grounds such as `Utils`, `Helpers`, or large
  catch-all files. Names should describe the specific domain capability.
- Prefer small named functions and types over long private blocks inside a
  large file. A small change should not require rereading an unrelated feature.

## Code Consistency Rule

Consistency is part of maintainability. Prefer one clear local pattern over
inventing a second style for the same job.

- Search for existing models, mappers, naming conventions, and state patterns
  before adding new ones.
- Avoid parallel representations of the same concept unless there is a clear
  boundary, such as domain DTO versus Room entity.
- Names should describe the domain behavior, not the implementation shortcut.
- Keep package boundaries meaningful: domain rules, data adapters, device
  integrations, notifications, preferences, and UI should remain distinct.
- Do not introduce broad "common" files that mix unrelated responsibilities.

## Dependency Rule

Every new dependency must fit the local-first and shared-first direction.

- Do not add network, analytics, ads, remote config, account, backend, or cloud
  SDK dependencies without an explicit design discussion and README update.
- Do not add Android/JVM-only dependencies for logic that could reasonably move
  to shared code.
- Prefer standard Kotlin or multiplatform-ready libraries for shared-candidate
  logic.
- Add dependencies through the Gradle version catalog when practical, and keep
  the reason for the dependency clear in the change.
- Do not add a library for simple behavior that the standard Kotlin or Android
  toolchain already handles cleanly.

## Testing Rule

Tests should protect the portable behavior and the local-first data model.

- New shared-candidate behavior is incomplete without focused unit tests.
- Bug fixes should include regression tests unless the behavior is impractical
  to test at that layer.
- Prioritize tests for reminder rules, contact type behavior, backup/import,
  migrations, validation, privacy-preserving import/export, and repository
  behavior.
- Prefer plain Kotlin/JVM tests for domain rules so they can move to future
  common tests.
- UI or Android integration tests should cover platform wiring, not compensate
  for missing domain tests.

## Performance Rule

Optimize for understandable code first, then prevent obvious repeated work on
hot paths.

- Do not perform heavy filtering, sorting, parsing, backup/import work, or
  database access directly in Compose render paths.
- Prepare derived display data in domain/ViewModel boundaries where it can be
  tested and controlled.
- Avoid repeated full-list scans when a feature can reuse existing grouped or
  indexed state cleanly.
- Use coroutines and dispatchers deliberately for I/O or CPU work, keeping the
  business operation itself platform-neutral where practical.
- Do not add caching until there is a clear reason and an invalidation plan.

## Privacy Rule

CircleKeep is local-first. Do not add account, backend, analytics, cloud sync,
remote config, ads, or network access without an explicit design discussion and
README update.

## Agent Workflow Rule

Agents should make changes in a way that keeps the codebase easier to continue.

- Before editing, inspect the relevant shared boundary, file size, and existing
  local patterns.
- If touching an oversized or mixed-responsibility file, look for a small
  extraction that supports the requested change.
- Preserve user changes and avoid unrelated rewrites.
- After changes, report tests run, KMP/shared impact, privacy impact, and any
  intentionally deferred cleanup.
- If a requested change would violate local-first or shared-first rules, pause
  and explain the tradeoff before implementing it.

## Documentation Rule

Any feature that changes permissions, storage, backup/import, notifications,
platform assumptions, shared-module boundaries, or privacy behavior must update
README/CONTRIBUTING docs and relevant plan files in the same change.

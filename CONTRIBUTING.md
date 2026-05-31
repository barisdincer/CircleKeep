# Contributing to CircleKeep

Thanks for taking a look at CircleKeep. The project is small by design: it should stay local-first, understandable, and privacy-preserving.

CircleKeep is Android-first today, but new work should keep a future Kotlin Multiplatform shared core in mind.

## Development Setup

Recommended setup:

- JDK 17
- Android SDK platform 36
- Android SDK build tools 36.0.0
- Android Studio or the Gradle wrapper

Useful checks:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

## Contribution Guidelines

- Keep contact and call-log data on device by default.
- Do not add network access without opening a design discussion first.
- Explain every new Android permission in `README.md`.
- Add focused tests for database migrations, backup/restore, reminders, and import logic.
- Keep UI copy human and personal rather than CRM-oriented.
- Keep business rules portable when practical. Reminder cadence, contact type behavior, backup DTOs, import/export rules, and validation logic should not depend on Android framework APIs.
- Keep Android-specific APIs at the app edge: Room, `Context`, `ContentResolver`, notifications, contacts, call logs, file pickers, and Compose UI.
- Prefer stable string keys and epoch-millis timestamps for concepts that may cross Android and Apple clients.

## Pull Requests

Please include:

- What changed.
- Why it matters.
- How it was tested.
- Any privacy or permission impact.
- Any KMP/shared-core impact, especially if Android-specific dependencies were introduced.

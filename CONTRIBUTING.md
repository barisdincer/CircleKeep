# Contributing to CircleKeep

Thanks for taking a look at CircleKeep. The project is small by design: it should stay local-first, understandable, and privacy-preserving.

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

## Pull Requests

Please include:

- What changed.
- Why it matters.
- How it was tested.
- Any privacy or permission impact.

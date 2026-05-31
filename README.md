# CircleKeep

CircleKeep is a local-first Android app for remembering the people you care about. It helps you keep simple check-in rhythms for friends, family, colleagues, and wider circles without turning personal relationships into a sales pipeline.

The project is Android-first today, but it is being shaped for a future Kotlin Multiplatform shared core so the same relationship rhythm logic can support Apple clients later.

The app stores people in reminder cycles, surfaces who is due for a check-in, records recent interactions, and can optionally match device call-log entries to people you already track.

## Problem

Modern contact lists are huge, passive, and easy to forget. Calendar reminders are too rigid for personal relationships, while CRM tools are built for revenue workflows rather than care, memory, and lightweight follow-up.

CircleKeep focuses on a smaller question: who should I check in with, and when did I last reach out?

## Who Uses It?

CircleKeep is for people who want a private, low-friction way to maintain personal relationships:

- People who move cities, change jobs, or maintain long-distance friendships.
- Founders, makers, and community builders who want to remember warm connections without adopting a business CRM.
- Families and friend groups where a small reminder can prevent months of accidental silence.
- Anyone who wants contact reminders that stay on their own device.

## Privacy Model

CircleKeep is designed as a local-first app:

- Contact data is stored in the app's local Room database.
- The app does not require an account.
- The app does not use a backend service.
- The app does not request internet access.
- JSON backup and restore are user-initiated local file actions.
- Local profile and theme preferences stay on the device and are not included in JSON backups yet.
- Android Auto Backup is disabled so the app database is not silently backed up by the OS.
- Contacts and call logs are read only after Android runtime permission prompts.

The current project intentionally avoids analytics, ads, cloud sync, remote configuration, and third-party tracking SDKs.

## Permissions Explained

CircleKeep asks only for permissions that map directly to user-visible features.

`READ_CONTACTS`
: Used to import selected people from the device address book. Contacts are not uploaded.

`READ_CALL_LOG`
: Used to match recent incoming or outgoing phone calls against people already stored in CircleKeep, then update the last-contact date. Call logs are not uploaded.

`POST_NOTIFICATIONS`
: Used on Android 13+ to show daily check-in reminders.

If a permission is denied, the related feature is disabled while the rest of the app continues to work.

## Local-First Architecture

CircleKeep is a Kotlin Android app built with:

- Jetpack Compose for UI.
- Room for local persistence.
- Kotlin coroutines and Flow for reactive state.
- Android contacts and call-log providers for optional device integrations.
- SharedPreferences for local-only profile and theme preferences.
- JSON export/import for portable local backups.

Core data currently lives in Room entities:

- `Wave`: a reminder cycle such as "Yakınlar", "Arkadaşlar", or "Tanıdıklar".
- `ContactType`: a user-visible interaction type such as call, message, or meeting.
- `Person`: a tracked contact with phone, notes, tags, memory fields, reminder state, and last interaction dates.
- `InteractionLog`: a local record of contact moments with type, timestamp, and optional note.

Database migrations are explicit and non-destructive. Existing local data is migrated through the `1 -> 2 -> 3 -> 4 -> 5` chain instead of being wiped.

### KMP-Ready Direction

CircleKeep should keep platform-specific code at the edges. Relationship rhythm rules, due/upcoming grouping, contact type behavior, backup DTOs, and import/export rules should remain portable Kotlin whenever practical. Android-only pieces such as Room, contacts, call-log access, notification actions, and Compose screens should act as platform adapters around that core.

Future Apple support should preserve the same privacy model: local storage, no required account, no backend dependency, and explicit user-controlled import/export.

## Roadmap

- Improve onboarding and first-run permission education.
- Add editable reminder presets and smarter default cycles.
- Add search and richer filtering across people, tags, and cycles.
- Add encrypted backup options.
- Add better import conflict handling.
- Add accessibility and localization polish.
- Add optional widgets or quick actions for "called", "met", and "snooze".
- Extract portable models, reminder rules, and backup DTOs into a Kotlin Multiplatform shared module.
- Add an Apple client after the shared core boundary is stable.

## Contributing

Contributions are welcome, especially around privacy, Android UX, testing, accessibility, and local-first data design.

To run the project:

1. Install Android Studio and JDK 17.
2. Open the repository root in Android Studio.
3. Let Gradle sync complete.
4. Run `./gradlew testDebugUnitTest`.
5. Run the app on an emulator or physical Android device.

Call-log syncing requires a physical Android device with call-log permission granted.

Before opening a pull request:

- Keep data local by default.
- Avoid adding network access unless the privacy model is discussed first.
- Keep new business logic KMP-ready and free of Android framework dependencies when possible.
- Add or update tests for database, import, backup, and reminder logic.
- Explain any new permission in this README.

See [CONTRIBUTING.md](CONTRIBUTING.md) for the maintainer checklist.
Agent-specific architecture rules live in [AGENTS.md](AGENTS.md), and the KMP migration direction is tracked in [docs/plans/kmp-shared-architecture.md](docs/plans/kmp-shared-architecture.md).

## Security Policy

Please do not open a public issue for sensitive security reports. Use GitHub Security Advisories if available, or contact the maintainer privately.

Security-sensitive areas include:

- Permission handling.
- Contact and call-log access.
- Backup/restore parsing.
- Database migration safety.
- Any future sync or network feature.

CircleKeep is currently pre-1.0. Breaking changes may happen, but privacy regressions should be treated as bugs.

## License

CircleKeep is released under the MIT License. See [LICENSE](LICENSE).

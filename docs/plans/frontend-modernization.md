# Frontend Modernization

## Goal
CircleKeep should feel like a calm, premium relationship rhythm app rather than
an admin panel or lightweight CRM. The Android client remains the first UI, but
new behavior should keep shared-candidate decisions outside Compose where
practical.

## Design System
- Central Compose primitives live under `ui/design`: cards, section headers,
  search fields, filter rows, metric cards, empty states, form sections, and
  destructive confirmations.
- The palette uses a quieter teal/slate/accent system with explicit success,
  warning, error, and information colors.
- Cards and controls stay at an 8dp radius, with 4dp/8dp spacing rhythm and
  minimum 48dp tap targets for interactive controls.

## Screen Audit And Changes
- App shell: compact devices use bottom navigation; wider devices use a
  navigation rail. A global quick search dialog jumps directly to people or
  groups.
- Dashboard: the first screen now starts with a focus summary and KPI cards
  before the action bar and reminder list. Call-log permission is requested
  only when the user starts call-log matching.
- People: the list now has search, saved-style views, sorting, group filters,
  tag filters, clearer empty states, and status pills backed by portable list
  presentation logic.
- Person detail: the long single-column editor is split into `Genel`, `Ritim`,
  `Hafıza`, and `Geçmiş` tabs so each workflow is easier to scan.
- Group detail: member search and a waiting-member filter reduce friction in
  larger groups while group logs still operate on the whole group.
- Logs: search and saved-style views sit above the event list; grouping and
  filtering are handled in Android-free presentation code.
- Reports: chart rows and summary metrics are built by a portable summary
  builder instead of being calculated inside composables.
- Profile: remains local-first and keeps JSON backup/restore explicit and
  user-initiated.

## Shared Boundary
New portable presentation builders live in `data/presentation`:

- `DashboardPresentation`
- `PeopleListQuery` and `PeopleListItem`
- `InteractionLogQuery` and `InteractionLogPresentation`
- `ReportsSummary`

These types use Room entities for now because the app does not yet have a real
shared module, but they avoid Android and Compose imports so they can later move
toward a shared Kotlin core.

## Privacy And Permissions
- No network, analytics, account, backend, ad, cloud sync, or remote config
  dependency was added.
- Startup no longer asks for call-log or notification permissions. Call-log
  permission is requested in context from the Dashboard sync action.
- JSON backup format and Room schema are unchanged.

## Test Coverage
- Portable presentation behavior is covered by JVM tests for people filtering,
  interaction grouping/search, dashboard counts, and report summary math.
- Existing repository and reminder tests continue to protect local data and
  rhythm behavior.

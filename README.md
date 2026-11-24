# Evernote (internal codename)

Android app for internal staff: create notes from multiple photos (camera or gallery) plus text.
Kotlin + Jetpack Compose, offline-first, Supabase for database and storage.

Not for public release. "Evernote" is an internal codename only.

Jules↔Claude collaboration happens through issues and PR comments; see `docs/jules-bridge.md`.

## Building

Requires JDK 21 and the Android SDK with platform 37.2 (`platforms;android-37.2`). Gradle downloads itself through the wrapper. Debug builds only for now.

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest lintDebug
```

Architecture and layering: `docs/architecture.md`.

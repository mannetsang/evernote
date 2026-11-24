# Architecture

Evernote (internal codename) is an Android app for internal staff. A note is
some text plus one or more photos. It must work with no connectivity and sync
to Supabase when a connection is available.

## Stack

| Concern | Choice |
|---|---|
| Language / UI | Kotlin, Jetpack Compose, Material 3 |
| Architecture | MVVM, unidirectional data flow (`StateFlow` out, events in) |
| DI | Hilt |
| Local database | Room (SQLite) |
| Photo files | App-private storage (`filesDir/photos`), never public URLs |
| Camera | CameraX |
| Gallery | Android Photo Picker (multi-select, no storage permission) |
| Background sync | WorkManager, workers built through Hilt |
| Remote (later) | Supabase: Postgres with row-level security, private Storage bucket |
| Min / target / compile SDK | 26 / 37 / 37.2 |
| Build | Gradle 9.9 Kotlin DSL, AGP 9.5 (built-in Kotlin), KSP, version catalog in `gradle/libs.versions.toml` |
| CI | GitHub Actions: unit tests, lint, debug APK |

## Layers

```
ui/          Compose screens + ViewModels. No Android storage or network code.
domain/      Plain Kotlin models (Note, Photo). No Android imports.
data/        NoteRepository (interface) and its implementations.
  local/     Room entities, DAOs, database. The source of truth on device.
  files/     PhotoStore: compress, write, read, delete photo files.
  remote/    (later) Supabase client, behind the same NoteRepository.
sync/        (later) WorkManager workers that push local changes up and pull
             remote changes down.
auth/        AuthProvider interface. Sign-in method is undecided, so nothing
             above this interface may assume how a user is identified.
di/          Hilt modules binding interfaces to implementations.
```

Rules that keep the later Supabase work from touching the UI:

- ViewModels talk only to `NoteRepository` and `AuthProvider` interfaces.
- The offline implementation is the one the app ships with first. Sync is a
  second implementation (or a decorator) that keeps Room as the source of
  truth and reconciles with the remote in the background.
- Photos are compressed on device (about 1600 px on the long edge, JPEG) before
  they are written anywhere. The original is never kept.
- Every note and photo has a client-generated UUID so offline-created records
  can be uploaded without ID clashes.
- Deletes are soft (`deletedAt`) locally so they can be propagated, then purged
  after sync confirms.

## Open decisions (do not hard-wire)

- Sign-in method: an emailed link restricted to the company domain is the
  recommendation, not a decision. `AuthProvider` hides it.
- Private vs. shared notes: the model carries an `ownerId` and nothing else
  assumes privacy, so sharing can be added without a migration of meaning.
- Distribution: debug builds only. No signing config, no Play setup.

## Secrets

Supabase URL and anon key (when they exist) are read from
`local.properties` or CI secrets and injected through `BuildConfig`. Nothing of
the kind is committed; the repo is public.

## Milestones

1. **M0/M1** project skeleton, CI, this note. *(this PR)*
2. **Offline MVP** create, list, view, edit, delete notes; multiple photos from
   camera and gallery; reorder and remove photos; all offline.
3. **Supabase sync** behind the repository abstraction.
4. Later: search, tags, sharing, export.

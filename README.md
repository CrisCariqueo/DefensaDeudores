# Defensa de Deudores

Money-lending tracker. See `SCOPE.md` for the full feature spec and `DATA_MODEL.md`
for the schema/rationale. This README covers project setup only.

## What's built

- Gradle/Kotlin/Compose project (`app/`), package `com.cristobalcariqueo.defensadedeudores`
- `minSdk 29`, `targetSdk 35`, Material3 dark/light theme
- **Local-first v1 feature set is implemented and compiling** (`assembleDebug`
  green, matcher unit tests pass, detekt clean): Room DB mirroring the Supabase
  schema, onboarding, People/Sources CRUD, Main + Track screens (graphs,
  quick-create, tables, search/filters), return-search + retReg matching,
  edit/supersede flow, Config screen (theme/language/font/colors/table sizes)
- Room is the source of truth; the Supabase sync engine + auth is the one
  remaining v1 task and needs the Supabase project below to exist first
- supabase-kt is pinned to 3.x (`auth-kt` and the `io.github.jan.supabase.auth`
  namespace don't exist in 2.x)
- `supabase/schema.sql` -- full DDL matching `DATA_MODEL.md`, including RLS policies and triggers
- detekt config tuned for Compose (PascalCase composables, dp/sp literals)

Gradle needs a JDK 17-21 (Java 25 is too new for Gradle 8.10); point
`JAVA_HOME` or Android Studio's Gradle JDK at one, e.g.
`JAVA_HOME=~/.jdks/jdk-21.0.11+10 ./gradlew assembleDebug`.

## Setup, in order

1. **Open in Android Studio.** It'll regenerate `gradle/wrapper/gradle-wrapper.jar`
   (deliberately not committed -- binary, Studio/`gradle wrapper` recreates it)
   and `local.properties`' `sdk.dir` automatically on first sync. Accept any
   AGP/Gradle upgrade prompt it offers.

2. **Create the Supabase project** (dashboard, not automatable from here --
   needs your account): supabase.com -> New Project. Grab the project URL and
   anon key from Settings -> API.

3. **Run the schema**: paste `supabase/schema.sql` into the Supabase SQL Editor
   and run it once. Creates all tables, RLS policies, indexes, and the
   auto-provision-settings-row trigger.

4. **Local secrets**: copy `local.properties.example` to `local.properties`,
   fill in `SUPABASE_URL` / `SUPABASE_ANON_KEY` from step 2. This file is
   gitignored -- never commit real keys.

5. **Google Sign-In** (needed before auth works, not just for later):
   - Get your debug keystore's SHA-1: `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android`
   - Google Cloud Console -> Credentials -> OAuth Client ID -> Android type.
     Package name: `com.cristobalcariqueo.defensadedeudores` (or
     `.debug`-suffixed for debug builds, see `applicationIdSuffix`). Paste
     the SHA-1.
   - Repeat for your release keystore once you have one, before shipping.
   - Supabase dashboard -> Authentication -> Providers -> Google: paste the
     OAuth Client ID + secret.

6. **Build**: `./gradlew assembleDebug` (or just hit Run in Android Studio).

## Notes for whoever picks this up

- Fonts are placeholder (`FontFamily.Default` in `ui/theme/Type.kt`) --
  SCOPE.md wants something close to Claude's UI font; pin the real family
  before this goes further.
- App icon (`res/drawable/ic_launcher_*.xml`) is a placeholder shape, swap
  before release.
- Room (offline cache) isn't scaffolded yet -- `data/local/` is an empty
  placeholder directory. Add it when building whichever repository needs
  offline reads first (task #5 is the natural place to start).

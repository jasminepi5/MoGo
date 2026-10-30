# MoGo: Green Ride-Sharing for Monash

MoGo is an Android app for sharing rides between Monash University students and staff. Drivers post trips to campus with their free seats, riders book a seat and choose a pickup point on the map, and both see the CO₂ they've saved together.

Built as a final-year Computer Science project (FIT3161 and FIT3162) by Bri, Cai, Jas and Pia. This repository is Jas's fork of the [team repository](https://github.com/caileensutedja/MoGo).

## Features

- Sign-up limited to `@monash.edu` and `@student.monash.edu` email addresses
- Drivers post rides (with address autocomplete), including weekly repeats for up to 12 weeks
- Riders search by campus and date, pick a pickup point on the map and book a seat
- Live locations for driver and rider during the ride
- Safety: emergency contacts, Share Trip and an SOS text message
- CO₂ saved and kilometres shared on the dashboard

## Tech stack

- Kotlin, Jetpack Compose and Material 3, following MVVM (screens → view models → repositories)
- Supabase: Auth, PostgreSQL, Realtime and Storage, through [supabase-kt](https://github.com/supabase-community/supabase-kt)
- Google Maps Platform: Maps SDK for Android, Places API and Routes API
- Coroutines and StateFlow, Coil for images, DataStore for local settings

## Getting started

1. Install [Android Studio](https://developer.android.com/studio) (it bundles a suitable JDK).
2. Clone this repository and open the folder in Android Studio.
3. Create a `local.properties` file in the project root with your own keys. It's git-ignored, so keys never get committed:

   ```properties
   sdk.dir=<path to your Android SDK>
   SUPABASE_URL=https://<your-project>.supabase.co
   SUPABASE_ANON_KEY=<your Supabase anon key>
   MAPS_API_KEY=<your Google Maps Platform key>
   MAPS_SHA1_FINGERPRINT=<SHA-1 of your signing certificate>
   ```

   The Secrets Gradle Plugin makes these available as `BuildConfig` fields and in `AndroidManifest.xml`. Without `local.properties`, the build uses the placeholders in `local.defaults.properties`: the app compiles, but can't sign in or load maps.

   The Maps key needs the Maps SDK for Android, Places API (New) and Routes API enabled, restricted to this app's package name (`com.fit3161.fit3162.mogo`) and the SHA-1 above.
4. Run the app on an emulator or a phone running Android 8.0 (API 26) or newer.

## Building and testing

```bash
./gradlew assembleDebug        # build the debug APK
./gradlew testDebugUnitTest    # unit tests
./gradlew lintDebug            # Android Lint (fails only on issues not in app/lint-baseline.xml)
```

GitHub Actions runs all three on every push and pull request (see `.github/workflows/android.yml`).

## Database

The schema is in `mogo_dump.sql`, a `pg_dump` of the Supabase project from June 2026. To version future changes, link the Supabase CLI to your project and pull the schema into migrations:

```bash
npx supabase link --project-ref <your-project-ref>
npx supabase db pull
```

## Known issues

See [TODO.md](TODO.md) for the bugs and improvements found in the code review, ordered by priority.

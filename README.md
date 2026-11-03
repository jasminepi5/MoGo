# MoGo: Green Ride-Sharing for Monash

MoGo is an Android app for sharing rides between Monash University students and staff. Drivers post trips to campus with their free seats, riders book a seat and choose a pickup point on the map, and both see the CO₂ they've saved together.

Built as a final-year Computer Science project (FIT3161 and FIT3162) by team CS 15.
This repository is Jasmine's copy of the original [team repository](https://github.com/caileensutedja/MoGo), where the project was built.

## Team

| Name             | GitHub                                                                                           | LinkedIn                                                                  |
|------------------|--------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------|
| Brianna Vaughan  | [briclare](https://github.com/briclare)                                                          | [brianna-vaughan](https://www.linkedin.com/in/brianna-vaughan-80b968354/) |
| Caileen Sutedja  | [caileensutedja](https://github.com/caileensutedja), [caileenstj](https://github.com/caileenstj) | [caileensutedja](https://www.linkedin.com/in/caileensutedja/)             |
| Priyanshi Sharma | [piasharma2004](https://github.com/piasharma2004)                                                | [piasharma1](https://www.linkedin.com/in/piasharma1/)                     |
| Jasmine Irawan   | [jasminepi5](https://github.com/jasminepi5)                                                      | [jasmine-irawan](https://www.linkedin.com/in/jasmine-irawan/)             |

### Contributions

Summarised from the team repository's commit history (Feb to Jun 2026). Work done outside the code, such as design, reports, testing and coordination, doesn't show up there.

- **Brianna Vaughan**
  - Initial UI layouts for the Home Dashboard, Profile, Settings and booking preview screens.
  - Session management: staying signed in between app launches (session storage with DataStore).
  - Safety features: emergency contacts imported from the phone's contacts, managed in Settings.
  - Active Ride screen with the SOS and Share Trip actions.
  - Navigation to Settings and Active Ride from the profile and dashboard.

- **Caileen Sutedja**
  - Project setup.
  - Welcome, Sign In and Register screens with input validation.
  - Bottom navigation bar, and switching between rider and driver mode.
  - Ride search for riders, with filters for date, campus and hiding unwanted rides.
  - Upload Ride screen for drivers, including the 24-hour notice rule.
  - My Rides screen, Offers screen and home campus selection.

- **Priyanshi Sharma**
  - Home Dashboard: booked rides, the ongoing ride tile and notifications.
  - Profile screen with profile details and profile picture upload.
  - Register screen improvements: phone number handling and password strength rules.
  - Email verification page ([docs/verification.html](docs/verification.html)).
  - Driver view of My Rides, and fixes to ride filters and booking.

- **Jasmine Irawan**
  - Supabase setup and the app's architecture: the shared Supabase client, sign-in and registration logic, and screen navigation.
  - Google Maps Platform integration: the map, routes through the Routes API, and address autocomplete with the Places API.
  - Booking flow: choosing a pickup point, and the booking preview with the full route.
  - Live ride tracking, ride cancellation with reasons, and weekly repeating rides.
  - Manual functional testing for the app: tested each feature against the requirements traceability matrix.


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

# MoGo TODO

Bugs and fixes found in a code review on 30 Oct 2026, updated on 3 Nov 2026 after the `refactor/code-clean-up` branch. Line numbers refer to that branch (commit `a7ec1a7` plus the lint fix), so they shift as files change.

Database items are based on `mogo_dump.sql` (8 Jun 2026). Check each one against the live Supabase project before fixing, because it may have changed since the dump.

Items are ordered by impact: **Critical** items affect user safety or data, **High** items are bugs users will hit, **Medium** items are correctness and performance problems, and **Cleanup** covers dead code, build and repo hygiene. Ticked items list the commit that fixed them; unticked items that were partly fixed say what's done and what's left.

Each open item says who does it:

- **Owner: you**: learning tasks. Jas writes the code; Claude answers questions and reviews, without writing the solution.
- **Owner: pair**: Jas designs and writes the core logic; Claude helps with boilerplate, repetition and test setup.
- **Owner: Claude**: mechanical fixes with little to learn. Claude makes the change and Jas reviews it.

Suggested order for the learning tasks: 1, 2, 20 and 7 together as migrations for the new Supabase project, then 4 and 10, then 5, then 22 and 11, then 12, then 14.

---

## Critical

- [ ] **1. Database is open to anyone (row-level security off, all privileges granted to `anon`)** · Owner: you
  - Grants: [mogo_dump.sql#L3008](mogo_dump.sql#L3008) to [#L3098](mogo_dump.sql#L3098) (`GRANT ALL ON TABLE public.* TO anon`)
  - Tables: [mogo_dump.sql#L1277](mogo_dump.sql#L1277) to [#L1476](mogo_dump.sql#L1476). No `ENABLE ROW LEVEL SECURITY` on any public table.
  - The anon key ships in every APK and is in the public git history (commits `b200044`, `739fae8`, `b2731c9`, file `MainActivity.kt`), so anyone can read, change or delete every user's name, phone, email, date of birth and gender, emergency contacts, and live locations.
  - Selecting fewer columns in the app (item 13) doesn't help until this is fixed: anyone with the anon key can still query the full tables.
  - Fix: enable RLS on every table and add policies, then revoke `anon`'s table grants and run Supabase's Security Advisor.
    - `users`: expose only a limited public profile.
    - `bookings`: rider, or the ride's driver.
    - `rides`: anyone can read; only the driver can write.
    - `emergency_contacts` / `trusted_contacts`: owner only.

- [ ] **2. `anon` can run every database function, including `SECURITY DEFINER` ones** · Owner: you
  - [mogo_dump.sql#L479](mogo_dump.sql#L479) `create_user_profile(user_id, ...)` runs as the owner and accepts any user ID. Granted to `anon` at [#L2765](mogo_dump.sql#L2765).
  - Also granted to `anon`: `decrement_seats` [#L2775](mogo_dump.sql#L2775), `handle_new_user` [#L2784](mogo_dump.sql#L2784), `increment_seats` [#L2793](mogo_dump.sql#L2793).
  - Fix: `REVOKE EXECUTE ... FROM anon, public`, grant only what the app calls, and check `auth.uid()` inside each function.

- [ ] **3. SOS never sends: SMS permission is never requested at runtime** · Owner: pair
  - [app/src/main/AndroidManifest.xml#L14](app/src/main/AndroidManifest.xml#L14) declares `SEND_SMS`, but nothing requests it at runtime (it's a dangerous permission).
  - [ActiveRideScreen.kt#L536](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/ActiveRide/ActiveRideScreen.kt#L536): `sendSms` catches the `SecurityException` and only calls `printStackTrace()` ([#L540](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/ActiveRide/ActiveRideScreen.kt#L540)), so the user never learns it failed.
  - [ActiveRideScreen.kt#L125](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/ActiveRide/ActiveRideScreen.kt#L125): the SOS message has no location, and `sendTextMessage` fails on messages over 160 characters. Share Trip ([#L93](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/ActiveRide/ActiveRideScreen.kt#L93)) has the same problems.
  - Telephony is now optional in the manifest ([#L16](app/src/main/AndroidManifest.xml#L16)), so tablets without SMS can install the app.
  - [ActiveRideScreen.kt#L538](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/ActiveRide/ActiveRideScreen.kt#L538): `SmsManager.getDefault()` is deprecated.
  - Fix:
    - request `SEND_SMS` before sending;
    - check `PackageManager.FEATURE_TELEPHONY` first and offer Share or Call instead on devices without it;
    - use `context.getSystemService(SmsManager::class.java)`, `divideMessage` and `sendMultipartTextMessage`;
    - add a Google Maps link to the current location;
    - show success or failure per contact;
    - add a "Call 000" button.

- [ ] **4. Booking never reduces `available_seats`; cancelling adds seats back** · Owner: you
  - [BookRepository.kt#L594](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/BookRepository.kt#L594) `bookRide` inserts a booking ([#L617](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/BookRepository.kt#L617)) but never decrements seats.
  - [BookRepository.kt#L387](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/BookRepository.kt#L387) `cancelBooking` adds seats back by reading and then writing the count, so every cancellation inflates it.
  - [mogo_dump.sql#L506](mogo_dump.sql#L506) `decrement_seats` exists but the app never calls it.
  - Also not atomic: `bookRide` deletes and then inserts ([#L608](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/BookRepository.kt#L608), [#L617](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/BookRepository.kt#L617)); `cancelRide` makes two updates ([#L323](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/BookRepository.kt#L323), [#L335](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/BookRepository.kt#L335)); `completeRide` makes two updates ([#L486](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/BookRepository.kt#L486), [#L499](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/BookRepository.kt#L499)).
  - Fix: Postgres functions `book_ride`, `cancel_booking`, `cancel_ride` and `complete_ride`, each checking and updating seats and statuses in one transaction and refusing a full ride. Call them with `postgrest.rpc(...)`.

## High

- [ ] **5. Departure times are stored 10–11 hours off (local time saved as UTC)** · Owner: you
  - [UploadRideViewModel.kt#L214](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/UploadRide/UploadRideViewModel.kt#L214): `atOffset(ZoneOffset.UTC)` treats the Melbourne time the driver typed as UTC.
  - [UploadRideViewModel.kt#L222](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/UploadRide/UploadRideViewModel.kt#L222): the 24-hour check compares local input with UTC now.
  - Wrong as a result:
    - the date filter: [BookRepository.kt#L239](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/BookRepository.kt#L239), which uses Melbourne day boundaries;
    - the countdowns: [HomeDashboard.kt#L91](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/HomeDashboard/HomeDashboard.kt#L91), [#L114](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/HomeDashboard/HomeDashboard.kt#L114), [BookScreen.kt#L69](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/BookScreen/BookScreen.kt#L69), [#L84](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/BookScreen/BookScreen.kt#L84);
    - the 30-minute filter: [BookRepository.kt#L217](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/BookRepository.kt#L217).
  - Display only looks right because [BookScreen.kt#L400](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/BookScreen/BookScreen.kt#L400) formats without converting the time zone.
  - Fix: build the time with `ZoneId.of("Australia/Melbourne")`, store the UTC instant, and display with `atZoneSameInstant(...)`. Put the conversion in a pure function and unit-test it, including the daylight-saving changeovers.

- [ ] **6. Staff can register but can't sign in** · Owner: Claude (or you, as a test-first warm-up)
  - [SignInViewModel.kt#L55](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/SignInScreen/SignInViewModel.kt#L55) accepts `@monash.edu.au` (message at [#L28](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/SignInScreen/SignInViewModel.kt#L28)).
  - [SignInScreen.kt#L117](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/SignInScreen/SignInScreen.kt#L117) has its own check, `endsWith(".monash.edu")`, which also rejects `name@monash.edu`.
  - [RegisterViewModel.kt#L159](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/RegisterScreen/RegisterViewModel.kt#L159) and the database trigger [mogo_dump.sql#L462](mogo_dump.sql#L462) require `@monash.edu`.
  - Fix: one shared `isMonashEmail()` (in `utils/`, with unit tests) used by both screens and the sign-in field.

- [ ] **7. Choosing "Other" for gender leaves the user without a profile** · Owner: you
  - [RegisterScreen.kt#L260](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/RegisterScreen/RegisterScreen.kt#L260) sends `"Other: <text>"`, and [RegisterViewModel.kt#L161](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/RegisterScreen/RegisterViewModel.kt#L161) accepts it.
  - The `gender` enum [mogo_dump.sql#L232](mogo_dump.sql#L232) has no such value, so the cast in `handle_new_user` fails. Its `EXCEPTION WHEN OTHERS` ([mogo_dump.sql#L548](mogo_dump.sql#L548)) only raises a warning, so sign-up succeeds but no `public.users` row is created, and the app breaks later for that user.
  - The dump also has no trigger that runs `handle_new_user` on `auth.users`. Check the team project's dashboard (Database → Triggers) for how profiles were really created before writing the new project's migration.
  - Fix: store `Other` in `user_gender` and the free text in a separate nullable column, and let the trigger fail sign-up instead of swallowing errors.

- [ ] **8. Logout doesn't sign out of Supabase** · Owner: Claude
  - [AppNavigation.kt#L275](app/src/main/java/com/fit3161/fit3162/mogo/ui/navigation/AppNavigation.kt#L275) only clears the DataStore flag.
  - [AuthRepository.kt#L66](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/AuthRepository.kt#L66) `logout()` (which calls `signOut()`) is never called, so the session and refresh token stay on the phone.
  - Fix: call `authRepository.logout()` before clearing local state.

- [x] **9. App only installs on Android 15+**
  - Fixed in `6cc4bf6`: `minSdk = 26` ([app/build.gradle.kts#L19](app/build.gradle.kts#L19)). Lint reports no API-level errors.

- [ ] **10. Completing a ride turns cancelled bookings into "completed"** · Owner: you
  - [BookRepository.kt#L499](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/BookRepository.kt#L499) to [#L503](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/BookRepository.kt#L503) updates every booking for the ride, with no status filter. Cancelled riders then get the ride history and the CO₂ credit. The comment at [#L498](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/BookRepository.kt#L498) says "confirmed", which the code doesn't do.
  - Fix: filter `eq("booking_status", BookingStatus.CONFIRMED)`, or do it inside the `complete_ride` function from item 4.

## Medium

- [ ] **11. CO₂ figures are mostly estimates, and "saved" is calculated wrongly** · Owner: you
  - [UploadRideViewModel.kt#L135](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/UploadRide/UploadRideViewModel.kt#L135) to [#L160](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/UploadRide/UploadRideViewModel.kt#L160): the distance comes from a name-matching table that usually falls back to 10 km; it's used at [#L170](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/UploadRide/UploadRideViewModel.kt#L170) to [#L176](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/UploadRide/UploadRideViewModel.kt#L176).
  - [HomeDashboardViewModel.kt#L126](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/HomeDashboard/HomeDashboardViewModel.kt#L126) to [#L132](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/HomeDashboard/HomeDashboardViewModel.kt#L132): riders are credited with the whole trip's emissions, and drivers with their own driving.
  - Distance and duration are worked backwards from the CO₂ estimate (wrong for EVs and hybrids, and assuming 40 km/h): [HomeDashboardViewModel.kt#L148](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/HomeDashboard/HomeDashboardViewModel.kt#L148), [#L164](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/HomeDashboard/HomeDashboardViewModel.kt#L164), [#L180](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/HomeDashboard/HomeDashboardViewModel.kt#L180), [#L198](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/HomeDashboard/HomeDashboardViewModel.kt#L198), [BookViewModel.kt#L85](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/BookScreen/BookViewModel.kt#L85).
  - Fix: use the real route distance (Routes API at upload, `estimated_distance_meters` on bookings). Count "saved" as the rider's avoided solo trip.

- [ ] **12. Session handling is fragile** · Owner: you
  - [AppNavigation.kt#L103](app/src/main/java/com/fit3161/fit3162/mogo/ui/navigation/AppNavigation.kt#L103): the start screen comes from a DataStore flag that starts as `false`, so it can switch after launch.
  - Screens fall back to an empty user ID before Supabase restores the session: [#L175](app/src/main/java/com/fit3161/fit3162/mogo/ui/navigation/AppNavigation.kt#L175), [#L198](app/src/main/java/com/fit3161/fit3162/mogo/ui/navigation/AppNavigation.kt#L198), [#L212](app/src/main/java/com/fit3161/fit3162/mogo/ui/navigation/AppNavigation.kt#L212), [#L231](app/src/main/java/com/fit3161/fit3162/mogo/ui/navigation/AppNavigation.kt#L231).
  - [SessionManager.kt#L30](app/src/main/java/com/fit3161/fit3162/mogo/data/SessionManager.kt#L30): a 7-day expiry runs separately from Supabase's own session.
  - Fix: drive navigation from `supabase.auth.sessionStatus`, show a splash while it loads, and remove `SessionManager`.

- [ ] **13. Too much personal data is fetched; avatars are public** · Owner: you
  - Done in `dd9311f`: joins select [RIDE_USER_COLUMNS](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/BookRepository.kt#L135) instead of `users(*)`, so email and date of birth are no longer fetched.
  - Left:
    - [BookRepository.kt#L136](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/BookRepository.kt#L136): ride search still fetches every driver's `user_phone` and `user_gender`. Fetch the phone only for rides the user has booked.
    - [ImageRepository.kt#L26](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/ImageRepository.kt#L26): the path has a doubled `avatars/avatars/...`. [#L30](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/ImageRepository.kt#L30) uses a public URL.
    - Column selection is only a client-side courtesy until item 1 adds RLS.
  - Fix: use a private bucket with signed URLs, and a `public_profiles` view (or RLS column grants) for what other users can see.

- [ ] **14. Live tracking polls the database every 20 seconds** · Owner: you (stretch)
  - [ActiveRideViewModel.kt#L68](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/ActiveRide/ActiveRideViewModel.kt#L68), [#L137](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/ActiveRide/ActiveRideViewModel.kt#L137). Realtime is installed but unused ([MogoApplication.kt#L36](app/src/main/java/com/fit3161/fit3162/mogo/MogoApplication.kt#L36)), and tracking stops when the app is in the background.
  - [ActiveRideViewModel.kt#L97](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/ActiveRide/ActiveRideViewModel.kt#L97) falls back to any confirmed booking even if its ride hasn't started.
  - Fix: use a Supabase Realtime channel (broadcast or row changes) and a foreground service for location updates.

- [ ] **15. Errors are swallowed into empty lists, so "failed" looks like "nothing found"** · Owner: pair
  - Done in `2e7f6be`: the `BookRepository` list functions and `EmergencyContactRepository.getContacts` now throw, and the view models show the message.
  - Left:
    - [ActiveRideViewModel.kt#L87](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/ActiveRide/ActiveRideViewModel.kt#L87): contacts load inside the same `try` as the ride, so if contacts fail the whole Active Ride screen, SOS button included, shows an error. Load them separately and keep the screen usable.
    - Still return `null` on any error: [ProfileRepository.kt#L62](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/ProfileRepository.kt#L62) `getProfile`, [ImageRepository.kt#L32](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/ImageRepository.kt#L32) avatar upload.
    - Errors show as red text (for example [MyRidesScreen.kt#L103](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/MyRides/MyRidesScreen.kt#L103)) with no way to retry.
  - Fix: return `Result<T>` (or a Loading/Error/Content state) and show an error state with a Retry button.

- [ ] **16. Cancelled rides never reappear in search** · Owner: Claude
  - [BookRepository.kt#L222](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/BookRepository.kt#L222) and [#L259](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/BookRepository.kt#L259) exclude every ride the rider ever booked, cancelled ones included. `getBookedRideIds` ([#L151](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/BookRepository.kt#L151)) already excludes cancelled bookings.
  - Fix: reuse `getBookedRideIds`.

- [x] **17. My Rides runs one query per ride**
  - Fixed in `8b7b22f`: [getBookingsForRides](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/BookRepository.kt#L194) loads all bookings in one query.
  - Follow-up: it sends every ride ID the driver has ever posted in the URL ([#L204](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/BookRepository.kt#L204)), so the request grows with history. Filter by driver through a join (`rides!inner(driver_id)`) instead, or only ask for active rides. Owner: Claude.

- [ ] **18. Personal data is written to logs in release builds** · Owner: Claude
  - Done in `e3e23ef`: the phone number and device location are no longer logged, and the Routes API headers are logged only in debug builds with the API key redacted.
  - Left: `DATE_DEBUG` logs still run in release builds: [BookRepository.kt#L255](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/BookRepository.kt#L255), [#L285](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/BookRepository.kt#L285), [#L287](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/BookRepository.kt#L287), [FutureRideViewModel.kt#L100](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/FutureRideScreen/FutureRideViewModel.kt#L100).
  - Fix: remove them, or log only when `BuildConfig.DEBUG` is true (or use Timber with a debug-only tree).

- [ ] **19. Phone validation assumes Australian numbers for every country** · Owner: Claude
  - Done in `2acdfd1`: registration validates per country with `libphonenumber` and stores E.164 ([PhoneNumbers.kt](app/src/main/java/com/fit3161/fit3162/mogo/utils/PhoneNumbers.kt), with tests).
  - Left:
    - [ProfileViewModel.kt#L101](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/Profile/ProfileViewModel.kt#L101): editing the phone in Profile saves any text without validation.
    - Existing rows are stored as `"+61 412345678"`. Convert them with a one-off SQL update, or parse both formats wherever the number is used.

- [ ] **20. Missing database constraints and indexes** · Owner: you
  - No `CHECK` on `available_seats >= 0` ([mogo_dump.sql#L1405](mogo_dump.sql#L1405)) or `seats_booked > 0` ([#L1297](mogo_dump.sql#L1297)).
  - No indexes on `rides.driver_id`, `rides.departure_time` or `bookings.rider_id`.
  - [mogo_dump.sql#L462](mogo_dump.sql#L462): `check_email_domain` is `SECURITY DEFINER` without `SET search_path`.

- [ ] **21. Check whether backups include the login session** · Owner: Claude
  - [AndroidManifest.xml#L20](app/src/main/AndroidManifest.xml#L20) `allowBackup="true"`, and both [backup_rules.xml](app/src/main/res/xml/backup_rules.xml) and [data_extraction_rules.xml](app/src/main/res/xml/data_extraction_rules.xml) are still the empty templates, so everything is backed up, including the Supabase session in shared preferences and the DataStore login flag.
  - Fix: exclude the auth/session storage in both files, or set `allowBackup="false"`.

## Cleanup

- [ ] **22. Dead code: the smart ride search is never called** · Owner: you
  - [BookViewModel.kt#L104](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/BookScreen/BookViewModel.kt#L104) `loadRides` (radius, detour, ranking) has no callers. It also contains a bug: `genderPref ?: ""` at [#L119](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/BookScreen/BookViewModel.kt#L119) and [#L121](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/BookScreen/BookViewModel.kt#L121) would filter for `user_gender = ""` and return no rides.
  - Helpers only it uses: [BookRepository.kt#L645](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/BookRepository.kt#L645) to [#L724](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/BookRepository.kt#L724), [MapsRepository.kt#L106](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/MapsRepository.kt#L106). The blocked-user sets are always empty ([BookViewModel.kt#L115](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/BookScreen/BookViewModel.kt#L115)), and the `blocked_users` table is unused.
  - Decide: wire it into `FutureRideViewModel` (then cache the original route, because it makes 2 Routes API calls per ride), or delete it.

- [x] **23. Dead code: unreachable Map screen with dummy data**
  - Fixed in `ddadfc5`: removed `Screen.Map`, `MapsScreenUI.kt`, `MapsViewModel.kt`, `MapsViewModelFactory.kt` and `DriverLocation.kt`.

- [x] **24. Dead code: test client and a second Supabase client**
  - Fixed in `ddadfc5` (removed `SupabaseClient.kt` and `TestViewModel.kt`) and `306a4f8` (`roleTrigger` is now passed to `AppNavigation`, so the dashboard reloads after a role switch).
  - `decrement_seats`/`increment_seats` ([mogo_dump.sql#L506](mogo_dump.sql#L506), [#L562](mogo_dump.sql#L562)) are still unused; reuse or drop them in item 4.

- [ ] **25. Duplicated code** · Owner: Claude
  - Done: one booking path in `FutureRideViewModel` (`0e67175`), one Routes API call shared by `getRoute` and `getRouteWithStop` (`3ca8dd6`), and the unused `PickupDialog` copy deleted (`b4598b5`).
  - Left: every booking is `seats_booked = 1` ([BookRepository.kt#L627](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/BookRepository.kt#L627)), and coordinates default to `0.0` instead of `null` ([#L598](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/BookRepository.kt#L598)).

- [x] **26. Package names don't match folders**
  - Fixed in `93596fd`: `DriverLocation.kt` deleted, `PlacesRepository.kt` moved to `data/repo`, `SessionManager.kt` moved to `data`, `BookingPreviewViewModel.kt` package fixed, and `UserRoleViewModel.kt` moved to `ui/navigation`.

- [ ] **27. Naming and small API issues** · Owner: Claude
  - Done: `isUserLoggedIn` typo and `getSupabaseClient()` removed (`9cff9c6`); statuses are constants in [Status.kt](app/src/main/java/com/fit3161/fit3162/mogo/data/model/Status.kt) (`b943b27`). They're `String` constants rather than enums, so an unknown database value can't crash decoding.
  - Left: [BookRepository.kt#L271](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/BookRepository.kt#L271) to [#L272](app/src/main/java/com/fit3161/fit3162/mogo/data/repo/BookRepository.kt#L272): `startUtc.format(formatter)` calls `String.format` on a string, which only works by accident. Pass `startUtc` directly.

- [ ] **28. Build file cleanup** ([app/build.gradle.kts](app/build.gradle.kts)) · Owner: pair
  - Done in `054cdb5`: duplicate splashscreen, Fragments navigation, benchmark, LiveData and the manual key loading removed; all versions moved into [libs.versions.toml](gradle/libs.versions.toml).
  - Left:
    - Release builds aren't shrunk: [#L34](app/build.gradle.kts#L34) `isMinifyEnabled = false`. Needs keep rules for kotlinx.serialization, Supabase, Retrofit and Gson, then a test on a device. (pair)
    - The rest of this list is Claude's.
    - Old Compose BOM (`2024.09.00`) with Compose pinned above it: [gradle/libs.versions.toml#L10](gradle/libs.versions.toml#L10). Move to a current BOM and drop the pins.
    - Two HTTP stacks: OkHttp/Retrofit for the Routes API and Ktor for Supabase. Calling the Routes API through Ktor would drop Retrofit, OkHttp and Gson.
    - Material 2 is still needed for pull-to-refresh on the dashboard; Material 3 has `PullToRefreshBox`.

- [x] **29. UI text is hard-coded**
  - Fixed in `f54cfad`: plain UI text moved to [strings.xml](app/src/main/res/values/strings.xml). Leftovers are in item 31.

- [ ] **30. Tests, CI, docs and repo hygiene** · Owner: pair
  - Done: CI runs lint, unit tests and a debug build on every push (`0449b61`); README rewritten (`a7ec1a7`); `.idea/` untracked (`2e858f2`); first real unit tests for `PhoneNumbers` (`2acdfd1`).
  - Left:
    - More unit tests for pure logic: time conversion (item 5), email check (item 6), CO₂ (item 11), seat rules (item 4).
    - [ExampleInstrumentedTest.kt#L19](app/src/androidTest/java/com/fit3161/fit3162/mogo/ExampleInstrumentedTest.kt#L19) is still the template.
    - The schema exists only as a dump ([mogo_dump.sql](mogo_dump.sql)). Move it to `supabase/migrations/` so RLS policies and functions are versioned.

- [ ] **31. Leftovers from the cleanup branch** · Owner: Claude
  - Strings that build text in code are still in Kotlin: "Error: …", "Driver: …", "Pickup: …" and "Vehicle Type: …" ([BookScreen.kt#L207](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/BookScreen/BookScreen.kt#L207), [#L332](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/BookScreen/BookScreen.kt#L332), [FutureRideScreen.kt#L282](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/FutureRideScreen/FutureRideScreen.kt#L282), [#L395](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/FutureRideScreen/FutureRideScreen.kt#L395), [MyRidesScreen.kt#L103](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/MyRides/MyRidesScreen.kt#L103), [#L414](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/MyRides/MyRidesScreen.kt#L414), [OfferScreen.kt#L60](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/OfferScreen/ui/OfferScreen.kt#L60), [ProfileScreen.kt#L145](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/Profile/ProfileScreen.kt#L145)). Use format strings such as `<string name="error_message">Error: %1$s</string>`.
  - Some string names were cut off when generated, for example `the_ride_has_ended_we_hope_you_had_a_saf`. Rename them to short names (`ride_ended_message`).
  - Deprecated APIs the compiler warns about:
    - `Modifier.menuAnchor()` without arguments: [BookScreen.kt#L278](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/BookScreen/BookScreen.kt#L278), [MyRidesScreen.kt#L228](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/MyRides/MyRidesScreen.kt#L228), [ProfileScreen.kt#L448](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/Profile/ProfileScreen.kt#L448), [#L541](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/Profile/ProfileScreen.kt#L541), [UploadRideScreen.kt#L155](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/UploadRide/UploadRideScreen.kt#L155), [#L320](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/UploadRide/UploadRideScreen.kt#L320);
    - `LocalClipboardManager`: [HomeDashboard.kt#L151](app/src/main/java/com/fit3161/fit3162/mogo/UIScreen/HomeDashboard/HomeDashboard.kt#L151);
    - `SmsManager.getDefault()`: see item 3.
  - [app/lint-baseline.xml](app/lint-baseline.xml) hides 49 older lint issues (mostly outdated dependencies and unused resources). Work through them and regenerate the baseline.

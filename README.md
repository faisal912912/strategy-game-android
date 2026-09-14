# Kingdom Frontier — Android

Starter Android client for the strategy game server.

## Included

- Kotlin and Jetpack Compose
- Landscape strategy-game city dashboard
- Resources, buildings, navigation, and starter domain models
- Server health client prepared for `/api/v1/health`
- Local emulator server URL: `http://10.0.2.2:8080/`
- Unit test and GitHub Actions APK build

## Open and run

1. Clone the repository and open it in Android Studio.
2. Let Gradle sync complete.
3. Start the Go game server on port 8080.
4. Run the app on an Android emulator (API 26 or newer).

Change `SERVER_BASE_URL` in `app/build.gradle.kts` when testing on a physical phone or production server.

The app name and package are temporary and can be changed before release.

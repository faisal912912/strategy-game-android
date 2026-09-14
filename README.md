# Kingdom Frontier Beta — Android

A portrait-mode Android beta client for the strategy game server.

## Beta features

- Governor onboarding and locally persisted profile
- Portrait-first Jetpack Compose interface
- City building upgrades with resource costs
- Troop training and technology research
- World map and playable campaign missions
- Commander roster
- Alliance dashboard and help actions
- Inbox, quests, inventory, rankings, and settings entry points
- Resource, troop, power, and progression state
- Server health client prepared for `/api/v1/health`
- Automated tests and APK generation with GitHub Actions

## Run

Open the repository in Android Studio, allow Gradle sync, and run on Android 8.0 (API 26) or newer.

The emulator server URL is `http://10.0.2.2:8080/`. Change `SERVER_BASE_URL` in `app/build.gradle.kts` for a physical phone or production host.

Current version: **Beta 0.9.0**. The game and package names remain temporary until release branding is selected.

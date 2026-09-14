# حُدود المملكة — Android online beta 0.2

An Arabic, portrait Android strategy client for the existing Core v30 server. This replaces the previous local demo: accounts, resources, troops and outcomes now come from the server.

## Play

Download `strategy-game-debug-apk` from the latest successful Android CI run in Actions, unzip it, and install `app-debug.apk` on Android 8 or newer. This is a debug-signed testing beta, not a Play Store production release.

1. Create an account with a username and a password of at least 10 characters. Keep both.
2. Upgrade buildings in **المدينة**. Review the resource costs before confirming.
3. Train troops in **الجيش**, then claim completed training.
4. Open **العالم → منطقة الحملات**, gather resources or assemble a hunting army. Compare army power against the monster before dispatch.
5. Claim completed campaigns from **أعمال المدينة** to return survivors and receive rewards. Heal wounded troops in the army screen.
6. Claim eligible daily rewards in **السجل**. Logging out and back in restores your server account.

## Implemented

- Real register/login/logout, encrypted Android Keystore session storage; passwords are never saved.
- HTTPS-only gateway with server identity and minimum client version checks.
- Arabic RTL interface, original animated vector citadel, button press animations and haptics, campaign result animation, reduced background motion setting.
- Server building upgrades, troop training, research and completion claims.
- Campaign gathering/hunting, troop selection, real losses, wounded troops, rewards and healing.
- New border scans, automatic gathering/hunt marches, gathering recall and server reports.
- Commander recruitment, daily quests, border daily missions, inbox read state, alliance creation/join/donation, inventory use and power rankings.
- Pending command body and idempotency key survive restart. Ambiguous failures retry the same command without optimistic resource credits.
- Account/gateway-scoped local timer metadata, foreground synchronization and visible error states.

## Beta boundaries

- The default Cloudflare quick tunnel URL is temporary. Change it in **إعدادات الاتصال** if it expires. Only Kingdom 1 has a verified gateway.
- New border regions need resource/monster spawns configured on the server. Empty regions show an empty state; the existing campaign region provides a playable loop.
- Older city APIs do not list active queues. Timers are saved on this device; recovery buttons can claim completed city work from another device. Classic campaign job IDs are local metadata: do not clear app storage with an active classic campaign. New border marches are fully retrieved from the server.
- The old campaign and new border hospitals/progression are separate server systems and labelled separately.
- Battles are server-resolved strategy commands and result animations, not real-time 3D combat. PvP warfare, chat, equipment progression, markets and the remaining Core v30 systems are not fully integrated.
- No offline account or fabricated starting resources. Old local demo progress is not imported into an online account.

## Build and validation

Java 17, Android SDK 35:

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew connectedDebugAndroidTest
```

CI publishes the APK and test/lint/device reports. Unit tests cover gateway validation, replay identity, null server arrays, version comparisons, time parsing and saved campaign claim IDs. The device test checks portrait launch and invalid registration prevention.

The opt-in live smoke test creates its own random account and verifies building, training, gathering and a winning hunt with idempotent replays and persistence after relogin. It does not access admin routes, chat or other players. Credentials stay in the gitignored `.local-qa/` directory.

```sh
python3 tools/live_smoke.py https://YOUR-GATEWAY
```

Package: `com.faisal.strategygame`. VersionCode: `2`; versionName: `0.2.0`. Production release still needs stable hosting, release signing and broader device/gameplay testing.

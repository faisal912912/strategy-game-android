## Beta 0.4 — Kingdom interface overhaul

A scene-first portrait client with a compact HUD, original illustrated map targets, interactive minimap/filter/nearby search, contextual building costs, cinematic heroes and a T1–T5 army progression track. See [release scope and validation](docs/BETA_0_4.md). The current default test gateway is `https://grows-warnings-rolling-volunteer.trycloudflare.com`; change it under connection settings when the temporary tunnel URL changes. Existing saved gateway and account data are preserved.

# حُدود المملكة — Android online beta 0.3

An Arabic, portrait Android strategy client for the existing Core v30 server. Accounts, resources, troops and outcomes come from the server. Beta 0.3 adds an illustrated, explorable kingdom and progression screens. See [release scope](docs/BETA_0_3.md) and [art direction](docs/ART_DIRECTION.md).

## Play

Download `strategy-game-debug-apk` from the latest successful Android CI run in Actions, unzip it, and install `app-debug.apk` on Android 8 or newer. This is a debug-signed testing beta, not a Play Store production release.

1. Create an account with a username and a password of at least 10 characters. Keep both.
2. Upgrade buildings in **المدينة**. Review the resource costs before confirming.
3. Train troops in **الجيش**, then claim completed training.
4. Open **المملكة**, drag and pinch to explore, and tap a resource or monster to dispatch. Use the flag button for existing campaigns and marches. Compare army power against the monster before dispatch.
5. Claim completed campaigns from **أعمال المدينة** to return survivors and receive rewards. Heal wounded troops in the army screen.
6. Recruit and develop commanders in **الأبطال**, browse packages in **المتجر**, and open **المزيد → المهام والسجل** for daily rewards. Logging out and back in restores your server account.

## Implemented

- Real register/login/logout, encrypted Android Keystore session storage; passwords are never saved.
- HTTPS-only gateway with server identity and minimum client version checks.
- Arabic RTL interface, ten original bundled illustrations, isometric town with animated citizens/torches, button press animations and haptics, campaign result animation, reduced background motion setting.
- Selectable town buildings with server upgrades; T1–T5 troop training with every tier requirement, price and timer; research, healing and completion claims.
- Panning and focal pinch zoom for town and kingdom, coordinate search, home position, real city/resource/monster markers.
- Hero skill progression and equipment crafting/equipping; premium wallet, server-priced gem offers and order history. Real-money checkout is explicitly disabled until a verified Play billing integration is provided.
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
- Battles are server-resolved strategy commands and result animations, not real-time 3D combat. PvP warfare, chat and the remaining Core v30 systems are not fully integrated. Training tiers are stored in queues by the server, while returned armies are aggregated by troop type; this client does not invent separate promoted troop counts.
- No offline account or fabricated starting resources. Old local demo progress is not imported into an online account.

## Build and validation

Java 17, Android SDK 35:

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew connectedDebugAndroidTest
```

CI publishes the APK and test/lint/device reports. Unit tests cover gateway validation, replay identity, null server arrays, version comparisons, time parsing and saved campaign claim IDs. Device tests check portrait launch, registration validation, building taps, world panning and target selection. Additional unit tests cover every troop tier, training costs and zoom/pan coordinate math. Scene screenshots use isolated local test fixtures, not real player accounts.

The opt-in live smoke test creates its own random account and verifies building, training, gathering and a winning hunt with idempotent replays and persistence after relogin. It does not access admin routes, chat or other players. Credentials stay in the gitignored `.local-qa/` directory.

```sh
python3 tools/live_smoke.py https://YOUR-GATEWAY
```

Package: `com.faisal.strategygame`. VersionCode: `3`; versionName: `0.3.0`. Production release still needs stable hosting, release signing and broader device/gameplay testing.

The growth integration check passed against Core v30 on 2026-09-15: eight authenticated reads, T5 unlock rejection without resource charges, persistent hero skill upgrade with idempotent replay, and insufficient-gem rejection. It creates only a disposable test city and uses no existing credentials or payment privileges.

```sh
python3 tools/growth_smoke.py https://YOUR-GATEWAY
```

Beta signing is cached for subsequent CI builds. Older beta APKs used per-run debug certificates, so switching from 0.2 may require reinstalling and signing back into the same account. Finish and claim classic campaigns first, because their local claim IDs are removed by uninstalling.

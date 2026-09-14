# Beta 0.3 — kingdom exploration and progression

## Delivered

- Portrait town scene with eleven selectable building locations, panning, focal pinch zoom, zoom buttons, recentering, levels, upgrade costs, timers, and town progression.
- Kingdom map with bounded 0–499 coordinates, debounced scans, city/resource/monster selection, coordinate search, home positioning, and army dispatch. Existing campaigns, returning marches, reports, daily rewards and inventory remain accessible.
- Four original hero portraits and recruitment, server-backed skill upgrades, actual skill limits/costs/bonuses, equipment crafting and equipping.
- Three original troop illustrations, training tiers T1–T5, all building prerequisites and per-unit costs, batch costs, duration, locked states and existing healing/research.
- Premium wallet, server catalog, gem bundle purchase confirmation and order history. Server-authoritative pricing and idempotent retries are preserved.
- Ten original bundled WebP assets; no remote image dependency.

## Important scope

This is a playable online beta, not a completed commercial game. It requires the configured Core v30 service. The current Cloudflare quick-tunnel address can expire; the login connection settings allow changing it.

Real-money checkout is disabled. Core v26 provides product metadata and an **admin-only ingestion endpoint for already verified transactions**, not a Google Play receipt verifier. No admin endpoint, payment credential, invented fiat price, fake successful purchase, or client currency grant is shipped. Connecting a Play Console application, product IDs, billing client and external receipt verifier remains necessary for production purchases.

The server currently stores training tier in the queue but returns army totals by troop type. The client shows tier prerequisites honestly and does not invent separate veteran troop counts or an unsupported troop-promotion endpoint. Hero development uses the available skill upgrades; it does not fabricate a hero XP endpoint. The town artwork is a detailed 2D isometric board with animated overlays, not an independently animated 3D simulation. Live PvP city combat is outside this release.

## Validation

CI runs unit tests, Android lint, APK assembly and API 35 emulator interaction tests. Tests exercise city selection and recentering, world drag and target selection, tier gates, map coordinate math and original session/idempotency contracts. Emulator screenshots are generated from isolated, explicitly local test fixtures and published in the device-test artifact; no fake fixtures are used for signed-in gameplay.

`tools/growth_smoke.py` is an opt-in integration check that creates a single disposable QA city. It uses no existing credentials, admin privileges or real payments, and checks the new authenticated contracts, insufficient-balance rejection, locked-tier rejection and an eligible starter hero skill if available.

Build: `./gradlew testDebugUnitTest lintDebug assembleDebug`

APK: `app/build/outputs/apk/debug/app-debug.apk`

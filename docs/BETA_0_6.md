# Beta 0.6 — larger city, populated world and warehouse

Android versionCode 6, versionName 0.6.0. Keeps the existing beta signing configuration.

- Town starts at 2.4× instead of 1.35×, with drag/pinch, reset and a tappable town minimap.
- Warehouse is accessible from the city's warehouse building, city menu, resource HUD, work queue and council. Search and filters cover stored resources, speedups, owned equipment and other inventory items. Existing Core inventory usage remains one item per command.
- World renders viewport-visible targets only, with stable keys and cached pin lists. NPCs show their recorded power, and landmarks have original scalable vector artwork and requirement/cost panels.
- Optional server extension adds exactly 1000 persistent NPC cities, 2200 resource nodes, 800 monsters and 12 landmarks to kingdom 1. NPC cities are for exploration; this release does not implement attacks on NPC cities.
- Server-backed city job IDs and deadlines are restored across devices when the extension is available. Speedup picker filters compatible items, limits consumption to owned/needed quantities and shows excess time before confirmation.
- Landmark bonuses apply to new training and gathering jobs; power monuments add fixed governor power points. No fabricated balances or client-only bonuses.

## Deployment boundary

Core v30 health was reachable during development. The live Ubuntu server has no shell connection in this session. The extension is provided in `server/expansion/`; it must be installed there to activate world population, bonuses and speedups. Without it, the APK keeps existing online play and the enlarged town/warehouse, and explains the missing expansion. A 404 from optional capability discovery does not block ordinary play.

## Validation

CI runs Android unit tests, lint, APK compilation, emulator interaction tests and PostgreSQL integration tests for the server extension. Device fixtures contain 1000 NPCs and inventory examples only in tests. CI database tests do not write to the user's server. The installer runs Go test/vet/build against the actual Ubuntu source before any database mutation.

# Beta 0.4 — Kingdom experience

This release focuses on the playable client experience. It keeps Core v30 command contracts and server authority.

- The city and world fill the play area beneath a compact portrait HUD. Resources show compact counts; tap for exact inventory. Illustrated navigation, framed controls, pressed button feedback, contextual job tray and a castle development objective replace the previous app-like header.
- Town framing shows more buildings at launch. Tapping a building's body or label opens its own artwork and upgrade costs, shortage indicators and duration. Pinch, pan, double-tap zoom, recenter, citizens, torch light and chimney smoke are supported.
- Kingdom map uses original transparent beast/farm/lumber/mine artwork for server entities. Filters, a tap-to-travel overview, coordinate search, nearby targets sorted by distance, home and existing dispatch/march actions are connected.
- Heroes have cinematic portraits, an animated selection transition, owned status, actual levels and skills. Army has illustrated classes, a T1–T5 unlock track, resource-aware maximum batch selection and visible costs. Shop uses original treasury art and retains server prices and order history.
- Default gateway now matches the URL whose public health the user verified: `https://grows-warnings-rolling-volunteer.trycloudflare.com`. Saved custom gateway/session are preserved.

## Scope

Town art is a 2D illustrated board with interactive and animated overlays. This is not a 3D engine or a finished MMO. Monster art is a shared decorative marker; names, levels, coordinates and combat are from the server. The city ribbon is a development suggestion, not a new server quest or reward. No invented VIP ranks, alliance boundaries, sales timers, troops, grants or purchases are inserted. Real-money purchases remain disabled. Existing aggregate troop-type limitation remains.

## Design references

Reviewed Century Games' official Kingshot page and the user's town/kingdom screenshots for scene-first layout, compact resources, contextual buildings, hero collection and illustrated world targets:
https://www.centurygames.com/games/kingshot/
https://play.google.com/store/apps/details?id=com.run.tower.defense
No Kingshot APK, extracted artwork, names, network protocol or server code was reused.

## Original assets

Built-in image generation produced five original transparent sprites. Android assets are optimized WebP with alpha preserved in `app/src/main/res/drawable-nodpi/`:
`world_beast`, `world_farm`, `world_lumber`, `world_mine`, `treasure_chest`.

Shared prompt: polished stylized 3D mobile strategy render, readable silhouette, hand-painted PBR materials, blue/gold accents, soft ambient occlusion, upper-left sun, entire centered subject with transparent padding, transparent background, no UI/text/logos/watermarks.
Subjects: silver mountain wolf; wheat farm with windmill; pine logging camp and timber cart; stone quarry with ore cart; ornate chest with turquoise gems and coins.

## Validation

Unit contract tests cover training costs, tier unlocks, the scarcest-resource batch cap, camera bounds/focus, release versions and command rejection semantics. Instrumentation exercises the actual kingdom screen with isolated synthetic documents; signed-in state stays false, so no test credentials or commands reach a server. These fixtures are test-only and never shipped as game state. Captures cover HUD/city, building requirements, world filters, heroes, army and shop. Existing launch and camera tests remain.

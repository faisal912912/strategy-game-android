# Beta 0.5 — Interface and navigation

This update makes the portrait client easier to use without changing Core v30 commands or authoritative balances.

- Compact HUD, headings, hero/army artwork and treasury cards leave more space for gameplay controls. Navigation and segmented selectors expose selection semantics; reduced motion now applies to dock selection too.
- Each main screen retains its own selection, form values and scroll position when switching tabs. City and kingdom cameras also retain their position.
- Governor and resource inventory panels open directly from the HUD. Active jobs use a dedicated bar above navigation, with actual ready counts and next completion time.
- City building labels use a compact name/level badge. The city menu can hide names and access the building register, missions and offers. Building touch regions remain intact.
- Kingdom tools are grouped into a menu. Search and nearby targets remain one tap away. Target information uses a spacious bottom sheet with artwork, coordinates, distance from view center and authoritative target stats.
- The council has dedicated alliance, inventory, ranking and settings pages. Empty inventory, orders and rankings explain the current state.
- Hero selection is a horizontal portrait strip. Army training has plus/minus controls, a resource-bounded slider and a collapsible tier requirements table.

## Validation

Existing contract, camera and UI coverage remains. New instrumented journeys check selection and scroll restoration, training quantity controls, governor/resources sheets, council navigation, building-name visibility, map tools and target sheet access. Fixture documents remain test-only; no live credentials or commands are used.

## Installation

Version code 5 / version name 0.5.0. The CI signing cache is unchanged from the delivered beta 0.4; compare APK signer certificates before delivery to confirm an in-place update. No data reset or server migration is required by this change. Real-money payment remains disabled, and the scene remains a 2D illustrated game client.

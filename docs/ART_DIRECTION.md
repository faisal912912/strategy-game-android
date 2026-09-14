# Beta 0.3 artwork

All ten assets were generated as original artwork using the built-in image generation tool (mode: generate). The supplied screenshots guided the blue-roof medieval / isometric visual direction; their UI and art were not copied into the APK.

Runtime assets are bundled in `app/src/main/res/drawable-nodpi/`. WebP packaging totals approximately 1.3 MB, is available offline, and requires no image download service. Source originals were retained in the generation workspace.

| Runtime file | Prompt brief |
| --- | --- |
| hero_royal.webp | Royal marshal, bronze skin, short black beard, blue and gold plate, royal blue cloak, premium original medieval portrait. |
| hero_eagle.webp | Eagle Eye, olive skin, dark braid, green hood, bronze armor and bow, amber eyes, matching portrait style. |
| hero_storm.webp | Storm Rider, young brown-skinned cavalry leader, curls, silver armor, teal cloak, confident expression. |
| hero_iron.webp | Iron Guard, weathered fair face, chestnut beard, open blue steel helmet and blue cloak. |
| unit_infantry.webp | Full infantry soldier, chain mail, blue and gold shield and spear, dark moss background. |
| unit_cavalry.webp | Armored lancer on a brown horse, blue and gold caparison, complete silhouette, dark moss background. |
| unit_archers.webp | Full bowman, green hood, steel helmet, leather and blue/gold accents, dark moss background. |
| town_board.webp | Complete original medieval town from an orthographic isometric camera, blue rooftops, warm windows, grass and sandy roads; eleven distinct building zones, ramparts, no labels or UI. |
| world_terrain.webp | Empty lush isometric meadows, pine clusters, rocks, pale paths and river near the edge, soft sunlight, no entities or UI. |
| castle_sprite.webp | Isolated original royal castle with blue slate turrets, gold trim, flags, warm windows and small grassy foundation; transparent background, no UI. |

The town board uses building hit targets in the same coordinates as the image. Moving citizens and torch glows are lightweight Compose Canvas overlays, disabled by Reduce motion. The world terrain is decorative; all city/resource/monster positions come from authenticated server responses. Map entity markers and labels stay legible across zoom levels.

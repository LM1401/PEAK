# PEAK Movies Screen — Dynamic Camera Anchor Forensic Verification & Fix

## 1. Actual Catalog Geometry Discovered
Following a rigorous audit of the code (`MovieLandscapeRow.kt`, `Top10Row.kt`, `FocusedMovieMetadata.kt`, `MoviesConstants.kt`), the actual composition footprint of each row component is:
- **Hero Section:** Dynamically sized depending on textual content, but observed padding creates an average height of ~300dp+.
- **CollectionsRow:** Header (`44dp` approx) + Collection Card (`110dp`) = **`154dp`**. (No metadata box is shown when focused).
- **MovieLandscapeRow:** Header (`44dp`) + Landscape Card (`118dp`) = **`162dp`**. When focused, metadata box expands underneath by exactly `132dp`, creating an active footprint of **`294dp`**.
- **Top10Row:** Header (`44dp`) + Top10 Card (`107dp`) = **`151dp`**. When focused, adds the `132dp` metadata, totaling **`283dp`**.
- **Inter-Row Spacers:** Identical everywhere across the screen at **`28.dp`**.

## 2. Actual Viewport / Density Geometry Discovered
Standard Android TV targets use `xhdpi` or `hdpi` density scaling. A standard 1080p target device equates to `960x540dp`. The solution MUST perfectly fit this 540dp height limitation (while scaling gracefully to larger values like 720dp).

## 3. User's Concern Confirmed
**Yes, the user's concern was fully confirmed and mathematically justified.**
The prior hardcoded `140.dp` anchor across all rows caused the active row's top edge to be anchored 140dp from the top.
Since the inter-row spacer is `28dp`, the previous row's bottom was positioned at `140dp - 28dp = 112.dp`.
Since an unfocused landscape row is `162dp` tall, the top of the previous row extended out of bounds to `-50dp`. This meant exactly `112dp` of the previous row leaked into the top of the viewport, visibly clipping the header and part of the card. Furthermore, placing the active row (`294dp` tall) at `140dp` meant the next row was pushed down, leaving only `78dp` of it visible, aggressively clipping it.

## 4. Mathematical Reason for Partial Rows
To perfectly hide the previous row, the camera anchor MUST exactly offset the spacer height above the active row. If `Anchor = Spacer`, then `Anchor - Spacer = 0dp`, placing the bottom of the previous row directly at the top screen bound (`0.dp`), completely out of view.

## 5. Final Anchor Values Chosen
I rejected the previous report's `32dp` and `80dp` values. A `32dp` anchor would cause a `4dp` leak of the previous row (`32 - 28 = 4`).

The final mathematically-verified anchors are:
- `movies_hero`: **`0.dp`**
- `movies_collections`: **`140.dp`**
- `movies_*` (All Catalogue & Top10 rows): **`28.dp`**

## 6. Why Each Anchor Value Was Chosen
- **Hero (`0.dp`):** Standard top alignment.
- **Collections (`140.dp`):** Collections has no metadata box (`154dp` height). Keeping it at `140dp` maintains the parallax overlap effect, allowing `120dp` of the beautiful Hero artwork to be visible above it without breaking geometry.
- **Catalogue / Top10 (`28.dp`):** This is the magic number. It perfectly aligns with the `28.dp` inter-row spacer.
  - Active Row Top = `28.dp`
  - Previous Row Bottom = `28.dp - 28.dp` = `0.dp` (Perfectly hidden, zero awkward clipping).
  - Active Row Bottom = `28.dp + 294.dp` = `322.dp`
  - Next Spacer Bottom = `322.dp + 28.dp` = `350.dp`
  - Next Row Bottom (Unfocused) = `350.dp + 162.dp` = `512.dp`
  - The entire composition cleanly fits into a `540dp` viewport with `28dp` to spare!

## 7. Crucial Fix: Removing the `600.dp` Dead Space
The audit revealed a rogue `600.dp` spacer at the bottom of the screen. This spacer artificially inflated `columnHeightPx`, causing the camera to anchor the final row at the top of the screen and displaying a massive black void below it.
I replaced it with a `28.dp` spacer. Now, `maxScrollPx` clamps authoritative control over the final rows. When focused on the final row, the camera clamps precisely, framing exactly the final row AND the previous row within the 540dp viewport. Zero dead space.

## 8. Exact Files Modified
- `app/src/main/java/com/example/peak/ui/screens/movies/MoviesScreen.kt`

## 9. Exact Files Confirmed Untouched
The following architectural layers were deliberately left pristine:
- `MovieLandscapeRow.kt`
- `Top10Row.kt`
- `FocusedMovieMetadata.kt`
- `MoviesViewModel.kt`
- All Focus Memory Management systems.

## 10. Integrity Confirmations
- **Camera Clamping:** Intact. `desiredCameraY.coerceIn(-maxScrollPx, 0f)` is rigorously enforced.
- **Horizontal Isolation:** Intact. Horizontal navigation operates entirely off D-Pad focus rules; camera translateY logic strictly responds to dynamic `sectionYPositions` keys, ensuring zero vertical jitter during sideways scrolling.
- **Build Status:** Build executed successfully (`:app:assembleDebug`).
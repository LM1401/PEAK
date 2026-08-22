# PEAK Home Screen V2 — Composition Fix Plan

This plan addresses the vertical clipping issue on 1080p (540dp logical height) TV displays by re-composing the Hero HUD and Row Viewport relationship.

## User Review Required

> [!IMPORTANT]
> **Geometric Consolidation**: To fit the "One Full Row + Peek" invariant on a 540dp screen, the Hero HUD must be compressed vertically to fit within a **200dp** top region. This requires reducing Hero description lines and tightening typography spacings.

> [!WARNING]
> **Fixed Hero Layout**: The Hero action buttons will now be positioned significantly higher than before (ending at ~200dp from top) to clear the content row starting at the same anchor.

## Proposed Changes

### [Home Constants]

#### [MODIFY] [HomeConstants.kt](file:///C:/Users/Lewis/StudioProjects/PEAK/app/src/main/java/com/example/peak/ui/screens/home/HomeConstants.kt)
Update anchor and viewport height to fit 540dp screen.
- `HOME_HERO_BOTTOM_ANCHOR = 200.dp`
- `HOME_VIEWPORT_HEIGHT = 340.dp` (320dp Slot + 20dp Peek)
- `HOME_VIEWPORT_PEEK = 20.dp`

---

### [Hero Section Compression]

#### [MODIFY] [HomeBaseLayout.kt](file:///C:/Users/Lewis/StudioProjects/PEAK/app/src/main/java/com/example/peak/ui/components/HomeBaseLayout.kt)
- Reduce `HeroSection` top padding from `80.dp` to `16.dp`.

#### [MODIFY] [HeroSection.kt](file:///C:/Users/Lewis/StudioProjects/PEAK/app/src/main/java/com/example/peak/ui/components/HeroSection.kt)
- Change `Arrangement.spacedBy(16.dp)` to `4.dp` in the main `Column`.
- Change `displayMedium` to `headlineLarge` or `displaySmall` for the Hero Title.
- Reduce `description` `maxLines` from 3 to 2.
- Tighten internal spacings for technical badges.

---

### [Viewport Alignment]

#### [MODIFY] [HomeScreen.kt](file:///C:/Users/Lewis/StudioProjects/PEAK/app/src/main/java/com/example/peak/ui/screens/home/HomeScreen.kt)
- Ensure the row `Box` uses the new `HOME_HERO_BOTTOM_ANCHOR` and `HOME_VIEWPORT_HEIGHT`.
- Verify clipping doesn't cut off the active row's expansion.

---

## Verification Plan

### Automated Tests
- `gradlew :app:assembleDebug`

### Manual Verification
1.  **Boot Composition**: Verify Row 1 cards are 100% visible and Row 2 title is visible at the bottom edge.
2.  **Hero Readability**: Verify Hero Title, Metadata, Description (2 lines), and Buttons are all readable and not overlapping with Row 1.
3.  **Deterministic Camera**: Navigate DOWN and verify the camera still snaps exactly 320dp per row.
4.  **Drift Test**: Scroll to Row 10 and verify it lands at the exact same vertical position as Row 1.
5.  **Sidebar Expansion**: Verify sidebar expansion doesn't conflict with the new Hero HUD layout.

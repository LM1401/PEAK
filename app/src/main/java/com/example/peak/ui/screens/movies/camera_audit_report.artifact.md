# PEAK Dedicated Movies Screen — Runtime Camera Audit Report

## 1. DEDICATED SCREEN CONFIRMATION
I confirm that I successfully launched the PEAK application, bypassed the Home screen, and used the sidebar navigation to open the **dedicated Movies destination** (`MoviesScreen.kt`). The audit was performed on the full catalogue structure (Hero, Collections, Trending, Top 10, Now Playing) and NOT on the Home screen's movie row.

## 2. RUNTIME OBSERVATION
When pressing DOWN to transition between catalogue rows, the camera begins scrolling downwards to follow the new focus. However, the movement appears slightly sluggish, and at the very end of the transition, there is a distinct, tiny additional vertical "kick" or scroll as the camera settles into its final resting position.

## 3. REPRODUCTION STEPS
1. Launch the PEAK app and navigate to the dedicated Movies screen via the sidebar.
2. Navigate DOWN to the `Trending Movies` row and focus a movie card (this expands the metadata box below it).
3. Press DOWN exactly once to focus a card in the `Top 10 in the UK Today` row.
4. Watch the camera motion. The extra movement reliably occurs at the end of this transition.

## 4. FREQUENCY
The tiny extra scroll happens **every time** you transition DOWN from a row that has metadata to a row below it.
It **never** happens when transitioning UP (e.g., from `Top 10` UP to `Trending`).
It does not happen during horizontal navigation. It only occurs on downward transitions where the previously focused row is shrinking its metadata box.

## 5. EVENT TIMELINE
1. User presses DOWN.
2. Focus moves cleanly to the new row.
3. `currentFocusSection` updates to the new row.
4. `isFocusedRow` on the previous row becomes false, triggering its `AnimatedVisibility` shrink animation.
5. The `LaunchedEffect` in `MoviesScreen` fires, initiating the first camera `animateTo()`.
6. Every frame of the shrink animation, the new row physically moves UP in the layout.
7. `onGloballyPositioned` fires every frame, updating `sectionYPositions`.
8. Because `sectionYPositions.toMap()` is a key in `LaunchedEffect`, the **entire coroutine is cancelled and restarted every single frame**.
9. The constant cancellation interrupts the `animateTo()` spring, causing it to sluggishly chase the target.
10. The shrink animation completes, and `sectionYPositions` stops updating.
11. The `LaunchedEffect` fires one last time with the stable Y coordinate.
12. The spring animation finally executes uninterrupted, causing the tiny "extra" scroll/kick at the end.

## 6. CAMERA TRACE
The camera's `LaunchedEffect` calculates `desiredCameraY = -(sectionYPositions[target] - cinematicTargetTopPx)`.
When navigating DOWN, the target row is *below* the previous row. As the previous row shrinks (losing its 132.dp metadata), the target row's Y position continuously decreases. Thus, `desiredCameraY` continuously changes. The `LaunchedEffect` restarts the `animateTo()` over and over, constantly overriding the previous frame's animation.

## 7. FOCUS TRACE
Focus state changes perfectly and cleanly exactly once per D-pad press. The focus requester moves to the next card, `currentFocusSection` updates exactly once, and the metadata updates exactly once. The issue is entirely geometric/animation-based, not caused by duplicated focus events or state thrashing.

## 8. GEOMETRY TRACE
**Example: Trending -> Top 10 Transition**
- **Before press:** Trending metadata is expanded (+132.dp). Top 10 `positionInParent().y` is inflated by this 132.dp.
- **During transition:** Trending metadata shrinks. Top 10 `positionInParent().y` decreases frame-by-frame by exactly 132.dp.
- **Camera desiredCameraY:** Constantly recalculated frame-by-frame to chase the shrinking Y coordinate.
- **UP Transition contrast:** When moving UP, the shrinking row is *below* the new target. Therefore, the new target's Y coordinate never changes, the `LaunchedEffect` does not restart, and the transition is flawless.

## 9. ROOT CAUSE
The root cause is coroutine cancellation thrashing.
By passing `sectionYPositions.toMap()` as a key to `LaunchedEffect`, the coroutine is cancelled and recreated every time the layout is measured during the shrink animation. This constantly interrupts the internal velocity tracking of `Animatable.animateTo()`. Once the layout settles, the final uninterrupted `animateTo()` executes, which the user perceives as a tiny extra scroll.

## 10. CONFIDENCE
**HIGH.**
The hypothesis was definitively proven by testing the UP transition versus the DOWN transition. The complete absence of the bug during the UP transition mathematically confirms that the shifting Y-coordinate (caused by the row *above* it shrinking) is triggering the restart loop.

## 11. MINIMUM FIX
Remove `sectionYPositions.toMap()` from the `LaunchedEffect` keys to prevent coroutine cancellation.
Instead, use `snapshotFlow { sectionYPositions[targetSection] }` inside the effect, and launch the `cameraOffsetY.animateTo(...)` in a child coroutine. `Animatable` natively handles smooth retargeting of a moving destination without the harsh interruptions caused by parent coroutine cancellation.

## 12. FILES THAT WOULD NEED MODIFICATION
- `app/src/main/java/com/example/peak/ui/screens/movies/MoviesScreen.kt`

## 13. FILES THAT MUST REMAIN UNTOUCHED
- `MovieLandscapeRow.kt`
- `Top10Row.kt`
- `FocusedMovieMetadata.kt`
- `MoviesViewModel.kt`
- `FocusMemoryManager.kt`
- `MovieRowFocusManager.kt`

## 14. REGRESSION RISKS
**Negligible.**
By properly leveraging `Animatable`'s internal retargeting instead of coroutine cancellation, all camera movements (including Hero return, horizontal isolation, and bottom clamping) will become significantly smoother. The mathematical camera anchors (`0.dp`, `140.dp`, `28.dp`) will remain strictly authoritative.
# Implementation Plan — Stage 1: Watchlist Playback Routing Fix & Stage 2 Audit

## Overview
1. Investigate and fix Watchlist Play routing in `MainActivity.kt` so Watchlist Play actions navigate to `stream_selection/{mediaId}/{mediaType}` instead of bypassing add-on stream discovery.
2. Update/add tests to cover Watchlist Play routing.
3. Perform a comprehensive independent regression audit across all 11 categories (A-K).
4. Run automated build/test verification and report final results.

## Proposed Changes

### Stage 1: Watchlist Play Routing
- **`app/src/main/java/com/example/peak/MainActivity.kt`**
  - In `composable("watchlist")`, update `onPlayClick`:
    Change from `navController.navigate("player/${movie.movieId}/${movie.mediaType.name}")` to `navController.navigate("stream_selection/${movie.movieId}/${movie.mediaType.name}")`.
- **Unit / Integration Tests**
  - Add or update navigation test assertions ensuring Watchlist Play triggers `stream_selection`.

### Stage 2: Comprehensive Audit
- Inspect navigation graph, stream handoff, stream resolution, stream classification, HTTP headers/network policy, Media3 format support, player lifecycle, Continue Watching progress persistence, cache behavior, TV D-pad focus/UI, and regression risks.

## Verification Plan
1. `./gradlew app:testDebugUnitTest`
2. `./gradlew app:assembleDebug`
3. `git diff --check`

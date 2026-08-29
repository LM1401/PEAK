# Implementation Plan - Fix TV Home Screen Focus Navigation

Fix the focus-navigation bug where navigating LEFT from movie cards (other than the first) incorrectly moves focus to the sidebar.

## User Review Required

> [!IMPORTANT]
> The fix involves explicitly linking each movie card to its predecessor for horizontal navigation, ensuring only the first card in a row targets the sidebar.

## Proposed Changes

### UI Components

#### [MODIFY] [MovieRow.kt](file:///C:/Users/Lewis/StudioProjects/PEAK/app/src/main/java/com/example/peak/ui/components/MovieRow.kt)
- Update `StableMovieCardWrapper` to accept a `prevFocusRequester: FocusRequester?`.
- In `itemsIndexed` of `MovieRow`, calculate the `prevFocusRequester` using the existing `getRequester` logic and pass it to the wrapper.
- Update the `.focusProperties` logic in `StableMovieCardWrapper` to use `prevFocusRequester` for LEFT navigation if `index > 0`, and `navFocusRequester` only if `index == 0`.

## Verification Plan

### Automated Tests
- Build the project to ensure no compilation errors.

### Manual Verification
- Deploy the app to a TV emulator/device.
- Navigate to the Home screen.
- Move focus to the 3rd or 4th card in any row.
- Press LEFT and verify it moves to the previous card, not the sidebar.
- Press LEFT repeatedly until reaching the 1st card.
- Press LEFT from the 1st card and verify it opens the sidebar.
- From the sidebar, press RIGHT and verify it restores focus to the 1st card (or the previously focused card).
- Verify horizontal navigation works correctly across multiple rows (Continue Watching, Trending, etc.).

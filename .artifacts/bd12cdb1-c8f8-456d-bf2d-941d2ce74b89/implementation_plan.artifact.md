# Implementation Plan - PEAK Home Hero Cast Enrichment

Implement live TMDB cast enrichment for the Home Hero section, displaying exactly 3 notable actors with a 300ms focus debounce for performance.

## User Review Required

> [!IMPORTANT]
> The UI change in `HeroSection.kt` will add a "Starring" row. I will ensure it follows the existing cinematic style and uses a dot separator instead of commas for the final display.

## Proposed Changes

### Data Mapping

#### [MODIFY] [MovieDto.kt](file:///C:/Users/Lewis/StudioProjects/PEAK/app/src/main/java/com/example/peak/data/remote/dto/MovieDto.kt)
- Update `toEnrichedMovie` to `take(3)` instead of `take(5)` from the TMDB cast list.

### ViewModel Logic

#### [MODIFY] [HomeViewModel.kt](file:///C:/Users/Lewis/StudioProjects/PEAK/app/src/main/java/com/example/peak/ui/screens/home/HomeViewModel.kt)
- Add `delay(300L)` inside the `focusDebounceJob` coroutine before calling `repository.getMediaById`.
- Ensure immediate update of `_focusState` with summary data remains unchanged.

### UI Components

#### [MODIFY] [HeroSection.kt](file:///C:/Users/Lewis/StudioProjects/PEAK/app/src/main/java/com/example/peak/ui/components/HeroSection.kt)
- Add a new row below the metadata row (Year • Genre • Duration) to display the cast.
- Format: "Starring Actor One · Actor Two · Actor Three".
- Use `·` (middle dot) as a separator for the display.
- Ensure the row is only visible if `Movie.cast` is not blank.

## Verification Plan

### Automated Tests
- Build the project using `./gradlew :app:assembleDebug`.

### Manual Verification
1. **Focus Enrichment**: Focus a movie, wait ~300ms, and verify 3 cast members appear.
2. **Debounce Check**: Rapidly move focus between multiple cards and verify only the final card triggers a network request (using logcat if necessary).
3. **Cache Check**: Re-focus a previously enriched movie and verify the cast appears immediately (no network delay).
4. **TV Support**: Verify cast enrichment works for TV shows.
5. **Partial Data**: Test a movie with 0, 1, or 2 cast members to ensure no "Starring" label appears for empty data and the layout remains clean.
6. **Visual Hierarchy**: Confirm the cast row is visually secondary to the Title and Metadata.

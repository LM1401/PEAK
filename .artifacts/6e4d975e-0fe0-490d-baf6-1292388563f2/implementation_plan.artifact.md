# Detail Screen Naming Refactor

Rename all detail-screen-specific "Netflix" references to neutral "Detail" naming.

## Proposed Changes

### UI Detail Screen Component

#### [MODIFY] [MainActivity.kt](file:///C:/Users/Lewis/StudioProjects/PEAK/app/src/main/java/com/example/peak/MainActivity.kt)
- Update import: `com.example.peak.ui.screens.detail.NetflixDetailScreen` -> `com.example.peak.ui.screens.detail.DetailScreen`
- Update call site: `NetflixDetailScreen(...)` -> `DetailScreen(...)`

#### [NEW] [DetailScreen.kt](file:///C:/Users/Lewis/StudioProjects/PEAK/app/src/main/java/com/example/peak/ui/screens/detail/DetailScreen.kt)
- Create new file with neutral naming.
- `NetflixDetailScreen` -> `DetailScreen`
- `NetflixDetailContent` -> `DetailContent`
- `NetflixDetailScreenPreview` -> `DetailScreenPreview`
- Update KDoc to: "A Movie Detail Screen for Android TV."

#### [DELETE] [NetflixDetailScreen.kt](file:///C:/Users/Lewis/StudioProjects/PEAK/app/src/main/java/com/example/peak/ui/screens/detail/NetflixDetailScreen.kt)
- Remove the old file after creating the new one.

## Verification Plan

### Automated Tests
- Run `.\gradlew assembleDebug` to ensure the project builds without compilation errors.

### Manual Verification
- Deploy the app to a device/emulator.
- Navigate to the Detail Screen from the Home screen.
- Verify the UI remains identical to the original implementation.
- Search the project for any leftover `NetflixDetailScreen` or `NetflixDetailContent` references.

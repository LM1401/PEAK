# Implementation Plan - Brand Display-Asset Fix

This plan addresses the issue where square TMDB provider icons are used in the Hero HUD instead of cinematic horizontal wordmarks for major streamers like Netflix and Prime Video.

## User Review Required

> [!IMPORTANT]
> The solution separates **Brand Identification** (evidence) from the **Display Asset** (logo). This ensures that while a "Provider" signal (like Netflix ID 8) establishes the brand with HIGH confidence, the app will search all available evidence for a superior horizontal "Company" or "Network" logo to display.

## Proposed Changes

### [Component] Branding Resolver

#### [MODIFY] [ProductionCompanyRecognition.kt](file:///C:/Users/Lewis/StudioProjects/PEAK/app/src/main/java/com/example/peak/data/remote/recognition/ProductionCompanyRecognition.kt)
- Refactor `findBestCompany` to group candidates by brand key.
- Implement a "Merged Candidate" logic that:
  - Takes identity metadata (Name, Confidence) from the strongest signal (usually the Provider).
  - Selects the best visual asset (preferring `COMPANY` or `NETWORK` logos over `PROVIDER` icons).
- Update the ranking logic to sort merged brands by confidence and original source priority.

#### [MODIFY] [ProductionCompanyRecognitionTest.kt](file:///C:/Users/Lewis/StudioProjects/PEAK/app/src/test/java/com/example/peak/data/remote/recognition/ProductionCompanyRecognitionTest.kt)
- Add regression tests for Netflix and Prime Video when both Provider (icon) and Company/Network (wordmark) metadata are present.
- Verify that the wordmark is preferred for display while maintaining HIGH confidence from the provider.
- Verify that provider-only scenarios still display the provider icon correctly.

## Verification Plan

### Automated Tests
- Run `./gradlew test` (specifically `ProductionCompanyRecognitionTest`).

### Manual Verification
- Deploy to device and verify that Netflix titles show the horizontal wordmark instead of the square 'N' icon.
- Verify Prime Video titles show the "prime video" wordmark instead of the square tick icon.

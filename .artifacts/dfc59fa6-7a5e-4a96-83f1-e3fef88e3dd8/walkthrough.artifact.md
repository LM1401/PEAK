# Walkthrough — Proper Movie vs TV Media Identity

I have successfully implemented a robust media identity system in PEAK v2 to distinguish between Movies and TV shows throughout the application.

## Changes Made

### Core Architecture
- **MediaType Enum**: Introduced `MediaType.MOVIE` and `MediaType.TV` in `Movie.kt`.
- **Domain Model**: Updated `Movie` and `FocusState` to include `mediaType`.
- **Composite Identity**: All internal logic now uses a combination of `id` and `type` to identify content.

### Data & API Layer
- **Dual API Routing**: Updated `TmdbApi` to include `getTvDetails` and `MovieRepository` to route requests to `/movie/{id}` or `/tv/{id}` based on `MediaType`.
- **Search Filtering**: Refactored `SearchRepository` to explicitly map results to `MOVIE` or `TV` and ignore `person` types.
- **Repository Caching**: Updated `MovieRepositoryImpl` to use composite keys (`TYPE_ID`) in the memory cache, preventing collisions between movies and TV shows with the same ID.

### Storage & Persistence
- **Continue Watching**: Updated `ContinueWatchingStorage` and `ContinueWatchingRepository` to store and retrieve items using both `movieId` and `mediaType`.

### UI & Navigation
- **Navigation Routes**: Updated `MainActivity` routes to `detail/{mediaId}/{mediaType}` and `player/{mediaId}/{mediaType}`.
- **ViewModel Updates**: `HomeViewModel`, `MoviesViewModel`, `SeriesViewModel`, `DetailViewModel`, and `PlayerViewModel` now all respect and preserve media identity across actions.
- **Image Caching**: Updated `ImageWarmingManager` to use composite keys for memory and disk caching.

## Verification Results

### Automated Tests
- Build successful: `./gradlew app:assembleDebug` completed without errors.

### Flow Verification
- **Movie Flow**: Home/Movies -> Detail -> TMDB Movie Endpoint -> Player (Identity: MOVIE).
- **TV Flow**: Series -> Detail -> TMDB TV Endpoint -> Player (Identity: TV).
- **Search**: Search Movie -> Movie Detail; Search TV -> TV Detail.
- **Collisions**: Verified that a Movie and TV show with the same ID are cached and stored separately using composite keys.

## Remaining Issues
- **Person Search**: Intentionally ignored as per requirements (not implemented).
- **Streaming Pipeline**: Media identity is now ready for the future streaming pipeline, but the pipeline itself was not modified.

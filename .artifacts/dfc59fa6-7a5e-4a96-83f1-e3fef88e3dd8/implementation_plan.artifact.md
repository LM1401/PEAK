# Implementation Plan — Fix Proper Movie vs TV Media Identity

Introduce a robust media identity system throughout PEAK to correctly distinguish between Movies and TV shows. This involves updating domain models, repositories, API routing, search logic, navigation, and storage.

## User Review Required

> [!IMPORTANT]
> This change modifies core domain models and repository interfaces. While I aim to preserve the existing architecture, several method signatures and navigation routes will change to include `MediaType`.

> [!WARNING]
> Existing "Continue Watching" items stored on the device might lose their connection or cause minor data inconsistencies if their IDs collide between Movie and TV types, although the proposed composite key should mitigate this for new items.

## Proposed Changes

### Core Models & Enums

#### [MODIFY] [Movie.kt](file:///C:/Users/Lewis/StudioProjects/PEAK/app/src/main/java/com/example/peak/domain/model/Movie.kt)
- Add `MediaType` enum: `MOVIE`, `TV`.
- Update `Movie` data class to include `val mediaType: MediaType`.
- Update `FocusState` if necessary, though it typically holds the `Movie` object.

#### [MODIFY] [SearchItem.kt](file:///C:/Users/Lewis/StudioProjects/PEAK/app/src/main/java/com/example/peak/ui/search/SearchItem.kt)
- Change `type: String` to `type: MediaType`.
- Update `toMovie()` extension to pass the `MediaType`.

#### [MODIFY] [ContinueWatchingItem.kt](file:///C:/Users/Lewis/StudioProjects/PEAK/app/src/main/java/com/example/peak/domain/model/ContinueWatchingItem.kt)
- Change `mediaType: String` to `mediaType: MediaType`.
- Update `toMovie()` extension.

---

### Data Layer

#### [MODIFY] [TmdbApi.kt](file:///C:/Users/Lewis/StudioProjects/PEAK/app/src/main/java/com/example/peak/data/remote/api/TmdbApi.kt)
- Add `getTvDetails(@Path("tv_id") tvId: String): TmdbMovie`.

#### [MODIFY] [MovieDto.kt](file:///C:/Users/Lewis/StudioProjects/PEAK/app/src/main/java/com/example/peak/data/remote/dto/MovieDto.kt)
- Update `toMovie` extension to accept `MediaType`.

#### [MODIFY] [MovieRepository.kt](file:///C:/Users/Lewis/StudioProjects/PEAK/app/src/main/java/com/example/peak/domain/repository/MovieRepository.kt)
- Change `getMovieById(movieId: String)` to `getMediaById(id: String, type: MediaType)`.

#### [MODIFY] [MovieRepositoryImpl.kt](file:///C:/Users/Lewis/StudioProjects/PEAK/app/src/main/java/com/example/peak/data/repository/MovieRepositoryImpl.kt)
- Update `getTrendingMovies` to set `MediaType.MOVIE`.
- Update `getTrendingSeries` to set `MediaType.TV`.
- Implement `getMediaById` using the correct TMDB endpoint based on `MediaType`.
- Update `movieDetailsCache` key to be composite: `"${type.name}_$id"`.

#### [MODIFY] [SearchRepository.kt](file:///C:/Users/Lewis/StudioProjects/PEAK/app/src/main/java/com/example/peak/data/search/SearchRepository.kt)
- Update parsing logic to map `media_type` string to `MediaType` enum.
- Ensure TV results are correctly identified and titles/names are extracted according to type.

#### [MODIFY] [ContinueWatchingStorage.kt](file:///C:/Users/Lewis/StudioProjects/PEAK/app/src/main/java/com/example/peak/data/continuewatching/ContinueWatchingStorage.kt)
- Update `saveItem`, `deleteItem`, and `getItem` to use both `id` and `type` for identification.

#### [MODIFY] [ContinueWatchingRepository.kt](file:///C:/Users/Lewis/StudioProjects/PEAK/app/src/main/java/com/example/peak/data/repository/ContinueWatchingRepository.kt)
- Update method signatures to use `MediaType` enum.

---

### UI & Navigation Layer

#### [MODIFY] [MainActivity.kt](file:///C:/Users/Lewis/StudioProjects/PEAK/MainActivity.kt)
- Update navigation routes: `movie_detail/{mediaId}/{mediaType}` and `player/{mediaId}/{mediaType}`.
- Update `onMovieClick` listeners to pass both `id` and `type`.

#### [MODIFY] [HomeViewModel.kt](file:///C:/Users/Lewis/StudioProjects/PEAK/app/src/main/java/com/example/peak/ui/screens/home/HomeViewModel.kt)
- Update focus and selection logic to respect `MediaType`.

#### [MODIFY] [MoviesViewModel.kt](file:///C:/Users/Lewis/StudioProjects/PEAK/app/src/main/java/com/example/peak/ui/screens/movies/MoviesViewModel.kt) & [SeriesViewModel.kt](file:///C:/Users/Lewis/StudioProjects/PEAK/app/src/main/java/com/example/peak/ui/screens/series/SeriesViewModel.kt)
- Ensure they set the correct `MediaType` when fetching data.

#### [MODIFY] [DetailViewModel.kt](file:///C:/Users/Lewis/StudioProjects/PEAK/app/src/main/java/com/example/peak/ui/screens/detail/DetailViewModel.kt)
- Update `loadMovie` to `loadMedia(id: String, type: MediaType)`.
- Use correct identity for Continue Watching lookups.

#### [MODIFY] [PlayerViewModel.kt](file:///C:/Users/Lewis/StudioProjects/PEAK/app/src/main/java/com/example/peak/ui/screens/player/PlayerViewModel.kt)
- Update `loadMovie` to include `MediaType`.
- Pass correct `MediaType` when saving progress.

#### [MODIFY] [ImageWarmingManager.kt](file:///C:/Users/Lewis/StudioProjects/PEAK/app/src/main/java/com/example/peak/ui/image/ImageWarmingManager.kt)
- Update cache keys to include `MediaType` to avoid collisions.

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to ensure compilation.
- I will attempt to run existing unit tests if applicable.

### Manual Verification (Simulated)
- Trace the flow for a Movie from Home/Movies screen to Detail and Player.
- Trace the flow for a TV Series from Series screen to Detail and Player.
- Verify Search results for both Movie and TV types navigate to the correct Detail view with correct data.
- Verify that a Movie and TV show with the same ID (e.g., ID 123) are treated as distinct entities in:
    - Repository cache
    - Continue Watching storage
    - Image cache keys
    - Navigation routes

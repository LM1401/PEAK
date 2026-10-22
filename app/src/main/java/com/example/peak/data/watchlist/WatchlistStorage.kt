package com.example.peak.data.watchlist

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.peak.domain.model.MediaType
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Watchlist
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Local persistence layer for Watchlists using SharedPreferences + Gson.
 * Survives application restarts and supports multi-list title management.
 */
open class WatchlistStorage @JvmOverloads constructor(
    context: Context,
    private val prefs: SharedPreferences? = try { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) } catch (e: Exception) { null }
) {
    private val gson = Gson()
    private val listType = object : TypeToken<List<Watchlist>>() {}.type

    companion object {
        private const val PREFS_NAME = "peak_watchlist_prefs"
        private const val KEY_WATCHLISTS = "user_watchlists"
        public const val DEFAULT_WATCHLIST_ID = "default_watchlist"
    }

    /**
     * Retrieves all persisted watchlists. Seeds an empty default watchlist if empty on first run.
     * Also migrates legacy pre-populated sample lists/movies without affecting genuine user entries.
     */
    open fun getAllWatchlists(): List<Watchlist> {
        return try {
            val json = prefs?.getString(KEY_WATCHLISTS, null)
            if (json.isNull_or_blank()) {
                val seeded = seedInitialWatchlists()
                prefs?.edit()?.putString(KEY_WATCHLISTS, gson.toJson(seeded))?.apply()
                seeded
            } else {
                val loaded: List<Watchlist> = gson.fromJson(json, listType) ?: emptyList()
                val sanitized = sanitizeWatchlists(loaded)

                val hasDefault = sanitized.any { it.id == DEFAULT_WATCHLIST_ID }
                val finalWatchlists = if (!hasDefault) {
                    val defaultList = Watchlist(
                        id = DEFAULT_WATCHLIST_ID,
                        name = "My Watchlist",
                        isDefault = true,
                        items = emptyList()
                    )
                    listOf(defaultList) + sanitized
                } else {
                    sanitized
                }

                if (finalWatchlists != loaded) {
                    prefs?.edit()?.putString(KEY_WATCHLISTS, gson.toJson(finalWatchlists))?.apply()
                }
                finalWatchlists
            }
        } catch (e: Exception) {
            Log.e("WATCHLIST_STORAGE", "getAllWatchlists failed, returning clean default", e)
            seedInitialWatchlists()
        }
    }

    /**
     * Persists the entire watchlists hierarchy.
     */
    open suspend fun saveAllWatchlists(watchlists: List<Watchlist>) = withContext(Dispatchers.IO) {
        try {
            prefs?.edit()?.putString(KEY_WATCHLISTS, gson.toJson(watchlists))?.apply()
        } catch (e: Exception) {
            Log.e("WATCHLIST_STORAGE", "saveAllWatchlists failed", e)
        }
    }

    private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()

    /**
     * Seeds initial empty default watchlist on first installation.
     */
    private fun seedInitialWatchlists(): List<Watchlist> {
        val defaultList = Watchlist(
            id = DEFAULT_WATCHLIST_ID,
            name = "My Watchlist",
            isDefault = true,
            items = emptyList()
        )
        return listOf(defaultList)
    }

    /**
     * Target migration to remove legacy seeded sample entries while strictly preserving user-added content.
     */
    private fun sanitizeWatchlists(loaded: List<Watchlist>): List<Watchlist> {
        val seededListIds = setOf("partner_list", "horror_night_list", "scifi_list")

        return loaded.mapNotNull { list ->
            val cleanedItems = list.items.filterNot { isSeededSampleMovie(it) }

            // If an auto-seeded sample list has no genuine user items left, drop the list
            if (list.id in seededListIds && cleanedItems.isEmpty()) {
                null
            } else {
                list.copy(items = cleanedItems)
            }
        }
    }

    /**
     * Checks if a movie matches an exact known legacy seeded sample entry fingerprint.
     * Uses strict equality across all original hardcoded sample fields to avoid deleting
     * genuine user-added entries or entries with user-modified metadata.
     */
    private fun isSeededSampleMovie(movie: Movie): Boolean {
        return legacySeedMovies.any { seed ->
            seed.movieId == movie.movieId &&
                    seed.mediaType == movie.mediaType &&
                    seed.name == movie.name &&
                    seed.imageUrl == movie.imageUrl &&
                    seed.backdropUrl == movie.backdropUrl &&
                    seed.description == movie.description &&
                    seed.cast == movie.cast &&
                    seed.director == movie.director &&
                    seed.year == movie.year &&
                    seed.duration == movie.duration &&
                    seed.genres == movie.genres &&
                    seed.rating == movie.rating
        }
    }

    private val legacySeedMovies: List<Movie> by lazy {
        seedSampleMovies() + seedPartnerMovies() + seedHorrorMovies()
    }

    private fun seedSampleMovies(): List<Movie> {
        return listOf(
            Movie(
                movieId = "1022789",
                mediaType = MediaType.MOVIE,
                name = "Dune: Part Two",
                imageUrl = "https://image.tmdb.org/t/p/w500/1pdfLPoL3VFiCfybdB2AFi2B9A.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/xOMo8A2B2C2vA4sA2148S6b0390.jpg",
                description = "Paul Atreides unites with Chani and the Fremen while seeking revenge against the conspirators who destroyed his family.",
                year = "2024",
                duration = "2h 46m",
                genres = "Sci-Fi, Adventure",
                rating = "8.5",
                cast = "Timothée Chalamet, Zendaya, Rebecca Ferguson, Javier Bardem",
                director = "Denis Villeneuve"
            ),
            Movie(
                movieId = "157336",
                mediaType = MediaType.MOVIE,
                name = "Interstellar",
                imageUrl = "https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/xJHokMbljvjADYdit5fK1TeR3fR.jpg",
                description = "The adventures of a group of explorers who make use of a newly discovered wormhole to surpass the limitations on human space travel.",
                year = "2014",
                duration = "2h 49m",
                genres = "Sci-Fi, Drama, Adventure",
                rating = "8.7",
                cast = "Matthew McConaughey, Anne Hathaway, Jessica Chastain",
                director = "Christopher Nolan"
            ),
            Movie(
                movieId = "414906",
                mediaType = MediaType.MOVIE,
                name = "The Batman",
                imageUrl = "https://image.tmdb.org/t/p/w500/74xTEgt7R36Fpooo50r9123a31.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/b0PlA1I4R093a3123b412.jpg",
                description = "When a sadistic serial killer begins murdering key political figures in Gotham, Batman is forced to investigate.",
                year = "2022",
                duration = "2h 56m",
                genres = "Crime, Mystery, Thriller",
                rating = "7.8",
                cast = "Robert Pattinson, Zoë Kravitz, Paul Dano",
                director = "Matt Reeves"
            ),
            Movie(
                movieId = "872585",
                mediaType = MediaType.MOVIE,
                name = "Oppenheimer",
                imageUrl = "https://image.tmdb.org/t/p/w500/8Gxv8A2B2C2vA4sA2148S6b0390.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/fm6K31312349120.jpg",
                description = "The story of J. Robert Oppenheimer's role in the development of the atomic bomb during World War II.",
                year = "2023",
                duration = "3h 00m",
                genres = "Drama, History",
                rating = "8.9",
                cast = "Cillian Murphy, Emily Blunt, Matt Damon, Robert Downey Jr.",
                director = "Christopher Nolan"
            ),
            Movie(
                movieId = "569094",
                mediaType = MediaType.MOVIE,
                name = "Spider-Man: Across the Spider-Verse",
                imageUrl = "https://image.tmdb.org/t/p/w500/8Vt6mWEReuy4Of61Lnj5Xj7s32.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/4XM8123049120.jpg",
                description = "Miles Morales catapults across the Multiverse, where he encounters a team of Spider-People charged with protecting its existence.",
                year = "2023",
                duration = "2h 20m",
                genres = "Animation, Action, Adventure",
                rating = "8.8",
                cast = "Shameik Moore, Hailee Steinfeld, Oscar Isaac",
                director = "Joaquim Dos Santos"
            ),
            Movie(
                movieId = "100088",
                mediaType = MediaType.TV,
                name = "The Last of Us",
                imageUrl = "https://image.tmdb.org/t/p/w500/u3bZgnA2vA4sA2148S6b0390.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/a9123049120.jpg",
                description = "Twenty years after modern civilization has been destroyed, Joel is hired to smuggle Ellie out of an oppressive quarantine zone.",
                year = "2023",
                duration = "1 Season",
                genres = "Drama, Sci-Fi",
                rating = "8.8",
                cast = "Pedro Pascal, Bella Ramsey",
                director = "Craig Mazin, Neil Druckmann"
            ),
            Movie(
                movieId = "335984",
                mediaType = MediaType.MOVIE,
                name = "Blade Runner 2049",
                imageUrl = "https://image.tmdb.org/t/p/w500/g9123049120.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/b9123049120.jpg",
                description = "Young Blade Runner K's discovery of a long-buried secret leads him to track down former Blade Runner Rick Deckard.",
                year = "2017",
                duration = "2h 44m",
                genres = "Sci-Fi, Mystery",
                rating = "8.0",
                cast = "Ryan Gosling, Harrison Ford, Ana de Armas",
                director = "Denis Villeneuve"
            ),
            Movie(
                movieId = "76600",
                mediaType = MediaType.MOVIE,
                name = "Avatar: The Way of Water",
                imageUrl = "https://image.tmdb.org/t/p/w500/t69123049120.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/c9123049120.jpg",
                description = "Jake Sully lives with his newfound family formed on the extrasolar moon Pandora.",
                year = "2022",
                duration = "3h 12m",
                genres = "Sci-Fi, Action, Adventure",
                rating = "7.6",
                cast = "Sam Worthington, Zoe Saldana, Sigourney Weaver",
                director = "James Cameron"
            ),
            Movie(
                movieId = "361743",
                mediaType = MediaType.MOVIE,
                name = "Top Gun: Maverick",
                imageUrl = "https://image.tmdb.org/t/p/w500/62123049120.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/d9123049120.jpg",
                description = "After thirty years, Maverick is still pushing the envelope as a top naval aviator.",
                year = "2022",
                duration = "2h 11m",
                genres = "Action, Drama",
                rating = "8.3",
                cast = "Tom Cruise, Miles Teller, Jennifer Connelly",
                director = "Joseph Kosinski"
            ),
            Movie(
                movieId = "27205",
                mediaType = MediaType.MOVIE,
                name = "Inception",
                imageUrl = "https://image.tmdb.org/t/p/w500/ed123049120.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/e9123049120.jpg",
                description = "A thief who steals corporate secrets through the use of dream-sharing technology is given the inverse task of planting an idea.",
                year = "2010",
                duration = "2h 28m",
                genres = "Sci-Fi, Action",
                rating = "8.8",
                cast = "Leonardo DiCaprio, Joseph Gordon-Levitt, Elliot Page",
                director = "Christopher Nolan"
            ),
            Movie(
                movieId = "603",
                mediaType = MediaType.MOVIE,
                name = "The Matrix",
                imageUrl = "https://image.tmdb.org/t/p/w500/f123049120.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/f9123049120.jpg",
                description = "When a beautiful stranger leads computer hacker Neo to a forbidding underworld, he discovers the shocking truth.",
                year = "1999",
                duration = "2h 16m",
                genres = "Sci-Fi, Action",
                rating = "8.7",
                cast = "Keanu Reeves, Laurence Fishburne, Carrie-Anne Moss",
                director = "Lana Wachowski, Lilly Wachowski"
            ),
            Movie(
                movieId = "329865",
                mediaType = MediaType.MOVIE,
                name = "Arrival",
                imageUrl = "https://image.tmdb.org/t/p/w500/g123049120.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/g9123049120.jpg",
                description = "A linguist works with the military to communicate with alien lifeforms after twelve mysterious spacecraft appear around the world.",
                year = "2016",
                duration = "1h 56m",
                genres = "Sci-Fi, Drama, Mystery",
                rating = "7.9",
                cast = "Amy Adams, Jeremy Renner, Forest Whitaker",
                director = "Denis Villeneuve"
            )
        )
    }

    private fun seedPartnerMovies(): List<Movie> {
        return listOf(
            Movie(
                movieId = "2164",
                mediaType = MediaType.MOVIE,
                name = "The Notebook",
                imageUrl = "https://image.tmdb.org/t/p/w500/r123049120.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/r9123049120.jpg",
                description = "An elderly man reads to a woman with Alzheimer's the story of two young lovers separated by social differences.",
                year = "2004",
                duration = "2h 03m",
                genres = "Romance, Drama",
                rating = "7.8",
                cast = "Ryan Gosling, Rachel McAdams",
                director = "Nick Cassavetes"
            ),
            Movie(
                movieId = "1072790",
                mediaType = MediaType.MOVIE,
                name = "Anyone But You",
                imageUrl = "https://image.tmdb.org/t/p/w500/s123049120.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/s9123049120.jpg",
                description = "After an amazing first date, Bea and Ben's fiery attraction turns ice cold until they find themselves unexpectedly reunited at a destination wedding in Australia.",
                year = "2023",
                duration = "1h 43m",
                genres = "Romance, Comedy",
                rating = "7.1",
                cast = "Sydney Sweeney, Glen Powell",
                director = "Will Gluck"
            ),
            Movie(
                movieId = "1063879",
                mediaType = MediaType.MOVIE,
                name = "The Fall Guy",
                imageUrl = "https://image.tmdb.org/t/p/w500/t123049120.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/t9123049120.jpg",
                description = "A down-on-his-luck stuntman must find a missing movie star, solve a conspiracy and try to win back the love of his life.",
                year = "2024",
                duration = "2h 06m",
                genres = "Action, Comedy, Romance",
                rating = "7.3",
                cast = "Ryan Gosling, Emily Blunt, Aaron Taylor-Johnson",
                director = "David Leitch"
            ),
            Movie(
                movieId = "346698",
                mediaType = MediaType.MOVIE,
                name = "Barbie",
                imageUrl = "https://image.tmdb.org/t/p/w500/u123049120.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/u9123049120.jpg",
                description = "Barbie and Ken are having the time of their lives in the colorful and seemingly perfect world of Barbie Land.",
                year = "2023",
                duration = "1h 54m",
                genres = "Comedy, Adventure, Fantasy",
                rating = "7.2",
                cast = "Margot Robbie, Ryan Gosling, America Ferrera",
                director = "Greta Gerwig"
            ),
            Movie(
                movieId = "455207",
                mediaType = MediaType.MOVIE,
                name = "Crazy Rich Asians",
                imageUrl = "https://image.tmdb.org/t/p/w500/v123049120.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/v9123049120.jpg",
                description = "An Asian-American professor travels to Singapore to meet her boyfriend's family and discovers they're among the richest in the country.",
                year = "2018",
                duration = "2h 01m",
                genres = "Comedy, Romance",
                rating = "7.1",
                cast = "Constance Wu, Henry Golding, Michelle Yeoh",
                director = "Jon M. Chu"
            ),
            Movie(
                movieId = "316029",
                mediaType = MediaType.MOVIE,
                name = "The Greatest Showman",
                imageUrl = "https://image.tmdb.org/t/p/w500/w123049120.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/w9123049120.jpg",
                description = "Celebrates the birth of show business and tells of a visionary who rose from nothing to create a spectacle that became a worldwide sensation.",
                year = "2017",
                duration = "1h 45m",
                genres = "Drama, Music, Romance",
                rating = "7.9",
                cast = "Hugh Jackman, Zac Efron, Zendaya",
                director = "Michael Gracey"
            ),
            Movie(
                movieId = "122906",
                mediaType = MediaType.MOVIE,
                name = "About Time",
                imageUrl = "https://image.tmdb.org/t/p/w500/x123049120.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/x9123049120.jpg",
                description = "At the age of 21, Tim discovers he can travel in time and change what happens and has happened in his own life.",
                year = "2013",
                duration = "2h 03m",
                genres = "Romance, Drama, Fantasy",
                rating = "8.1",
                cast = "Domhnall Gleeson, Rachel McAdams, Bill Nighy",
                director = "Richard Curtis"
            ),
            Movie(
                movieId = "313369",
                mediaType = MediaType.MOVIE,
                name = "La La Land",
                imageUrl = "https://image.tmdb.org/t/p/w500/y123049120.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/y9123049120.jpg",
                description = "While navigating their careers in Los Angeles, a pianist and an actress fall in love while attempting to reconcile their aspirations.",
                year = "2016",
                duration = "2h 08m",
                genres = "Comedy, Drama, Music, Romance",
                rating = "8.0",
                cast = "Ryan Gosling, Emma Stone, John Legend",
                director = "Damien Chazelle"
            )
        )
    }

    private fun seedHorrorMovies(): List<Movie> {
        return listOf(
            Movie(
                movieId = "348",
                mediaType = MediaType.MOVIE,
                name = "Alien",
                imageUrl = "https://image.tmdb.org/t/p/w500/z123049120.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/z9123049120.jpg",
                description = "The crew of a commercial spacecraft encounter a deadly lifeform after investigating a mysterious transmission.",
                year = "1979",
                duration = "1h 57m",
                genres = "Horror, Sci-Fi",
                rating = "8.1",
                cast = "Sigourney Weaver, Tom Skerritt, John Hurt",
                director = "Ridley Scott"
            ),
            Movie(
                movieId = "138843",
                mediaType = MediaType.MOVIE,
                name = "The Conjuring",
                imageUrl = "https://image.tmdb.org/t/p/w500/aa123049120.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/aa9123049120.jpg",
                description = "Paranormal investigators Ed and Lorraine Warren work to help a family terrorized by a dark presence in their farmhouse.",
                year = "2013",
                duration = "1h 52m",
                genres = "Horror, Mystery, Thriller",
                rating = "7.5",
                cast = "Vera Farmiga, Patrick Wilson, Lili Taylor",
                director = "James Wan"
            ),
            Movie(
                movieId = "447332",
                mediaType = MediaType.MOVIE,
                name = "A Quiet Place",
                imageUrl = "https://image.tmdb.org/t/p/w500/ab123049120.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/ab9123049120.jpg",
                description = "In a post-apocalyptic world, a family is forced to live in silence while hiding from monsters with ultra-sensitive hearing.",
                year = "2018",
                duration = "1h 30m",
                genres = "Horror, Drama, Sci-Fi",
                rating = "7.4",
                cast = "Emily Blunt, John Krasinski, Millicent Simmonds",
                director = "John Krasinski"
            ),
            Movie(
                movieId = "419430",
                mediaType = MediaType.MOVIE,
                name = "Get Out",
                imageUrl = "https://image.tmdb.org/t/p/w500/ac123049120.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/ac9123049120.jpg",
                description = "A young African-American visits his white girlfriend's parents for the weekend, where his simmering uneasiness about their reception reaches a boiling point.",
                year = "2017",
                duration = "1h 44m",
                genres = "Horror, Mystery, Thriller",
                rating = "7.7",
                cast = "Daniel Kaluuya, Allison Williams, Bradley Whitford",
                director = "Jordan Peele"
            ),
            Movie(
                movieId = "493922",
                mediaType = MediaType.MOVIE,
                name = "Hereditary",
                imageUrl = "https://image.tmdb.org/t/p/w500/ad123049120.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/ad9123049120.jpg",
                description = "A grieving family is haunted by tragic and disturbing occurrences after the death of their secretive grandmother.",
                year = "2018",
                duration = "2h 07m",
                genres = "Horror, Mystery, Drama",
                rating = "7.3",
                cast = "Toni Collette, Alex Wolff, Milly Shapiro",
                director = "Ari Aster"
            ),
            Movie(
                movieId = "530385",
                mediaType = MediaType.MOVIE,
                name = "Midsommar",
                imageUrl = "https://image.tmdb.org/t/p/w500/ae123049120.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/ae9123049120.jpg",
                description = "A couple travels to Northern Europe to visit a rural hometown's fabled Swedish midsummer festival. What begins as an idyllic retreat quickly devolves into an increasingly violent pagan competition.",
                year = "2019",
                duration = "2h 27m",
                genres = "Horror, Drama, Mystery",
                rating = "7.1",
                cast = "Florence Pugh, Jack Reynor, William Jackson Harper",
                director = "Ari Aster"
            )
        )
    }
}

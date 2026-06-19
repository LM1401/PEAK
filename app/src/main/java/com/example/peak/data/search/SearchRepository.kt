package com.example.peak.data.search

import android.util.Log
import com.example.peak.ui.search.SearchItem
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SearchRepository(private val client: OkHttpClient) {

    private val TAG = "SearchRepo"
    private val apiKey = "28698e1796764c8280ae3896267caecd"
    private val imageBaseUrl = "https://image.tmdb.org/t/p/w500"

    suspend fun getTrending(): List<SearchItem> = withContext(Dispatchers.IO) {
        val url = "https://api.themoviedb.org/3/trending/all/week?api_key=$apiKey"
        Log.d(TAG, "Requesting Trending")
        return@withContext executeAndParse(url)
    }

    suspend fun search(query: String): List<SearchItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val url = "https://api.themoviedb.org/3/search/multi?api_key=$apiKey&query=$query"
        Log.d(TAG, "Requesting Search: $query")
        return@withContext executeAndParse(url)
    }

    private fun executeAndParse(url: String): List<SearchItem> {
        val request = Request.Builder().url(url).build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "HTTP Error: ${response.code}")
                    return emptyList()
                }

                val body = response.body?.string() ?: return emptyList()
                val jsonObject = JSONObject(body)
                val jsonArray = jsonObject.optJSONArray("results") ?: return emptyList()
                
                val results = mutableListOf<SearchItem>()
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.optJSONObject(i) ?: continue
                    val type = item.optString("media_type", "movie")
                    
                    val title = if (type == "movie") {
                        item.optString("title")
                    } else {
                        item.optString("name")
                    }

                    if (title.isNullOrBlank()) continue

                    val date = if (type == "movie") {
                        item.optString("release_date")
                    } else {
                        item.optString("first_air_date")
                    }
                    val year = if (date.length >= 4) date.substring(0, 4) else null

                    val posterPath = item.optString("poster_path")
                    val posterUrl = if (!posterPath.isNullOrBlank() && posterPath != "null") {
                        "$imageBaseUrl$posterPath"
                    } else null

                    val backdropPath = item.optString("backdrop_path")
                    val backdropUrl = if (!backdropPath.isNullOrBlank() && backdropPath != "null") {
                        "https://image.tmdb.org/t/p/w1280$backdropPath"
                    } else null

                    val overview = item.optString("overview")
                    val rating = item.optDouble("vote_average", 0.0)
                    val popularity = item.optDouble("popularity", 0.0)

                    results.add(
                        SearchItem(
                            id = item.optString("id", i.toString()),
                            title = title,
                            year = year,
                            type = type,
                            posterUrl = posterUrl,
                            backdropUrl = backdropUrl,
                            overview = overview,
                            rating = rating,
                            popularity = popularity
                        )
                    )
                }
                Log.d(TAG, "Parsed ${results.size} items")
                results
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during fetch/parse", e)
            emptyList()
        }
    }
}

package com.example.peak.data.remote.dto

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TraktDtoTest {
    private val gson = Gson()

    @Test
    fun `Movie response parsing`() {
        val json = """
        [
          {
            "type": "movie",
            "score": 1000,
            "movie": {
              "title": "Batman Begins",
              "ids": {
                "trakt": 1,
                "tmdb": 272
              },
              "images": {
                "logo": [
                  "https://walter.trakt.tv/logo.png"
                ]
              }
            }
          }
        ]
        """.trimIndent()

        val results = gson.fromJson(json, Array<TraktSearchResponseDto>::class.java).toList()
        assertEquals(1, results.size)
        val item = results.first()
        assertEquals("movie", item.type)
        assertEquals("Batman Begins", item.movie?.title)
        assertEquals(272, item.movie?.ids?.tmdb)
        assertEquals("https://walter.trakt.tv/logo.png", item.movie?.images?.logo?.first())
        assertNull(item.show)
    }

    @Test
    fun `TV response parsing`() {
        val json = """
        [
          {
            "type": "show",
            "score": 1000,
            "show": {
              "title": "Breaking Bad",
              "ids": {
                "trakt": 2,
                "tmdb": 1396
              },
              "images": {
                "logo": [
                  "https://walter.trakt.tv/show_logo.png"
                ]
              }
            }
          }
        ]
        """.trimIndent()

        val results = gson.fromJson(json, Array<TraktSearchResponseDto>::class.java).toList()
        assertEquals(1, results.size)
        val item = results.first()
        assertEquals("show", item.type)
        assertEquals("Breaking Bad", item.show?.title)
        assertEquals(1396, item.show?.ids?.tmdb)
        assertEquals("https://walter.trakt.tv/show_logo.png", item.show?.images?.logo?.first())
        assertNull(item.movie)
    }

    @Test
    fun `Missing logo arrays parsing`() {
        val json = """
        [
          {
            "type": "movie",
            "movie": {
              "title": "No Logo Movie",
              "ids": {
                "tmdb": 123
              },
              "images": {}
            }
          }
        ]
        """.trimIndent()

        val results = gson.fromJson(json, Array<TraktSearchResponseDto>::class.java).toList()
        val item = results.first()
        assertNull(item.movie?.images?.logo)
    }
}

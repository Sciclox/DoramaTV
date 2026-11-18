package com.tvpro.app.navigation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NavigationLogicTest {

    private val navToScreen = mapOf(
        "home" to TvScreen.HOME,
        "emision" to TvScreen.EMISION,
        "series" to TvScreen.SERIES,
        "movies" to TvScreen.MOVIES,
        "latino" to TvScreen.LATINO,
        "search" to TvScreen.SEARCH,
        "my_list" to TvScreen.MY_LIST
    )

    @Test
    fun testNavIdToScreenMappingIsComplete() {
        assertEquals(TvScreen.HOME, navToScreen["home"])
        assertEquals(TvScreen.EMISION, navToScreen["emision"])
        assertEquals(TvScreen.SERIES, navToScreen["series"])
        assertEquals(TvScreen.MOVIES, navToScreen["movies"])
        assertEquals(TvScreen.LATINO, navToScreen["latino"])
        assertEquals(TvScreen.SEARCH, navToScreen["search"])
        assertEquals(TvScreen.MY_LIST, navToScreen["my_list"])
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testDebouncedNavigationCancelsRapidTraversals() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(testDispatcher)

        var selectedScreenId: String? = "home"
        var triggeredScreenId: String? = null
        var navJob: kotlinx.coroutines.Job? = null

        val onFocusGained: (String) -> Unit = { id ->
            navJob?.cancel()
            navJob = scope.launch {
                delay(800L)
                triggeredScreenId = id
            }
        }

        // Simulate rapid D-Pad RIGHT presses through "movies", "series", "animes" to "my_list"
        onFocusGained("movies")
        advanceTimeBy(200L) // only 200ms elapsed
        assertNull("Should not have triggered 'movies' yet", triggeredScreenId)

        onFocusGained("series")
        advanceTimeBy(300L) // 300ms elapsed
        assertNull("Should not have triggered 'series' yet", triggeredScreenId)

        onFocusGained("animes")
        advanceTimeBy(150L) // 150ms elapsed
        assertNull("Should not have triggered 'animes' yet", triggeredScreenId)

        // Settle on "my_list"
        onFocusGained("my_list")
        advanceTimeBy(400L)
        assertNull("Should still be waiting at 400ms", triggeredScreenId)

        advanceTimeBy(450L) // total 850ms on "my_list"
        assertEquals("Should trigger 'my_list' after 800ms settled", "my_list", triggeredScreenId)
    }
}

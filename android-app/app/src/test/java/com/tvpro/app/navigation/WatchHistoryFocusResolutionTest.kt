package com.tvpro.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class WatchHistoryFocusResolutionTest {

    private fun resolveTargetIndex(
        historyCount: Int,
        lastFocusedIndex: Int,
        firstVisibleIndex: Int,
        activeRequestersIndices: Set<Int>
    ): Int {
        if (historyCount <= 0) return 0

        // 1. Last focused card if attached
        if (lastFocusedIndex in activeRequestersIndices) {
            return lastFocusedIndex
        }

        // 2. First visible item if attached
        if (firstVisibleIndex in activeRequestersIndices) {
            return firstVisibleIndex
        }

        // 3. Any attached card
        if (activeRequestersIndices.isNotEmpty()) {
            return activeRequestersIndices.minOrNull() ?: 0
        }

        // 4. Fallback: clamped target
        return lastFocusedIndex.coerceIn(0, historyCount - 1)
    }

    @Test
    fun testInitialRowStateSelectsIndex0() {
        // Initial state: cards 0, 1, 2, 3 visible on screen
        val active = setOf(0, 1, 2, 3)
        val target = resolveTargetIndex(
            historyCount = 8,
            lastFocusedIndex = 0,
            firstVisibleIndex = 0,
            activeRequestersIndices = active
        )
        assertEquals(0, target)
    }

    @Test
    fun testAdvancedRowStateSelectsLastFocusedCardEvenWhenIndex0IsOffscreen() {
        // Advanced row as in user screenshot:
        // cards 0, 1, 2 are scrolled off-screen to the left (not in active set!)
        // cards 3, 4, 5, 6 are currently visible on screen.
        // User was on card 3 (Street Fighter) and moved up to Hero Banner.
        val active = setOf(3, 4, 5, 6)
        val target = resolveTargetIndex(
            historyCount = 10,
            lastFocusedIndex = 3,
            firstVisibleIndex = 3,
            activeRequestersIndices = active
        )
        // Must successfully pick card 3 without getting stuck on index 0!
        assertEquals(3, target)
    }

    @Test
    fun testAdvancedRowStateFallsBackToFirstVisibleCard() {
        // User scrolled manually without focusing (e.g. scroll state shifted),
        // card 4 is first visible, but last focused was 1 (which is off-screen)
        val active = setOf(4, 5, 6, 7)
        val target = resolveTargetIndex(
            historyCount = 10,
            lastFocusedIndex = 1,
            firstVisibleIndex = 4,
            activeRequestersIndices = active
        )
        assertEquals(4, target)
    }
}

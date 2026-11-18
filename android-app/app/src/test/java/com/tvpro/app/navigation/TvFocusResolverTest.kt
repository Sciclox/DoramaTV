package com.tvpro.app.navigation

import org.junit.Assert.*
import org.junit.Test

class TvFocusResolverTest {

    @Test
    fun testResolveTargetIndex_emptyList_returnsZero() {
        val target = TvFocusResolver.resolveTargetIndex(
            itemCount = 0,
            lastFocusedIndex = 5,
            firstVisibleIndex = 2,
            activeIndices = emptySet()
        )
        assertEquals(0, target)
    }

    @Test
    fun testResolveTargetIndex_initialState_selectsZero() {
        val target = TvFocusResolver.resolveTargetIndex(
            itemCount = 10,
            lastFocusedIndex = 0,
            firstVisibleIndex = 0,
            activeIndices = setOf(0, 1, 2, 3)
        )
        assertEquals(0, target)
    }

    @Test
    fun testResolveTargetIndex_advancedRow_selectsLastFocusedIfActive() {
        // Row is advanced: cards 0, 1, 2 unmounted, cards 3, 4, 5, 6 active.
        // User was on card 4 when leaving row.
        val target = TvFocusResolver.resolveTargetIndex(
            itemCount = 12,
            lastFocusedIndex = 4,
            firstVisibleIndex = 3,
            activeIndices = setOf(3, 4, 5, 6)
        )
        assertEquals(4, target)
    }

    @Test
    fun testResolveTargetIndex_lastFocusedUnmounted_selectsFirstVisible() {
        // User had focused card 1, but row scrolled further so cards 4, 5, 6, 7 are active
        val target = TvFocusResolver.resolveTargetIndex(
            itemCount = 12,
            lastFocusedIndex = 1,
            firstVisibleIndex = 4,
            activeIndices = setOf(4, 5, 6, 7)
        )
        assertEquals(4, target)
    }

    @Test
    fun testResolveTargetIndex_neitherActive_selectsClosestActiveIndex() {
        // Last focused is 8, first visible reported is 1 (out of date), active are 5, 6, 7
        val target = TvFocusResolver.resolveTargetIndex(
            itemCount = 15,
            lastFocusedIndex = 8,
            firstVisibleIndex = 1,
            activeIndices = setOf(5, 6, 7)
        )
        // 7 is closest to 8
        assertEquals(7, target)
    }

    @Test
    fun testResolveTargetIndex_noActiveIndices_returnsClampedLastFocused() {
        val target = TvFocusResolver.resolveTargetIndex(
            itemCount = 8,
            lastFocusedIndex = 10,
            firstVisibleIndex = 0,
            activeIndices = emptySet()
        )
        assertEquals(7, target)
    }
}

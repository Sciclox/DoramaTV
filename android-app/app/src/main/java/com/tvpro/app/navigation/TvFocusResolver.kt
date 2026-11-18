package com.tvpro.app.navigation

import androidx.compose.ui.focus.FocusRequester

/**
 * Robust focus resolution utility for Android TV virtualized lists (LazyRow, LazyColumn, LazyVerticalGrid).
 *
 * In Jetpack Compose TV, virtualized items scrolled off-screen are unmounted/detached.
 * Attempting to request focus on an unmounted FocusRequester throws an exception or fails silently.
 * If an upstream key listener consumes the key event (returns true) during a failed request,
 * focus becomes permanently trapped.
 *
 * This utility provides safe target index resolution and focus dispatching to ensure focus is
 * NEVER trapped when rows or grids are advanced.
 */
object TvFocusResolver {

    /**
     * Resolves the best target item index in a virtualized container:
     * 1. The last focused item index if currently composed/active.
     * 2. The first visible item index if currently composed/active.
     * 3. Any composed/active item index closest to the last focused index.
     * 4. A clamped index as a fallback for scrolling into view.
     */
    fun resolveTargetIndex(
        itemCount: Int,
        lastFocusedIndex: Int,
        firstVisibleIndex: Int,
        activeIndices: Collection<Int>
    ): Int {
        if (itemCount <= 0) return 0

        val clampedLast = lastFocusedIndex.coerceIn(0, itemCount - 1)
        val clampedVisible = firstVisibleIndex.coerceIn(0, itemCount - 1)

        // 1. Prioritize last focused item if active
        if (clampedLast in activeIndices) {
            return clampedLast
        }

        // 2. Prioritize first visible item if active
        if (clampedVisible in activeIndices) {
            return clampedVisible
        }

        // 3. Pick closest active index to last focused
        if (activeIndices.isNotEmpty()) {
            return activeIndices.minByOrNull { kotlin.math.abs(it - clampedLast) } ?: activeIndices.first()
        }

        // 4. Clamped fallback
        return clampedLast
    }

    /**
     * Safely attempts to request focus without crashing if unmounted.
     * Returns true if request succeeded, false otherwise.
     */
    fun safeRequestFocus(requester: FocusRequester?): Boolean {
        if (requester == null) return false
        return try {
            requester.requestFocus()
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Attempts to request focus on a specific target index in a map of requesters.
     * If the target is not mounted or fails, attempts the fallback indices in order,
     * then attempts any available mounted requester.
     */
    fun requestTargetFocus(
        requesters: Map<Int, FocusRequester>,
        targetIndex: Int,
        fallbackIndices: List<Int> = emptyList()
    ): Boolean {
        // 1. Try target
        val targetReq = requesters[targetIndex]
        if (safeRequestFocus(targetReq)) return true

        // 2. Try fallbacks
        for (idx in fallbackIndices) {
            val fbReq = requesters[idx]
            if (safeRequestFocus(fbReq)) return true
        }

        // 3. Try any available active requester
        for (req in requesters.values) {
            if (safeRequestFocus(req)) return true
        }

        return false
    }

    /**
     * Attempts to request focus on the target item in a list of requesters.
     */
    fun requestListTargetFocus(
        requesters: List<FocusRequester>,
        targetIndex: Int,
        firstVisibleIndex: Int = 0
    ): Boolean {
        if (requesters.isEmpty()) return false

        val clampedTarget = targetIndex.coerceIn(0, requesters.lastIndex)
        if (safeRequestFocus(requesters.getOrNull(clampedTarget))) return true

        val clampedVisible = firstVisibleIndex.coerceIn(0, requesters.lastIndex)
        if (clampedVisible != clampedTarget && safeRequestFocus(requesters.getOrNull(clampedVisible))) return true

        for (req in requesters) {
            if (safeRequestFocus(req)) return true
        }

        return false
    }
}

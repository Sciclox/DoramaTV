package com.tvpro.core.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebEmbedPlayerBridgeTest {

    @Test
    fun testPlayPauseJsContainsUniversalControls() {
        val pauseJs = WebEmbedPlayerBridge.buildPlayPauseJs(play = false)
        assertTrue(pauseJs.contains("targetPlay = false"))
        assertTrue(pauseJs.contains("playbackRate = 0.0"))
        assertTrue(pauseJs.contains("el.pause()"))
        assertTrue(pauseJs.contains("jp.pause()"))
        assertTrue(pauseJs.contains("postMessage"))

        val playJs = WebEmbedPlayerBridge.buildPlayPauseJs(play = true)
        assertTrue(playJs.contains("targetPlay = true"))
        assertTrue(playJs.contains("playbackRate = 1.0"))
        assertTrue(playJs.contains("el.play()"))
        assertTrue(playJs.contains("jp.play(true)"))
    }

    @Test
    fun testSeekJsContainsSeekCommands() {
        val seekJs = WebEmbedPlayerBridge.buildSeekJs(45.5)
        assertTrue(seekJs.contains("targetSec = 45.5"))
        assertTrue(seekJs.contains("seekJw"))
        assertTrue(seekJs.contains("targetSec"))
        assertTrue(seekJs.contains("postMessage"))
    }

    @Test
    fun testSeekLockWindow() {
        val now = 100_000L
        assertTrue(WebEmbedPlayerBridge.isSeekLocked(lastSeekTimeMs = now - 1000L, nowMs = now, lockDurationMs = 4000L))
        assertTrue(WebEmbedPlayerBridge.isSeekLocked(lastSeekTimeMs = now - 3999L, nowMs = now, lockDurationMs = 4000L))
        assertFalse(WebEmbedPlayerBridge.isSeekLocked(lastSeekTimeMs = now - 4001L, nowMs = now, lockDurationMs = 4000L))
        assertFalse(WebEmbedPlayerBridge.isSeekLocked(lastSeekTimeMs = 0L, nowMs = now, lockDurationMs = 4000L))
    }

    @Test
    fun testPlayPauseLockWindow() {
        val now = 200_000L
        assertTrue(WebEmbedPlayerBridge.isPlayPauseLocked(lastToggleTimeMs = now - 500L, nowMs = now, lockDurationMs = 2500L))
        assertFalse(WebEmbedPlayerBridge.isPlayPauseLocked(lastToggleTimeMs = now - 2600L, nowMs = now, lockDurationMs = 2500L))
    }
}

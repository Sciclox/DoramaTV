package com.tvpro.core.domain

import java.util.Locale

object PlayerTimeUtils {

    /**
     * Formats seconds into HH:MM:SS format (e.g. 01:05:45).
     */
    fun formatTimeHms(seconds: Long): String {
        val totalSec = seconds.coerceAtLeast(0L)
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    }

    /**
     * Formats seconds into MM:SS if under 1 hour, or H:MM:SS if 1 hour or more.
     */
    fun formatTimeSec(seconds: Long): String {
        val totalSec = seconds.coerceAtLeast(0L)
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) {
            String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%02d:%02d", m, s)
        }
    }

    /**
     * Formats milliseconds into HH:MM:SS or MM:SS format.
     */
    fun formatTimeMs(millis: Long): String {
        return formatTimeSec(millis / 1000L)
    }
}

package com.tvpro.core.domain

import com.tvpro.core.data.model.EpisodeItem

object EpisodeNavigator {

    /**
     * Resolves the next episode in the series:
     * 1. Next episode in the same season.
     * 2. If at the end of the season and next season exists, Season + 1, Ep 1.
     * 3. Null if at the very end of the series.
     */
    fun findNextEpisode(
        currentSeason: Int,
        currentEpisode: Int,
        totalSeasons: Int,
        seasonEpisodes: List<EpisodeItem>
    ): EpisodeItem? {
        val nextInSeason = seasonEpisodes.firstOrNull {
            it.seasonNumber == currentSeason && it.episodeNumber == currentEpisode + 1
        }
        if (nextInSeason != null) return nextInSeason

        // If not in current season, check if we can advance to the next season
        if (currentSeason < totalSeasons) {
            val nextSeasonFirstEp = seasonEpisodes.firstOrNull {
                it.seasonNumber == currentSeason + 1 && it.episodeNumber == 1
            }
            if (nextSeasonFirstEp != null) return nextSeasonFirstEp
        }

        return null
    }

    /**
     * Resolves the previous episode in the series.
     */
    fun findPreviousEpisode(
        currentSeason: Int,
        currentEpisode: Int,
        seasonEpisodes: List<EpisodeItem>
    ): EpisodeItem? {
        val prevInSeason = seasonEpisodes.firstOrNull {
            it.seasonNumber == currentSeason && it.episodeNumber == currentEpisode - 1
        }
        if (prevInSeason != null) return prevInSeason

        if (currentSeason > 1) {
            val prevSeasonEpisodes = seasonEpisodes.filter { it.seasonNumber == currentSeason - 1 }
            return prevSeasonEpisodes.maxByOrNull { it.episodeNumber }
        }

        return null
    }
}

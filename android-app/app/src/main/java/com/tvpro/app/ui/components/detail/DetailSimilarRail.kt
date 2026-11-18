package com.tvpro.app.ui.components.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import com.tvpro.app.ui.components.TvMovieCard
import com.tvpro.core.data.model.MovieItem

@Composable
fun DetailSimilarRail(
    similarMovies: List<MovieItem>,
    similarTabFocusRequester: FocusRequester,
    onMovieSelect: (MovieItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp, start = 2.dp, end = 24.dp),
        modifier = modifier
    ) {
        items(similarMovies, key = { it.url }) { simMovie ->
            TvMovieCard(
                movie = simMovie,
                onClick = { onMovieSelect(simMovie) },
                modifier = Modifier
                    .focusProperties {
                        up = similarTabFocusRequester
                    }
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionUp) {
                            try { similarTabFocusRequester.requestFocus() } catch (_: Exception) {}
                            true
                        } else false
                    }
            )
        }
    }
}

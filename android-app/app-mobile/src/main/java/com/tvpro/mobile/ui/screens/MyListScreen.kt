package com.tvpro.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvpro.core.data.model.MovieItem
import com.tvpro.core.data.model.WatchHistoryItem
import com.tvpro.core.data.repository.WatchHistoryManager
import com.tvpro.mobile.ui.components.MobileMovieCard
import com.tvpro.mobile.ui.theme.*

@Composable
fun MyListScreen(
    onMovieClick: (MovieItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var myList by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var rawHistoryList by remember { mutableStateOf<List<WatchHistoryItem>>(emptyList()) }

    fun refreshData() {
        myList = WatchHistoryManager.getMyList(context)
        rawHistoryList = WatchHistoryManager.getHistory(context)
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    val historyMovieItems = remember(rawHistoryList) {
        rawHistoryList.map { item ->
            MovieItem(
                title = item.title,
                url = item.movieUrl,
                image = item.image,
                rating = item.rating,
                postId = item.postId,
                slug = item.slug,
                type = if (item.isSeries) "series" else "movie"
            )
        }
    }

    val tabs = listOf("Mi Lista (${myList.size})", "Historial (${rawHistoryList.size})")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TvProDarkBg)
            .statusBarsPadding()
            .padding(top = 10.dp)
    ) {
        // Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Mi Biblioteca",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = (-0.3).sp,
                color = WarmIvory
            )

            if (selectedTabIndex == 1 && rawHistoryList.isNotEmpty()) {
                TextButton(onClick = {
                    WatchHistoryManager.clearHistory(context)
                    refreshData()
                }) {
                    Text(
                        text = "Borrar historial",
                        color = SoftSilver,
                        fontSize = 12.sp,
                        fontFamily = Inter
                    )
                }
            }
        }

        // Tab Row
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = TvProDarkBg,
            contentColor = Color.White,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = MochiStrawberryPink,
                    height = 2.5.dp
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTabIndex == index) MochiStrawberryPink else MutedSlate,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = Inter,
                            fontSize = 13.5.sp
                        )
                    }
                )
            }
        }

        val isListEmpty = if (selectedTabIndex == 0) myList.isEmpty() else rawHistoryList.isEmpty()

        if (isListEmpty) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedTabIndex == 0)
                        "Aún no tienes títulos guardados en Mi Lista."
                    else
                        "No tienes historial de reproducción reciente.",
                    color = MutedSlate,
                    fontSize = 14.sp,
                    fontFamily = Inter,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (selectedTabIndex == 0) {
                    items(myList, key = { it.url.ifBlank { it.title } }) { movie ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            MobileMovieCard(
                                movie = movie,
                                fillWidth = true,
                                onClick = { onMovieClick(movie) }
                            )
                            // Remove from MyList button
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.75f))
                                    .border(1.dp, SubtleBorder, CircleShape)
                                    .clickable {
                                        WatchHistoryManager.removeFromMyList(context, movie.url)
                                        refreshData()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Quitar",
                                    tint = WarmIvory,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                } else {
                    items(rawHistoryList, key = { it.movieUrl }) { historyItem ->
                        val movie = remember(historyItem) {
                            MovieItem(
                                title = historyItem.title,
                                url = historyItem.movieUrl,
                                image = historyItem.image,
                                rating = historyItem.rating,
                                postId = historyItem.postId,
                                slug = historyItem.slug,
                                type = if (historyItem.isSeries) "series" else "movie"
                            )
                        }
                        val progressFraction = remember(historyItem) {
                            if (historyItem.durationMs > 0) {
                                (historyItem.playbackPositionMs.toFloat() / historyItem.durationMs.toFloat()).coerceIn(0.05f, 1f)
                            } else 0f
                        }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                MobileMovieCard(
                                    movie = movie,
                                    fillWidth = true,
                                    onClick = { onMovieClick(movie) }
                                )
                                // Red progress bar indicator underneath poster
                                if (progressFraction > 0f) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(MochiPlumBorder)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(progressFraction)
                                                .fillMaxHeight()
                                                .background(LaMovieRed)
                                        )
                                    }
                                }
                            }
                            // Delete history item button
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.75f))
                                    .border(1.dp, SubtleBorder, CircleShape)
                                    .clickable {
                                        WatchHistoryManager.removeFromHistory(context, historyItem.movieUrl)
                                        refreshData()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Quitar del historial",
                                    tint = WarmIvory,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.tvpro.mobile.navigation

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tvpro.core.data.model.EpisodeItem
import com.tvpro.core.data.model.MovieItem
import com.tvpro.mobile.ui.components.MobileBottomNavBar
import com.tvpro.mobile.ui.components.MobileTab
import com.tvpro.mobile.ui.screens.*
import com.tvpro.mobile.ui.theme.*
import com.tvpro.mobile.util.AppUpdateChecker
import com.tvpro.mobile.util.UpdateInfo

sealed interface MobileRoute {
    data object Splash : MobileRoute
    data object Tabs : MobileRoute
    data class Detail(val movie: MovieItem) : MobileRoute
    data class Player(
        val movie: MovieItem,
        val episode: EpisodeItem? = null,
        val streamUrl: String? = null
    ) : MobileRoute
}

@Composable
fun TvProMobileAppContent(
    activityIntent: Intent? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(MobileTab.HOME) }
    var routeStack by remember { mutableStateOf<List<MobileRoute>>(listOf(MobileRoute.Splash)) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    val currentRoute = routeStack.lastOrNull() ?: MobileRoute.Tabs

    // Check for updates on startup
    LaunchedEffect(Unit) {
        updateInfo = AppUpdateChecker.checkForUpdates()
    }

    // Deep link handling (tvpro://play?url=... or tvpro://detail?...)
    LaunchedEffect(activityIntent) {
        val uri = activityIntent?.data
        if (uri != null && uri.scheme == "tvpro") {
            if (uri.host == "play") {
                val streamUrl = uri.getQueryParameter("url")
                val title = uri.getQueryParameter("title") ?: "Película"
                if (!streamUrl.isNullOrBlank()) {
                    val movie = MovieItem(
                        title = title,
                        url = streamUrl,
                        image = ""
                    )
                    routeStack = routeStack + MobileRoute.Player(movie = movie, streamUrl = streamUrl)
                }
            }
        }
    }

    // Hardware Back Button behavior
    BackHandler(enabled = (routeStack.size > 1 && currentRoute !is MobileRoute.Splash) || currentTab != MobileTab.HOME) {
        if (routeStack.size > 1 && currentRoute !is MobileRoute.Splash) {
            routeStack = routeStack.dropLast(1)
        } else if (currentTab != MobileTab.HOME) {
            currentTab = MobileTab.HOME
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = TvProDarkBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            // Only show bottom bar when on main tabs
            if (currentRoute is MobileRoute.Tabs) {
                MobileBottomNavBar(
                    currentTab = currentTab,
                    onTabSelected = { tab ->
                        currentTab = tab
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    bottom = if (currentRoute is MobileRoute.Tabs) innerPadding.calculateBottomPadding() else 0.dp
                )
        ) {
            AnimatedContent(
                targetState = currentRoute,
                transitionSpec = {
                    when {
                        targetState is MobileRoute.Player || initialState is MobileRoute.Player -> {
                            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                        }
                        targetState is MobileRoute.Detail -> {
                            (slideInHorizontally(animationSpec = tween(280)) { width -> width / 3 } + fadeIn(animationSpec = tween(280)))
                                .togetherWith(slideOutHorizontally(animationSpec = tween(240)) { width -> -width / 4 } + fadeOut(animationSpec = tween(240)))
                        }
                        initialState is MobileRoute.Detail -> {
                            (slideInHorizontally(animationSpec = tween(240)) { width -> -width / 4 } + fadeIn(animationSpec = tween(240)))
                                .togetherWith(slideOutHorizontally(animationSpec = tween(280)) { width -> width / 3 } + fadeOut(animationSpec = tween(280)))
                        }
                        initialState is MobileRoute.Splash -> {
                            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(250))
                        }
                        else -> {
                            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                        }
                    }
                },
                label = "mobileNavTransition",
                modifier = Modifier.fillMaxSize()
            ) { route ->
                when (route) {
                    is MobileRoute.Splash -> {
                        MobileSplashScreen(
                            onSplashFinished = {
                                routeStack = listOf(MobileRoute.Tabs)
                            }
                        )
                    }

                    is MobileRoute.Tabs -> {
                        when (currentTab) {
                            MobileTab.HOME -> {
                                HomeScreen(
                                    onMovieClick = { movie ->
                                        routeStack = routeStack + MobileRoute.Detail(movie)
                                    },
                                    onPlayMovie = { movie ->
                                        routeStack = routeStack + MobileRoute.Player(movie)
                                    },
                                    onSearchClick = {
                                        currentTab = MobileTab.SEARCH
                                    }
                                )
                            }
                            MobileTab.EXPLORE -> {
                                ExploreScreen(
                                    onMovieClick = { movie ->
                                        routeStack = routeStack + MobileRoute.Detail(movie)
                                    }
                                )
                            }
                            MobileTab.SEARCH -> {
                                SearchScreen(
                                    onMovieClick = { movie ->
                                        routeStack = routeStack + MobileRoute.Detail(movie)
                                    }
                                )
                            }
                            MobileTab.MY_LIST -> {
                                MyListScreen(
                                    onMovieClick = { movie ->
                                        routeStack = routeStack + MobileRoute.Detail(movie)
                                    }
                                )
                            }
                        }
                    }

                    is MobileRoute.Detail -> {
                        DetailScreen(
                            movie = route.movie,
                            onBack = {
                                routeStack = routeStack.dropLast(1)
                            },
                            onPlay = { movie, episode, streamUrl ->
                                routeStack = routeStack + MobileRoute.Player(
                                    movie = movie,
                                    episode = episode,
                                    streamUrl = streamUrl
                                )
                            },
                            onMovieClick = { simMovie ->
                                routeStack = routeStack + MobileRoute.Detail(simMovie)
                            }
                        )
                    }

                    is MobileRoute.Player -> {
                        MobilePlayerScreen(
                            movie = route.movie,
                            episode = route.episode,
                            directStreamUrl = route.streamUrl,
                            onBack = {
                                routeStack = routeStack.dropLast(1)
                            }
                        )
                    }
                }
            }
        }
    }

    // In-App update dialog
    if (updateInfo != null) {
        AlertDialog(
            onDismissRequest = { updateInfo = null },
            containerColor = MochiPlumCard,
            shape = RoundedCornerShape(22.dp),
            title = {
                Text(
                    text = "Nueva Versión Disponible",
                    color = WarmIvory,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Hay una actualización disponible (v${updateInfo?.versionName ?: ""}). ¿Deseas descargarla?",
                    color = SoftSilver,
                    fontFamily = Inter
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        AppUpdateChecker.openDownloadPage(context, updateInfo?.downloadUrl ?: "")
                        updateInfo = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LaMovieRed)
                ) {
                    Text("Descargar APK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { updateInfo = null }) {
                    Text("Más tarde", color = MutedSlate)
                }
            }
        )
    }
}

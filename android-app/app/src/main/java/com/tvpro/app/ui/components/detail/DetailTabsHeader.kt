package com.tvpro.app.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvpro.app.ui.theme.Inter
import com.tvpro.app.ui.theme.MochiCream

@Composable
fun DetailTabsHeader(
    availableTabs: List<DetailTab>,
    selectedTab: DetailTab,
    episodesCount: Int,
    isBackgroundLoading: Boolean,
    primaryActionFocusRequester: FocusRequester,
    episodesTabFocusRequester: FocusRequester,
    similarTabFocusRequester: FocusRequester,
    extrasTabFocusRequester: FocusRequester,
    selectedSeasonFocusRequester: FocusRequester,
    onTabSelected: (DetailTab) -> Unit,
    onScrollToTop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val warmIvory = MochiCream

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            availableTabs.forEach { tab ->
                var isTabFocused by remember { mutableStateOf(false) }
                val isSelected = (selectedTab == tab)
                val tabTitle = when (tab) {
                    DetailTab.EPISODES -> if (isBackgroundLoading && episodesCount == 0) "EPISODIOS" else "EPISODIOS ($episodesCount)"
                    DetailTab.SIMILAR -> "MÁS COMO ESTO"
                    DetailTab.EXTRAS -> "ESCENAS Y DETALLES"
                }

                val tabFocusRequester = when (tab) {
                    DetailTab.EPISODES -> episodesTabFocusRequester
                    DetailTab.SIMILAR -> similarTabFocusRequester
                    DetailTab.EXTRAS -> extrasTabFocusRequester
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .focusRequester(tabFocusRequester)
                        .focusProperties {
                            up = primaryActionFocusRequester
                            if (tab == DetailTab.EPISODES) {
                                down = selectedSeasonFocusRequester
                            }
                        }
                        .onFocusChanged { focusState ->
                            isTabFocused = focusState.isFocused
                            if (focusState.isFocused) {
                                onTabSelected(tab)
                            }
                        }
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionUp) {
                                onScrollToTop()
                                true
                            } else if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionDown && tab == DetailTab.EPISODES) {
                                try {
                                    selectedSeasonFocusRequester.requestFocus()
                                    true
                                } catch (_: Exception) {
                                    false
                                }
                            } else false
                        }
                        .clickable { onTabSelected(tab) }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                when {
                                    isTabFocused -> warmIvory
                                    isSelected -> Color(0xFF1B202C)
                                    else -> Color.Transparent
                                }
                            )
                            .border(
                                width = if (isTabFocused) 2.dp else if (isSelected) 1.2.dp else 1.dp,
                                color = when {
                                    isTabFocused -> warmIvory
                                    isSelected -> warmIvory.copy(alpha = 0.55f)
                                    else -> Color.Transparent
                                },
                                shape = RoundedCornerShape(50)
                            )
                            .padding(horizontal = 18.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = tabTitle,
                            color = when {
                                isTabFocused -> Color.Black
                                isSelected -> warmIvory
                                else -> Color(0xFF8E95A5)
                            },
                            fontSize = 12.sp,
                            fontFamily = Inter,
                            fontWeight = if (isTabFocused || isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            letterSpacing = 1.6.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    // Active underline bar
                    Box(
                        modifier = Modifier
                            .height(3.dp)
                            .width(if (isSelected) 36.dp else 0.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (isTabFocused) warmIvory else warmIvory.copy(alpha = 0.70f))
                    )
                }
            }
        }

        // Divider
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.10f))
        )
    }
}

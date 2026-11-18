package com.tvpro.mobile.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvpro.mobile.ui.theme.*

enum class MobileTab(val label: String, val icon: ImageVector) {
    HOME("Inicio", Icons.Default.Home),
    EXPLORE("Catálogo", Icons.Default.Explore),
    SEARCH("Buscar", Icons.Default.Search),
    MY_LIST("Mi Lista", Icons.Default.Bookmark)
}

@Composable
fun MobileBottomNavBar(
    currentTab: MobileTab,
    onTabSelected: (MobileTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color(0x26FFB7C5),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
        containerColor = Color(0xF5130D19),
        tonalElevation = 0.dp
    ) {
        MobileTab.entries.forEach { tab ->
            val selected = tab == currentTab
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        fontFamily = PlusJakartaSans,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MochiStrawberryPink,
                    selectedTextColor = MochiStrawberryPink,
                    unselectedIconColor = MochiMutedLilac,
                    unselectedTextColor = MochiMutedLilac,
                    indicatorColor = Color(0x33FFB7C5)
                )
            )
        }
    }
}

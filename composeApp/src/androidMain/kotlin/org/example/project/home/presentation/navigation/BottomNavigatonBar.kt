package org.example.project.home.presentation.navigation


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.example.project.theme.IssueSpotTypography
import org.example.project.R
import org.example.project.core.model.home.PostLevel

data class BottomNavItem(
    val level: PostLevel,
    val iconRes: Int,
    val selectedIconRes: Int? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavigationBar(
    currentLevel: PostLevel,
    onLevelChange: (PostLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomNavItem(PostLevel.LOCALITY, R.drawable.ic_location_on),
        BottomNavItem(PostLevel.DISTRICT, R.drawable.ic_business),
        BottomNavItem(PostLevel.STATE, R.drawable.ic_map),
        BottomNavItem(PostLevel.NATIONAL, R.drawable.ic_public)
    )

    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(if (currentLevel == item.level) (item.selectedIconRes ?: item.iconRes) else item.iconRes),
                            contentDescription = item.level.displayName
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.level.displayName,
                            style = IssueSpotTypography.labelSmall
                        )
                    }
                },
                selected = currentLevel == item.level,
                onClick = { onLevelChange(item.level) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

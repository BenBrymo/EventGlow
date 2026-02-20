package com.example.eventglow.user

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AirplaneTicket
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.eventglow.navigation.Routes
import com.example.eventglow.ui.theme.BottomNavGray
import com.example.eventglow.ui.theme.BrandPrimary
import com.example.eventglow.ui.theme.LightGrayText


@Composable
fun UserMainScreen(
    navController: NavController,
    initialEventId: String? = null
) {

    // Use the same NavHostController for both the Scaffold and BottomNavigation
    val bottomNavController = rememberNavController()
    val hasHandledInitialEvent = remember { mutableStateOf(false) }

    LaunchedEffect(initialEventId) {
        if (!hasHandledInitialEvent.value && !initialEventId.isNullOrBlank()) {
            hasHandledInitialEvent.value = true
            bottomNavController.navigate("detailed_event_screen/$initialEventId")
        }
    }

    // Scaffold to provide a basic material layout structure
    Scaffold(
        bottomBar = { BottomNavigationBar(navController = bottomNavController) }
    ) { paddingValues ->
        // Surface to hold main contents with padding applied
        Surface(Modifier.padding(paddingValues)) {
            BottomNavGraph(
                navController = bottomNavController,
                mainNavController = navController // Pass the main NavController
            )
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem("Home", Icons.Default.Home, Routes.USER_MAIN_SCREEN),
        BottomNavItem("Bookmarks", Icons.Default.Bookmarks, Routes.BOOKMARKS_SCREEN),
        BottomNavItem("Tickets", Icons.AutoMirrored.Filled.AirplaneTicket, Routes.TICKETS_SCREEN),
        BottomNavItem("Profile", Icons.Default.AccountCircle, Routes.USER_PROFILE_SCREEN)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = BottomNavGray,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(item.label)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BrandPrimary,
                    selectedTextColor = BrandPrimary,
                    unselectedIconColor = LightGrayText,
                    unselectedTextColor = LightGrayText,
                    indicatorColor = BottomNavGray
                ),
                selected = currentRoute == item.route,
                onClick = {

                    // navigate selected item but don't remove the previous screen from the stack
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = false // Make sure the original screen isn't removed
                        }
                        launchSingleTop = true  // Avoid creating multiple instances of the same destination
                    }
                }
            )
        }
    }
}


@Composable
fun BottomNavGraph(
    navController: NavHostController, //bottom NavController
    mainNavController: NavController //  main NavController
) {
    NavHost(navController = navController, startDestination = Routes.USER_MAIN_SCREEN) {
        composable(Routes.USER_MAIN_SCREEN) {
            userHomeScreen(navController = navController)
        }
        composable(Routes.TICKETS_SCREEN) {
            MyTicketsScreen(navController = navController)
        }
        composable(Routes.USER_PROFILE_SCREEN) {
            ProfileScreen(mainNavController = mainNavController, bottomNavController = navController)
        }

        composable(RoutesUser.FAVOURITE_EVENTS_SCREEN) {
            FavouriteScreen(navController = navController)
        }

        composable(Routes.USER_SEARCH_SCREEN) {
            UserSearchScreen(navController = navController)
        }

        composable(Routes.FILTER_SEARCH_SCREEN) {
            FilterSearchScreen(navController = navController)
        }

        composable(Routes.FILTERED_RESULT_SCREEN) {
            FilteredResultScreen(navController = navController)
        }

        composable(Routes.BOOKMARKS_SCREEN) {
            BookmarksScreen(navController = navController)
        }

        // defines detailed event screen route
        composable(
            route = "detailed_event_screen/{eventId}",
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            DetailedEventScreen(
                navController = navController,
                eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            )
        }

        composable(
            route = "detailed_ticket_screen/{transactionReference}",
            arguments = listOf(navArgument("transactionReference") { type = NavType.StringType })
        ) { backStackEntry ->
            TicketDetailScreen(
                navController = navController,
                transactionReference = backStackEntry.arguments?.getString("transactionReference") ?: "",
            )
        }

        composable(RoutesUser.SETTINGS) {
            UserSettingsScreen(navController = navController)
        }
    }
}

data class BottomNavItem(val label: String, val icon: ImageVector, val route: String)

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun UserMainScreenPreview() {
    UserMainScreen(navController = rememberNavController())
}

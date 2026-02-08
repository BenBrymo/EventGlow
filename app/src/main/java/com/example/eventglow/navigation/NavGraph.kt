package com.example.eventglow.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.eventglow.SplashScreen
import com.example.eventglow.admin_main_screen.AdminMainScreen
import com.example.eventglow.admin_main_screen.AdminProfileScreen
import com.example.eventglow.common.create_account.CreateAccountScreen
import com.example.eventglow.common.email_verification.EmailVerificationScreen
import com.example.eventglow.common.login.LoginScreen
import com.example.eventglow.common.password_reset.PasswordRecoveryScreen
import com.example.eventglow.common.password_reset.PasswordResetConfirmationScreen
import com.example.eventglow.events_management.AdminSearchScreen
import com.example.eventglow.events_management.CopyEventScreen
import com.example.eventglow.events_management.CreateEventScreen
import com.example.eventglow.events_management.DetailedEventScreenAdmin
import com.example.eventglow.events_management.DraftedEventsScreen
import com.example.eventglow.events_management.EditEventScreen
import com.example.eventglow.events_management.EventsMgmtScreen
import com.example.eventglow.events_management.FilterSearchScreenAdmin
import com.example.eventglow.events_management.FilteredResultScreenAdmin
import com.example.eventglow.reportingandanalytics.ReportingAndAnalyticsScreen
import com.example.eventglow.settings.SettingsScreen
import com.example.eventglow.ticket_management.TicketManagementScreen
import com.example.eventglow.user.UserMainScreen
import com.example.eventts.user_management.UserManagementSearchScreen
import com.example.eventts.user_management.UserMgmtScreen

object Routes {
    const val SPLASH_SCREEN = "splash_screen"
    const val LOGIN_SCREEN = "login_screen"
    const val CREATE_ACCOUNT_SCREEN = "create_account_screen"
    const val PASSWORD_RECOVERY_SCREEN = "password_recovery_screen"
    const val PASSWORD_RESET_CONFIRMATION_SCREEN = "password_reset_confirmation_screen"
    const val EMAIL_VERIFICATION_SCREEN = "email_verification_screen"
    const val USER_MAIN_SCREEN = "user_main_screen"
    const val ADMIN_MAIN_SCREEN = "admin_main_screen/{username}"
    const val EVENTS_MANAGEMENT_SCREEN = "events_management_screen"
    const val CREATE_EVENT_SCREEN = "create_event_screen"
    const val ADMIN_PROFILE_SCREEN = "admin_profile_screen"
    const val USER_PROFILE_SCREEN = "user_profile_screen"
    const val BOOKMARKS_SCREEN = "bookmarks_screen"
    const val TICKETS_SCREEN = "tickets_screen"
    const val DRAFTED_EVENTS_SCREEN = "drafted_events_screen"
    const val ADMIN_SEARCH_SCREEN = "admin_search_screen"
    const val USER_SEARCH_SCREEN = "user_search_screen"
    const val FILTER_SEARCH_SCREEN_ADMIN = "filter_search_screen_admin"
    const val FILTER_SEARCH_SCREEN = "filter_search_screen"
    const val FILTERED_RESULT_SCREEN_ADMIN = "filtered_result_screen_admin"
    const val FILTERED_RESULT_SCREEN = "filtered_result_screen"
    const val TICKET_MANAGEMENT_SCREEN = "ticket_management_screen"
    const val USER_MANAGEMENT_SCREEN = "user_management_screen"
    const val REPORTING_AND_ANALYTICS = "reporting_and_analytics"
    const val USER_MANAGEMENT_SEARCH_SCREEN = "user_management_search_screen"
    const val SETTINGS = "settings_screen"
}

@Composable
fun NavGraph(navController: NavHostController) {

    //defines a navhost and sets WelcomeScreen as the startDestination
    NavHost(navController = navController, startDestination = Routes.SPLASH_SCREEN) {
        // defines SPLASH_SCREEN  route
        composable(Routes.SPLASH_SCREEN) {
            SplashScreen(navController = navController)
        }

        // defines login screen route
        composable(Routes.LOGIN_SCREEN) {
            LoginScreen(navController = navController)
        }

        //defines create account screen route
        composable(Routes.CREATE_ACCOUNT_SCREEN) {
            CreateAccountScreen(navController = navController)
        }

        //defines password recovery screen route
        composable(Routes.PASSWORD_RECOVERY_SCREEN) {
            PasswordRecoveryScreen(navController = navController)
        }

        // defines password reset confirmation screen route
        composable(Routes.PASSWORD_RESET_CONFIRMATION_SCREEN) {
            PasswordResetConfirmationScreen(navController = navController)
        }

        // defines password reset confirmation screen route
        composable(Routes.EMAIL_VERIFICATION_SCREEN) {
            EmailVerificationScreen(navController = navController)
        }

        // defines admin main screen route
        composable(Routes.ADMIN_MAIN_SCREEN) {
            AdminMainScreen(navController = navController)
        }

        //defines create admin profile screen route
        composable(Routes.ADMIN_PROFILE_SCREEN) {
            AdminProfileScreen(navController = navController)
        }

        //defines events management screen route
        composable(Routes.EVENTS_MANAGEMENT_SCREEN) {
            EventsMgmtScreen(navController = navController)
        }


        // defines create event screen route
        composable(Routes.CREATE_EVENT_SCREEN) {
            CreateEventScreen(navController = navController)
        }

        // defines user main screen route
        composable(Routes.USER_MAIN_SCREEN) {
            UserMainScreen(navController = navController)
        }

        // defines admin search screen route
        composable(Routes.ADMIN_SEARCH_SCREEN) {
            AdminSearchScreen(navController = navController)
        }

        //defines detailed event admin screen route
        composable(
            route = "detailed_event_screen_admin/{eventId}",
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            DetailedEventScreenAdmin(
                navController = navController,
                eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            )
        }


        composable(Routes.FILTER_SEARCH_SCREEN_ADMIN) {
            FilterSearchScreenAdmin(navController = navController)
        }


        composable(Routes.FILTERED_RESULT_SCREEN_ADMIN) {
            FilteredResultScreenAdmin(navController = navController)
        }


        composable(
            route = "edit_event_screen/{eventId}",
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            EditEventScreen(
                navController = navController,
                eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            )
        }


        composable(Routes.DRAFTED_EVENTS_SCREEN) {
            DraftedEventsScreen(navController = navController)
        }


        composable(
            route = "copy_event_screen/{eventId}",
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            CopyEventScreen(
                navController = navController,
                eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            )
        }

        composable(Routes.TICKET_MANAGEMENT_SCREEN) {
            TicketManagementScreen(navController = navController)
        }


        composable(Routes.USER_MANAGEMENT_SCREEN) {
            UserMgmtScreen(navController = navController)
        }

        composable(Routes.USER_MANAGEMENT_SEARCH_SCREEN) {
            UserManagementSearchScreen(navController = navController)
        }

        composable(Routes.REPORTING_AND_ANALYTICS) {
            ReportingAndAnalyticsScreen()
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(navController = navController)
        }
    }
}

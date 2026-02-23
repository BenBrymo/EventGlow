package com.example.eventglow.notifications

const val ROUTE_DETAILED_EVENT_SCREEN = "detailed_event_screen"
const val ROUTE_DETAILED_EVENT_SCREEN_ADMIN = "detailed_event_screen_admin"
const val ROUTE_ADMIN_MAIN_SCREEN = "admin_main_screen"
const val ROUTE_USER_MAIN_SCREEN = "user_main_screen"

val SUPPORTED_NOTIFICATION_ROUTES = setOf(
    ROUTE_DETAILED_EVENT_SCREEN,
    ROUTE_DETAILED_EVENT_SCREEN_ADMIN,
    ROUTE_ADMIN_MAIN_SCREEN,
    ROUTE_USER_MAIN_SCREEN
)

fun routeRequiresEventId(route: String): Boolean {
    return route == ROUTE_DETAILED_EVENT_SCREEN || route == ROUTE_DETAILED_EVENT_SCREEN_ADMIN
}

fun generalRouteForRole(targetRole: String): String {
    return when (targetRole.trim().lowercase()) {
        "admin" -> ROUTE_ADMIN_MAIN_SCREEN
        "user", "all" -> ROUTE_USER_MAIN_SCREEN
        else -> ROUTE_USER_MAIN_SCREEN
    }
}

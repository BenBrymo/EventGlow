package com.example.eventglow.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AssignmentReturned
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.eventglow.common.help.HelpArticle
import com.example.eventglow.common.help.HelpCenterContent
import com.example.eventglow.common.help.HelpFaq
import com.example.eventglow.common.help.HelpTopic

@Composable
fun AdminHelpCenterScreen(navController: NavController) {
    val topics = listOf(
        HelpTopic(
            title = "Event Management",
            icon = Icons.Default.Event,
            article = HelpArticle(
                title = "Create/Edit/Publish events",
                steps = listOf(
                    "Open Events Management from admin home.",
                    "Use Create Event to add new events, then Review & Publish.",
                    "Use Edit/Copy screens to update or duplicate events."
                )
            )
        ),
        HelpTopic(
            title = "Ticket Management",
            icon = Icons.Default.ConfirmationNumber,
            article = HelpArticle(
                title = "Tickets and verification",
                steps = listOf(
                    "Open Ticket Management to monitor ticket stats.",
                    "Use Scan QR to verify entry tickets.",
                    "Recent scan activity is visible in management tools."
                )
            )
        ),
        HelpTopic(
            title = "Users & Roles",
            icon = Icons.Default.Groups,
            article = HelpArticle(
                title = "Managing users",
                steps = listOf(
                    "Open User Management to fetch and search users.",
                    "Use add-user flow for new admin/user accounts.",
                    "Follow verification and policy checks during account creation."
                )
            )
        ),
        HelpTopic(
            title = "Notifications",
            icon = Icons.Default.Campaign,
            article = HelpArticle(
                title = "Sending push notifications",
                steps = listOf(
                    "Use Notifications screen for general and event-targeted push.",
                    "Set allowed routes/roles in backend allowlists.",
                    "Verify push route behavior in both foreground and background."
                )
            )
        ),
        HelpTopic(
            title = "Analytics & Exports",
            icon = Icons.Default.Analytics,
            article = HelpArticle(
                title = "Reports and export",
                steps = listOf(
                    "Open Analytics screen and apply Daily/Weekly/Monthly/Range filters.",
                    "Use export action to generate XLS/PDF reports.",
                    "Choose save location when exporting files."
                )
            )
        ),
        HelpTopic(
            title = "Refunds",
            icon = Icons.Default.AssignmentReturned,
            article = HelpArticle(
                title = "Refund operations",
                steps = listOf(
                    "Users submit refund requests from ticket detail.",
                    "Admin reviews pending refund requests (management screen phase).",
                    "Approve/reject should be audited with admin note and timestamp."
                )
            )
        ),
        HelpTopic(
            title = "Account Settings",
            icon = Icons.Default.ManageAccounts,
            article = HelpArticle(
                title = "Admin profile and security",
                steps = listOf(
                    "Use Settings for profile updates and password changes.",
                    "Use Support and Help Center for troubleshooting.",
                    "Log out from settings when switching devices."
                )
            )
        )
    )

    val faqs = listOf(
        HelpFaq(
            question = "How do I publish an event?",
            article = HelpArticle(
                title = "Publish event",
                steps = listOf(
                    "Open Create Event.",
                    "Complete all required sections.",
                    "Review and publish from finish section."
                )
            )
        ),
        HelpFaq(
            question = "Why did category creation fail?",
            article = HelpArticle(
                title = "Category permission issue",
                steps = listOf(
                    "Check Firestore rules for eventCategories write access.",
                    "Ensure current account role is admin.",
                    "Retry category creation after auth/session refresh."
                )
            )
        ),
        HelpFaq(
            question = "Why are push notifications not sent?",
            article = HelpArticle(
                title = "Push troubleshooting",
                steps = listOf(
                    "Verify Supabase function deployment and secrets.",
                    "Check ALLOWED_ROUTES and ALLOWED_TARGET_ROLES values.",
                    "Inspect function logs for 400/401/404 errors."
                )
            )
        ),
        HelpFaq(
            question = "How do I export analytics reports?",
            article = HelpArticle(
                title = "Export reports",
                steps = listOf(
                    "Apply filter in Analytics screen.",
                    "Open export action from top bar.",
                    "Choose format and save destination."
                )
            )
        )
    )

    HelpCenterContent(
        title = "Help Center",
        topics = topics,
        faqs = faqs,
        onBack = { navController.popBackStack() },
        backIcon = Icons.AutoMirrored.Filled.ArrowBack
    )
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun AdminHelpCenterScreenPreview() {
    AdminHelpCenterScreen(navController = rememberNavController())
}

package com.example.eventglow.user

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AirplaneTicket
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AssignmentReturned
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.eventglow.common.help.HelpArticle
import com.example.eventglow.common.help.HelpCenterContent
import com.example.eventglow.common.help.HelpFaq
import com.example.eventglow.common.help.HelpTopic

@Composable
fun UserHelpCenterScreen(navController: NavController) {
    val topics = listOf(
        HelpTopic(
            title = "Account & Profile",
            icon = Icons.Default.AccountCircle,
            article = HelpArticle(
                title = "Manage your profile",
                steps = listOf(
                    "Open Profile tab in bottom navigation.",
                    "Go to Settings for profile updates and password changes.",
                    "Use Support if you need account help."
                )
            )
        ),
        HelpTopic(
            title = "Find Events",
            icon = Icons.Default.Event,
            article = HelpArticle(
                title = "Finding events quickly",
                steps = listOf(
                    "Browse Home sections: Featured, Live, Today, and Upcoming.",
                    "Tap the search bar to open Search screen.",
                    "Use Advanced Filters for date/category/status selection."
                )
            )
        ),
        HelpTopic(
            title = "Tickets",
            icon = Icons.AutoMirrored.Filled.AirplaneTicket,
            article = HelpArticle(
                title = "Buying and using tickets",
                steps = listOf(
                    "Open event details and pick a ticket type.",
                    "Pay (or claim free ticket if available).",
                    "Open Tickets tab, then open ticket details to view/download QR."
                )
            )
        ),
        HelpTopic(
            title = "Payments",
            icon = Icons.Default.Payment,
            article = HelpArticle(
                title = "Payment details",
                steps = listOf(
                    "Transactions screen shows your payment history.",
                    "Amounts are displayed in major currency format.",
                    "If payment fails, retry from event purchase flow."
                )
            )
        ),
        HelpTopic(
            title = "Refunds",
            icon = Icons.Default.AssignmentReturned,
            article = HelpArticle(
                title = "Requesting refunds",
                steps = listOf(
                    "Open Ticket Details for a paid successful ticket.",
                    "Use Request Refund and submit your reason.",
                    "Track approval outcome from app notifications and updates."
                )
            )
        ),
        HelpTopic(
            title = "Bookmarks",
            icon = Icons.Default.Bookmarks,
            article = HelpArticle(
                title = "Save and revisit events",
                steps = listOf(
                    "Bookmark or favorite events from event listings/details.",
                    "Open Bookmarks to revisit saved events."
                )
            )
        ),
        HelpTopic(
            title = "Notifications",
            icon = Icons.Default.Notifications,
            article = HelpArticle(
                title = "Push notifications",
                steps = listOf(
                    "Enable notifications in Settings.",
                    "Open notifications to jump to event or main screens.",
                    "If app is open, ensure notification routing is enabled."
                )
            )
        )
    )

    val faqs = listOf(
        HelpFaq(
            question = "How do I buy a ticket?",
            article = HelpArticle(
                title = "Buy ticket",
                steps = listOf(
                    "Open an event detail screen.",
                    "Select a ticket type.",
                    "Complete payment or claim free ticket."
                )
            )
        ),
        HelpFaq(
            question = "Why can't I buy a ticket?",
            article = HelpArticle(
                title = "Purchase blocked",
                steps = listOf(
                    "Event might be ongoing or ended.",
                    "Ticket type could be sold out.",
                    "You may have already purchased a ticket for that event."
                )
            )
        ),
        HelpFaq(
            question = "How do I request a refund?",
            article = HelpArticle(
                title = "Refund request",
                steps = listOf(
                    "Open Ticket Details.",
                    "Tap Request Refund.",
                    "Submit a short reason and wait for review."
                )
            )
        ),
        HelpFaq(
            question = "Where can I find my QR code?",
            article = HelpArticle(
                title = "QR code location",
                steps = listOf(
                    "Go to Tickets tab.",
                    "Open the ticket detail.",
                    "View QR code and use Download QR if needed."
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
fun HelpCenterScreenPreview() {
    UserHelpCenterScreen(navController = rememberNavController())
}

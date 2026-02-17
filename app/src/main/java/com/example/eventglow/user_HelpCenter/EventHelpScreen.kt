package com.example.eventglow.user_HelpCenter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun EventHelpScreen(navController: NavController) {
    val faqList = listOf(
        "How can I find upcoming events?",
        "How do I register for an event?",
        "What should I bring to an event?",
        "How do I receive event notifications?",
        "Can I invite others to join an event?"
    )

    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val coroutineScope = rememberCoroutineScope()
    var selectedFaq by remember { mutableStateOf<String?>(null) }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            selectedFaq?.let { faq ->
                EventFAQDetail(faq) {
                    coroutineScope.launch { sheetState.hide() }
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Event Help",
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            faqList.forEach { faq ->
                Text(
                    text = faq,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            selectedFaq = faq
                            coroutineScope.launch { sheetState.show() }
                        }
                )
                Divider()
            }
        }
    }
}


@Composable
fun EventFAQDetail(faq: String, onDismiss: () -> Unit) {
    val answer = when (faq) {
        "How can I find upcoming events?" -> "You can find upcoming events by browsing our events section, which is regularly updated with new events."
        "How do I register for an event?" -> "To register, go to the event page and click on the 'Register' button. Follow the on-screen instructions to complete registration."
        "What should I bring to an event?" -> "For most events, you should bring your ticket (digital or printed), ID, and any other specified items on the event page."
        "How do I receive event notifications?" -> "Make sure to enable notifications in your app settings, and we'll keep you informed about upcoming events and updates."
        "Can I invite others to join an event?" -> "Yes, you can share event details with others directly from the event page by clicking the 'Invite' or 'Share' button."
        else -> "This is a sample answer for any FAQ question."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(text = faq, style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = answer, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Close")
        }
    }
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun EventHelpScreenPreview() {
    EventHelpScreen(navController = rememberNavController())
}

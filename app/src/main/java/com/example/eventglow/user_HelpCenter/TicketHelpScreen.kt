package com.example.eventglow.user_HelpCenter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun TicketScreen(navController: NavController) {
    val faqList = listOf(
        "How do I purchase a ticket?",
        "Can I transfer my ticket to someone else?",
        "What should I do if I lose my ticket?",
        "Are there any discounts available?",
        "What if I miss the event?",
    )

    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val coroutineScope = rememberCoroutineScope()
    var selectedFaq by remember { mutableStateOf<String?>(null) }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            selectedFaq?.let { faq ->
                TicketFAQDetail(faq) {
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
                text = "Ticket Help",
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
fun TicketFAQDetail(faq: String, onDismiss: () -> Unit) {
    val answer = when (faq) {
        "How do I purchase a ticket?" -> "You can purchase a ticket through the app by selecting the event, choosing your ticket tier, and proceeding with the payment process."
        "Can I transfer my ticket to someone else?" -> "Yes, tickets can be transferred to another person through the app. Please contact support if you need assistance with this process."
        "What should I do if I lose my ticket?" -> "If you've lost your ticket, you can retrieve it from the 'My Tickets' section in the app."
        "Are there any discounts available?" -> "Discounts are sometimes offered for early-bird purchases or during promotional periods. Keep an eye on our announcements for discount opportunities."
        "What if I miss the event?" -> "If you miss the event, refunds are generally not provided. Please review the refund policy for more information."
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

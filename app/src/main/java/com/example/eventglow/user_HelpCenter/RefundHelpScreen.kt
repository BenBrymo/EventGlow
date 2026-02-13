package com.example.eventglow.user_HelpCenter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun RefundScreen(navController: NavController) {
    val faqList = listOf(
        "How do I request a refund?",
        "What is the refund policy for missed events?",
        "How long does it take to process a refund?",
        "Can I get a full refund?",
        "What if the event is canceled?"
    )

    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val coroutineScope = rememberCoroutineScope()
    var selectedFaq by remember { mutableStateOf<String?>(null) }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            selectedFaq?.let { faq ->
                RefundFAQDetail(faq) {
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
                text = "Refund Help",
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
fun RefundFAQDetail(faq: String, onDismiss: () -> Unit) {
    val answer = when (faq) {
        "How do I request a refund?" -> "You can request a refund by going to your ticket details and selecting the 'Request Refund' option. Note that certain conditions apply."
        "What is the refund policy for missed events?" -> "Generally, refunds are not issued for missed events. Please refer to the refund policy for details."
        "How long does it take to process a refund?" -> "Refunds are typically processed within 5-10 business days. Delays may occur depending on the payment method."
        "Can I get a full refund?" -> "Full refunds are available only under certain conditions. Please review the refund policy or contact support for assistance."
        "What if the event is canceled?" -> "If the event is canceled, you are eligible for a full refund. We will notify you with further instructions."
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

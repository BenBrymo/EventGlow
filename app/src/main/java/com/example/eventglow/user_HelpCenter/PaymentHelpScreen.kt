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
fun PaymentHelpScreen(navController: NavController) {
    val faqList = listOf(
        "What payment methods are accepted?",
        "Why was my payment declined?",
        "How do I update my payment information?",
        "Will I receive a payment receipt?",
        "What should I do if I was charged twice?"
    )

    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val coroutineScope = rememberCoroutineScope()
    var selectedFaq by remember { mutableStateOf<String?>(null) }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            selectedFaq?.let { faq ->
                PaymentFAQDetail(faq) {
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
                text = "Payment Help",
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
fun PaymentFAQDetail(faq: String, onDismiss: () -> Unit) {
    val answer = when (faq) {
        "What payment methods are accepted?" -> "We accept credit/debit cards, mobile payments, and various online payment methods. Check your payment screen for all options."
        "Why was my payment declined?" -> "Payments may be declined due to insufficient funds, incorrect card details, or restrictions by your bank. Please verify your information or contact your bank."
        "How do I update my payment information?" -> "You can update your payment information by going to your profile settings and selecting 'Payment Methods'."
        "Will I receive a payment receipt?" -> "Yes, you will receive a digital receipt via email after a successful transaction."
        "What should I do if I was charged twice?" -> "If you notice a duplicate charge, please contact our support team immediately for assistance in resolving the issue."
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
fun PaymentHelpScreenPreview() {
    PaymentHelpScreen(navController = rememberNavController())
}

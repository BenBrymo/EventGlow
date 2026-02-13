package com.example.eventglow.user

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AirplaneTicket
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AssignmentReturned
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch


private val TAG = "HelpCenterScreen"

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HelpCenterScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    var selectedFaq by remember { mutableStateOf<String?>(null) }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            selectedFaq?.let { faq ->
                FAQDetailBottomSheet(faq)
            }
        },
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetBackgroundColor = MaterialTheme.colorScheme.surface,
        scrimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Help Center", style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = {
                        IconButton(onClick = {
                            Log.d(TAG, "Back button pressed")
                            navController.popBackStack()
                        }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                item { SearchBar() }
                item { HelpTopicsList(navController) }
                item { Spacer(modifier = Modifier.height(32.dp)) }
                item {
                    FaqList { faq ->
                        selectedFaq = faq
                        scope.launch { sheetState.show() }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar() {
    var searchText by remember { mutableStateOf("") }

    OutlinedTextField(
        value = searchText,
        onValueChange = { searchText = it },
        placeholder = { Text("Search for help...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
        shape = RoundedCornerShape(corner = CornerSize(12.dp)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}

@Composable
private fun HelpTopicsList(navController: NavController) {
    val topics = listOf(
        Triple("Account", Icons.Default.AccountCircle, "help_topic_account_screen"),
        Triple("Ticket", Icons.AutoMirrored.Filled.AirplaneTicket, "help_topic_ticket_screen"),
        Triple("Refund", Icons.Default.AssignmentReturned, "help_topic_refund_screen"),
        Triple("Payment", Icons.Default.Payment, "help_topic_payment_screen"),
        Triple("Event", Icons.Default.Event, "help_topic_event_screen"),
        Triple("Contact support", Icons.Default.SupportAgent, "help_topic_support_screen")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        topics.forEach { (name, icon, route) ->
            HelpTopicItem(topic = name, icon = icon) {
                navController.navigate(route)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HelpTopicItem(topic: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(corner = CornerSize(12.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = topic,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
fun FaqList(onFaqClick: (String) -> Unit) {
    val faqs = listOf(
        "How to book a ticket?",
        "How do I get a refund?",
        "I didn't receive my ticket, what should I do?"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Frequently Asked Questions",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            faqs.forEach { faq ->
                HelpFaqItem(faq, onClick = { onFaqClick(faq) })
            }
        }
    }
}

@Composable
fun HelpFaqItem(faq: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Icon(
            Icons.AutoMirrored.Filled.HelpOutline,
            contentDescription = "FAQ",
            tint = MaterialTheme.colorScheme.tertiaryContainer
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = faq, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
fun FAQDetailBottomSheet(faq: String) {
    val steps = when (faq) {
        "How to book a ticket?" -> listOf(
            "Step 1: Open the app",
            "Step 2: Select the event",
            "Step 3: Choose your ticket and complete the payment"
        )

        "How do I get a refund?" -> listOf(
            "Step 1: Go to 'My Tickets'",
            "Step 2: Select the ticket",
            "Step 3: Request a refund"
        )

        "I didn't receive my ticket, what should I do?" -> listOf(
            "Step 1: Check your email",
            "Step 2: Contact support if not received within 24 hours"
        )

        else -> listOf("No steps available")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = faq,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        steps.forEach { step ->
            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 8.dp),
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = step,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HelpCenterScreenPreview() {
    HelpCenterScreen(navController = rememberNavController())
}



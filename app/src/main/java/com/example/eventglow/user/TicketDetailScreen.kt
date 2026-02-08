package com.example.eventglow.user

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.eventglow.dataClass.Transaction
import com.example.eventglow.ticket_management.TicketManagementViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketDetailScreen(
    transactionReference: String,
    navController: NavController,
    ticketManagementViewModel: TicketManagementViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel()
) {

    val ticket = userViewModel.getBoughtTicketByReference(transactionReference)
    Log.d("TicketDetailScreen", "Ticket fetched: $ticket")

    var transaction = Transaction()

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (ticket != null) {
            scope.launch { transaction = ticketManagementViewModel.getTransactionByReference(transactionReference)!! }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Ticket Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Colorful header row with ticket reference
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF56CCF2), Color(0xFF2F80ED))
                        )
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ticket Reference",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = transaction.id,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card with event details
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF))
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Event Name: ${ticket?.eventName}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Organizer: ${ticket?.eventOrganizer}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Date: ${ticket?.startDate} - ${ticket?.endDate}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Price: GHS ${ticket?.ticketPrice}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card with transaction details
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Transaction Details",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    // Add more transaction details if available
                    Text(
                        text = transaction.createdAt,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = transaction.paidAt,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}

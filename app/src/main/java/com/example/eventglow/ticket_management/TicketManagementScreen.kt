package com.example.eventglow.ticket_management

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun TicketManagementScreen(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Text(
            text = "Ticket Management",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        SearchBar(
            searchQuery = searchQuery,
            onSearchQueryChanged = { searchQuery = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Event List with Tickets
        EventTicketList(navController, searchQuery)

        Spacer(modifier = Modifier.height(16.dp))

        // Sales Analytics Section
        SalesAnalytics()

        Spacer(modifier = Modifier.height(16.dp))

        // Add New Event Button
        Button(
            onClick = {
                // Navigate to event creation screen
                navController.navigate("create_event_screen")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Event"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add New Event")
        }
    }
}

@Composable
fun SearchBar(searchQuery: String, onSearchQueryChanged: (String) -> Unit) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        BasicTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            decorationBox = { innerTextField ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        innerTextField()
                    }
                }
            }
        )
    }
}

@Composable
fun EventTicketList(navController: NavController, searchQuery: String) {
    // Replace with your data source
    val events = listOf(
        Event(
            "Music Concert",
            "August 25, 2024",
            listOf(TicketTier("VIP", 200.0, 50), TicketTier("General", 100.0, 150))
        ),
        Event("Art Exhibition", "September 10, 2024", listOf(TicketTier("Standard", 50.0, 100))),
        // More sample events...
    ).filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.date.contains(searchQuery, ignoreCase = true)
    }

    Column {
        events.forEach { event ->
            EventTicketCard(navController, event)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun EventTicketCard(navController: NavController, event: Event) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = event.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = event.date,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            event.ticketTiers.forEach { tier ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${tier.name}: ${tier.availableTickets} available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$${tier.price}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = {
                    // Navigate to edit event screen
                    navController.navigate("edit_event_screen/${event.name}")
                }) {
                    Text("Edit Event")
                }
                TextButton(onClick = {
                    // Handle delete action
                }) {
                    Text("Delete Event")
                }
            }
        }
    }
}

@Composable
fun SalesAnalytics() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Sales Analytics",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Total Sales",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$12,000",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Tickets Sold",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "450",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// Sample data classes (replace with your models)
data class Event(val name: String, val date: String, val ticketTiers: List<TicketTier>)
data class TicketTier(val name: String, val price: Double, val availableTickets: Int)

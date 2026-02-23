package com.example.eventglow.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.eventglow.events_management.EventsManagementViewModel
import com.example.eventglow.ui.theme.Divider
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    navController: NavController,
    senderViewModel: FirestoreNotificationSenderViewModel = viewModel(),
    eventsViewModel: EventsManagementViewModel = viewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val events by eventsViewModel.events.collectAsState()
    val isSending by senderViewModel.isSending.collectAsState()

    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var targetRole by remember { mutableStateOf("all") }
    var notificationType by remember { mutableStateOf("general") }
    var eventSearchQuery by remember { mutableStateOf("") }
    var selectedEventId by remember { mutableStateOf<String?>(null) }
    var selectedEventName by remember { mutableStateOf<String?>(null) }

    val matchingEvents = remember(events, eventSearchQuery) {
        val query = eventSearchQuery.trim()
        if (query.isBlank()) {
            emptyList()
        } else {
            events.filter {
                it.eventName.contains(query, ignoreCase = true) ||
                        it.id.contains(query, ignoreCase = true)
            }.take(10)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Compose Push Notification",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Body") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Text("Target Role", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("all", "user", "admin").forEach { role ->
                        FilterChip(
                            selected = targetRole == role,
                            onClick = { targetRole = role },
                            label = { Text(role) }
                        )
                    }
                }
            }
            item {
                Text("Notification Type", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = notificationType == "general",
                        onClick = {
                            notificationType = "general"
                            selectedEventId = null
                            selectedEventName = null
                        },
                        label = { Text("General") }
                    )
                    FilterChip(
                        selected = notificationType == "event",
                        onClick = { notificationType = "event" },
                        label = { Text("Event") }
                    )
                }
            }

            if (notificationType == "event") {
                item {
                    OutlinedTextField(
                        value = eventSearchQuery,
                        onValueChange = { eventSearchQuery = it },
                        label = { Text("Search Event by name or id") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (!selectedEventId.isNullOrBlank()) {
                    item {
                        AssistChip(
                            onClick = { },
                            label = { Text("Selected: ${selectedEventName ?: selectedEventId}") }
                        )
                    }
                }
                items(matchingEvents) { event ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedEventId = event.id
                                selectedEventName = event.eventName
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(text = event.eventName, style = MaterialTheme.typography.bodyLarge)
                        Text(text = event.id, style = MaterialTheme.typography.bodySmall)
                    }
                    Divider(color = Divider)
                }
            }

            item {
                TextButton(
                    enabled = !isSending,
                    onClick = {
                        val route = if (notificationType == "event") {
                            ROUTE_DETAILED_EVENT_SCREEN
                        } else {
                            generalRouteForRole(targetRole)
                        }
                        val eventId = if (notificationType == "event") selectedEventId else null
                        senderViewModel.sendNotificationToRole(
                            title = title,
                            body = body,
                            targetRole = targetRole,
                            route = route,
                            eventId = eventId
                        ) { success, message ->
                            val feedback = if (success) {
                                "Notification sent."
                            } else {
                                message ?: "Failed to send notification."
                            }
                            scope.launch {
                                snackbarHostState.showSnackbar(feedback)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isSending) "Sending..." else "Send Notification")
                }
            }
        }
    }
}

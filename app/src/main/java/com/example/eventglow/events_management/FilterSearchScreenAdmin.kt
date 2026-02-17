package com.example.eventglow.events_management

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.eventglow.navigation.Routes
import java.util.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSearchScreenAdmin(
    navController: NavController,
    viewModel: EventsManagementViewModel = viewModel(),
) {
    val context = LocalContext.current

    var selectedStatus by remember { mutableStateOf("") }
    var selectedCategories by remember { mutableStateOf<Set<String>>(emptySet()) }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var matchAllCriteria by remember { mutableStateOf(false) }

    // Function to show date picker
    val showDatePicker = remember { mutableStateOf(false) }
    val datePickerType = remember { mutableStateOf("start") } // Use `remember` to persist state

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Filter Events", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },

        ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val filteredEvents = viewModel.filteredEventsAdvanced.collectAsState().value
            Log.d("FilteredSearchScreen", "Number of events returned from viewModel: ${filteredEvents.size}")
            FilterSearchFormContent(
                selectedStatus = selectedStatus,
                selectedCategories = selectedCategories,
                startDate = startDate,
                endDate = endDate,
                matchAllCriteria = matchAllCriteria,
                onStatusChange = { selectedStatus = it },
                onCategoryToggle = { category ->
                    selectedCategories = if (selectedCategories.contains(category)) {
                        selectedCategories - category
                    } else {
                        selectedCategories + category
                    }
                },
                onStartDateClick = {
                    datePickerType.value = "start"
                    showDatePicker.value = true
                },
                onEndDateClick = {
                    datePickerType.value = "end"
                    showDatePicker.value = true
                },
                onMatchAllChange = { matchAllCriteria = it },
                onSearchClick = {
                    viewModel.filterEventsAdvanced(
                        eventStatus = selectedStatus,
                        eventCategories = selectedCategories.toList(),
                        eventStartDate = startDate,
                        eventEndDate = endDate,
                        matchAllCriteria = matchAllCriteria
                    )
                    navController.navigate(Routes.FILTERED_RESULT_SCREEN_ADMIN)
                }
            )
        }

    }

    // Shows date picker is showDatePicker is true
    if (showDatePicker.value) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = android.app.DatePickerDialog(
            context,
            { _, year, month, day ->
                val newDate = "$day/${month + 1}/$year"
                if (datePickerType.value == "start") {
                    startDate = newDate
                } else {
                    endDate = newDate
                }
                showDatePicker.value = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.setOnCancelListener {
            // Reset the date picker type when canceled
            datePickerType.value = "start"
        }
        // Set the minimum date to the current date
        datePickerDialog.datePicker.minDate = calendar.timeInMillis //sets mininum date to current date
        datePickerDialog.show()
        showDatePicker.value = false    // Reset the state after showing the date picker
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSearchFormContent(
    selectedStatus: String,
    selectedCategories: Set<String>,
    startDate: String,
    endDate: String,
    matchAllCriteria: Boolean,
    onStatusChange: (String) -> Unit,
    onCategoryToggle: (String) -> Unit,
    onStartDateClick: () -> Unit,
    onEndDateClick: () -> Unit,
    onMatchAllChange: (Boolean) -> Unit,
    onSearchClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text("Event Status", style = MaterialTheme.typography.titleMedium)
            Box {
                val eventStatusOptions = listOf("Upcoming", "Ongoing", "Completed", "Cancelled")
                var expanded by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = selectedStatus,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Event Status") },
                    trailingIcon = {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Drop Down")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    eventStatusOptions.forEach { status ->
                        DropdownMenuItem(
                            onClick = {
                                onStatusChange(status)
                                expanded = false
                            },
                            text = { Text(text = status) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            val eventCategories = listOf("Music", "Sports", "Tech", "Arts", "Health", "Other")
            Text("Event Category", style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.SpaceEvenly) {
                for (eventCategory in eventCategories) {
                    Button(
                        onClick = { onCategoryToggle(eventCategory) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedCategories.contains(eventCategory)) Color.Blue else Color.Gray
                        ),
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(text = eventCategory, color = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            Text("Start Date", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = startDate,
                onValueChange = {},
                readOnly = true,
                label = { Text("Select Start Date") },
                trailingIcon = {
                    IconButton(onClick = onStartDateClick) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Pick Start Date")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        item {
            Text("End Date", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = endDate,
                onValueChange = {},
                readOnly = true,
                label = { Text("Select End Date") },
                trailingIcon = {
                    IconButton(onClick = onEndDateClick) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Pick End Date")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
            ) {
                Checkbox(
                    checked = matchAllCriteria,
                    onCheckedChange = onMatchAllChange
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Results should contain all selected criteria", style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            Button(
                onClick = onSearchClick,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text("Search")
            }
        }
    }
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun FilterSearchFormContentPreview() {
    FilterSearchFormContent(
        selectedStatus = "Upcoming",
        selectedCategories = setOf("Music", "Tech"),
        startDate = "16/2/2026",
        endDate = "18/2/2026",
        matchAllCriteria = true,
        onStatusChange = {},
        onCategoryToggle = {},
        onStartDateClick = {},
        onEndDateClick = {},
        onMatchAllChange = {},
        onSearchClick = {}
    )
}


data class EventFilters(
    val status: String?,
    val category: String?,
    val startDate: String?,
    val endDate: String?,
    val matchAll: Boolean
)

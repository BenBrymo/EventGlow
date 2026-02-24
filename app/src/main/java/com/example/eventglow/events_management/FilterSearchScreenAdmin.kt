package com.example.eventglow.events_management

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.eventglow.navigation.Routes
import com.example.eventts.dataClass.EventFilterCriteria
import java.util.*


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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

    val eventCategories by viewModel.eventCategories.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Filter Events", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            selectedStatus = ""
                            selectedCategories = emptySet()
                            startDate = ""
                            endDate = ""
                            matchAllCriteria = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset")
                    }
                    Button(
                        onClick = {
                            val criteria = EventFilterCriteria(
                                eventStatus = selectedStatus,
                                eventCategories = selectedCategories.toList(),
                                eventStartDate = startDate,
                                eventEndDate = endDate,
                                matchAllCriteria = matchAllCriteria
                            )
                            viewModel.filterEventsAdvanced(criteria)
                            navController.navigate(Routes.FILTERED_RESULT_SCREEN_ADMIN)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Apply Filters")
                    }
                }
            }
        }
    ) { paddingValues ->
        val filteredEvents = viewModel.filteredEventsAdvanced.collectAsState().value
        Log.d("FilteredSearchScreen", "Number of events returned from viewModel: ${filteredEvents.size}")
        FilterSearchFormContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            selectedStatus = selectedStatus,
            selectedCategories = selectedCategories,
            startDate = startDate,
            endDate = endDate,
            matchAllCriteria = matchAllCriteria,
            eventCategoryOptions = eventCategories.map { it.name }.ifEmpty {
                listOf("Music", "Sports", "Tech", "Arts", "Health", "Other")
            },
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
            onMatchAllChange = { matchAllCriteria = it }
        )

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FilterSearchFormContent(
    modifier: Modifier = Modifier,
    selectedStatus: String,
    selectedCategories: Set<String>,
    startDate: String,
    endDate: String,
    matchAllCriteria: Boolean,
    eventCategoryOptions: List<String>,
    onStatusChange: (String) -> Unit,
    onCategoryToggle: (String) -> Unit,
    onStartDateClick: () -> Unit,
    onEndDateClick: () -> Unit,
    onMatchAllChange: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "Discover your events",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pick filters to narrow down event results quickly.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            Text("Event Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(10.dp))
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
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
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
            Text("Categories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(eventCategoryOptions, key = { it }) { eventCategory ->
                    FilterChip(
                        selected = selectedCategories.contains(eventCategory),
                        onClick = { onCategoryToggle(eventCategory) },
                        label = { Text(eventCategory) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            Text("Date Range", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(10.dp))
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
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
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
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Match Mode",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "All criteria must match for stricter results.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = matchAllCriteria,
                        onCheckedChange = onMatchAllChange
                    )
                }
            }
            Spacer(modifier = Modifier.height(88.dp))
        }
    }
}

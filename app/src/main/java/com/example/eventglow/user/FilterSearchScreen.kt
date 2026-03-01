package com.example.eventglow.user

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.eventglow.dataClass.Event
import com.example.eventglow.dataClass.UserPreferences
import com.example.eventglow.navigation.Routes
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FilterSearchScreen(
    navController: NavController,
    userViewModel: UserViewModel = viewModel(),
    userPreferences: UserPreferences = viewModel()
) {
    val context = LocalContext.current
    val allEvents by userViewModel.events.collectAsState()
    val eventCategories by userViewModel.eventCategories.collectAsState()

    var selectedStatus by remember { mutableStateOf("") }
    var selectedCategories by remember { mutableStateOf<Set<String>>(emptySet()) }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var matchAllCriteria by remember { mutableStateOf(false) }

    val showDatePicker = remember { mutableStateOf(false) }
    val datePickerType = remember { mutableStateOf("start") }

    LaunchedEffect(Unit) {
        userViewModel.fetchEvents()
        userViewModel.fetchEventCategories()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(text = "Filter Events", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp, shadowElevation = 8.dp) {
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
                            val filtered = applyUserFilters(
                                events = allEvents,
                                status = selectedStatus,
                                categories = selectedCategories,
                                startDate = startDate,
                                endDate = endDate,
                                matchAllCriteria = matchAllCriteria
                            )
                            Log.d("FilterSearchScreen", "Filtered events count: ${filtered.size}")
                            userPreferences.keepFilteredEventsInSharedPref(filtered)
                            navController.navigate(Routes.FILTERED_RESULT_SCREEN)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Apply Filters")
                    }
                }
            }
        }
    ) { paddingValues ->
        FilterSearchFormContentUser(
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
            datePickerType.value = "start"
        }
        datePickerDialog.show()
        showDatePicker.value = false
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun FilterSearchFormContentUser(
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
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
            Spacer(modifier = Modifier.height(14.dp))
        }

        item {
            Text("Event Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(10.dp))
            Box {
                val eventStatusOptions = listOf("Upcoming", "Ongoing", "Ended", "Cancelled")
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
            Spacer(modifier = Modifier.height(12.dp))
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
            Spacer(modifier = Modifier.height(12.dp))
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
            Spacer(modifier = Modifier.height(12.dp))
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

private fun applyUserFilters(
    events: List<Event>,
    status: String,
    categories: Set<String>,
    startDate: String,
    endDate: String,
    matchAllCriteria: Boolean
): List<Event> {
    val normalizedStatus = status.trim().lowercase(Locale.getDefault())
    val normalizedCategories = categories.map { it.trim().lowercase(Locale.getDefault()) }.toSet()
    val normalizedStart = startDate.trim()
    val normalizedEnd = endDate.trim()

    val hasStatus = normalizedStatus.isNotBlank()
    val hasCategories = normalizedCategories.isNotEmpty()
    val hasStart = normalizedStart.isNotBlank()
    val hasEnd = normalizedEnd.isNotBlank()

    return events.filter { event ->
        val statusMatch = !hasStatus || event.eventStatus.trim().equals(normalizedStatus, ignoreCase = true)
        val categoryMatch = !hasCategories || normalizedCategories.contains(
            event.eventCategory.trim().lowercase(Locale.getDefault())
        )
        val startMatch = !hasStart || event.startDate.trim() == normalizedStart
        val endMatch = !hasEnd || event.endDate.trim() == normalizedEnd

        if (matchAllCriteria) {
            statusMatch && categoryMatch && startMatch && endMatch
        } else {
            val matches = mutableListOf<Boolean>()
            if (hasStatus) matches += statusMatch
            if (hasCategories) matches += categoryMatch
            if (hasStart) matches += startMatch
            if (hasEnd) matches += endMatch
            matches.any { it }
        }
    }
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun FilterSearchScreenPreview() {
    FilterSearchScreen(navController = rememberNavController())
}

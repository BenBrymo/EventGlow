package com.example.eventglow.reportingandanalytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eventglow.ui.theme.AccentOrange
import com.example.eventglow.ui.theme.BackgroundBlack
import com.example.eventglow.ui.theme.CardBlack
import com.example.eventglow.ui.theme.FieldBorderGray
import com.example.eventglow.ui.theme.HintGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: ReportingAnalyticsViewModel = viewModel()
) {
    val summary by viewModel.summary.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val rows by viewModel.rows.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var showDetailed by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BackgroundBlack,
        bottomBar = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundBlack)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Button(
                    onClick = { showDetailed = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                ) {
                    Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Detailed")
                }
                TextButton(
                    onClick = viewModel::refresh,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Refresh")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundBlack)
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Reporting & Analytics", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text(
                        text = "GHS ${String.format("%.2f", summary.totalRevenue)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF4CAF50)
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    placeholder = { Text("Search events", color = HintGray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = HintGray) },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FieldBorderGray,
                        unfocusedBorderColor = FieldBorderGray,
                        focusedContainerColor = Color(0xFF2A2A2A),
                        unfocusedContainerColor = Color(0xFF2A2A2A),
                        cursorColor = AccentOrange
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = BackgroundBlack,
                    contentColor = AccentOrange,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = AccentOrange,
                            height = 3.dp
                        )
                    }
                ) {
                    listOf("Ongoing Events", "Ended Events").forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { viewModel.onTabSelected(index) },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (selectedTab == index) AccentOrange else HintGray
                                )
                            }
                        )
                    }
                }
            }

            if (errorMessage != null) {
                item {
                    Text(errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AccentOrange)
                    }
                }
            } else if (rows.isEmpty()) {
                item {
                    Text("No events found.", color = HintGray)
                }
            } else {
                items(rows) { row ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBlack),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                                Text(row.title, color = Color.White, style = MaterialTheme.typography.titleMedium)
                                Text(row.date, color = HintGray, style = MaterialTheme.typography.bodySmall)
                            }
                            androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "Sold ${row.sold}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "GHS ${String.format("%.2f", row.revenue)}",
                                    color = AccentOrange,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDetailed) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showDetailed = false },
            containerColor = BackgroundBlack
        ) {
            DetailedAnalyticsScreen(
                viewModel = viewModel,
                onClose = { showDetailed = false }
            )
        }
    }
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun AnalyticsScreenPreview() {
    AnalyticsScreen()
}

package com.example.eventglow.reportingandanalytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.eventglow.ui.theme.AccentOrange
import com.example.eventglow.ui.theme.BackgroundBlack
import com.example.eventglow.ui.theme.FieldBorderGray
import com.example.eventglow.ui.theme.HintGray


@Composable
fun DetailedAnalyticsScreen() {

    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack)
            .padding(horizontal = 20.dp)
    ) {

        Spacer(modifier = Modifier.height(16.dp))

        AnalyticsTopBar()

        Spacer(modifier = Modifier.height(20.dp))

        AnalyticsTabs(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )

        Spacer(modifier = Modifier.height(20.dp))

        StatsRow()

        Spacer(modifier = Modifier.height(20.dp))

        // Conditional Filters
        when (selectedTab) {
            2 -> MonthlyFilter() // Monthly
            3 -> RangeFilter()   // Range
        }

        Spacer(modifier = Modifier.height(24.dp))

        ChartPlaceholder()

        Spacer(modifier = Modifier.height(32.dp))

        ExportButton()

        Spacer(modifier = Modifier.height(24.dp))
    }
}


@Composable
fun AnalyticsTopBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = "Detailed Analytics",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )

        IconButton(onClick = { }) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}


@Composable
fun AnalyticsTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {

    val tabs = listOf("Daily", "Weekly", "Monthly", "Range")

    TabRow(
        selectedTabIndex = selectedTab,
        containerColor = Color(0xFF3A3A3A),
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                color = AccentOrange,
                height = 3.dp
            )
        }
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
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

@Composable
fun StatsRow() {

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        StatCard(
            title = "Revenue",
            value = "0.00"
        )

        StatCard(
            title = "Tickets Sold",
            value = "0"
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF3A3A3A))
            .padding(20.dp)
    ) {
        Column {

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = AccentOrange
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyFilter() {

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {

        FilterChip(label = "Pick Month")

        Button(
            onClick = { },
            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
            shape = RoundedCornerShape(50)
        ) {
            Text("Apply", color = Color.White)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RangeFilter() {

    Column {

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            FilterChip(label = "2026-02-14")

            Text(
                text = "→",
                color = HintGray
            )

            FilterChip(label = "2026-02-14")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { },
            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
            shape = RoundedCornerShape(50)
        ) {
            Text("Apply", color = Color.White)
        }
    }
}

@Composable
fun FilterChip(label: String) {

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF3A3A3A))
            .border(1.dp, FieldBorderGray, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = AccentOrange,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}


@Composable
fun ChartPlaceholder() {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF4A4A4A)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No chart data",
            color = HintGray,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}


@Composable
fun ExportButton() {

    Button(
        onClick = { },
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentOrange
        ),
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
    ) {
        Text(
            text = "Export to Excel",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
    }
}

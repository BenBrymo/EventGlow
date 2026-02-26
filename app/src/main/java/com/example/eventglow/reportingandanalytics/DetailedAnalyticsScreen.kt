package com.example.eventglow.reportingandanalytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.eventglow.ui.theme.AccentOrange
import com.example.eventglow.ui.theme.BackgroundBlack
import com.example.eventglow.ui.theme.CardBlack
import com.example.eventglow.ui.theme.HintGray
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedAnalyticsScreen(
    viewModel: ReportingAnalyticsViewModel,
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val period = when (selectedTab) {
        0 -> ReportingPeriod.DAILY
        1 -> ReportingPeriod.WEEKLY
        2 -> ReportingPeriod.MONTHLY
        else -> ReportingPeriod.RANGE
    }

    val rangeStart = remember {
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }.time
    }
    val rangeEnd = remember { Calendar.getInstance().time }
    val rows = viewModel.filterRowsByPeriod(period, rangeStart, rangeEnd)
    val revenue = rows.sumOf { it.revenue }
    val sold = rows.sumOf { it.sold }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundBlack)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Detailed Analytics", color = Color.White)
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = CardBlack,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = AccentOrange,
                    height = 3.dp
                )
            }
        ) {
            listOf("Daily", "Weekly", "Monthly", "Range").forEachIndexed { index, label ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(label, color = if (selectedTab == index) AccentOrange else HintGray) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Card(colors = CardDefaults.cardColors(containerColor = CardBlack), modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Revenue", color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text("GHS ${String.format("%.2f", revenue)}", color = AccentOrange)
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = CardBlack), modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Tickets Sold", color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text("$sold", color = AccentOrange)
                }
            }
        }

        if (rows.isEmpty()) {
            Text("No chart data for selected period.", color = HintGray)
        } else {
            Card(colors = CardDefaults.cardColors(containerColor = CardBlack), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.take(8).forEach { row ->
                        Text(
                            text = "${row.title} - Sold ${row.sold} - GHS ${String.format("%.2f", row.revenue)}",
                            color = Color.White
                        )
                    }
                }
            }
        }

        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
            shape = RoundedCornerShape(50)
        ) { Text("Close", color = Color.White) }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

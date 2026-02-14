package com.example.eventglow.reportingandanalytics


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Search
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
fun AnalyticsScreen() {

    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack)
            .padding(horizontal = 20.dp)
    ) {

        Spacer(modifier = Modifier.height(16.dp))

        TopBalanceBar()

        Spacer(modifier = Modifier.height(16.dp))

        SearchWithAction()

        Spacer(modifier = Modifier.height(24.dp))

        EventsTabs(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )

        Spacer(modifier = Modifier.height(40.dp))

        EmptyState()

        Spacer(modifier = Modifier.weight(1f))

        RevenueCard()

        Spacer(modifier = Modifier.height(24.dp))
    }
}


@Composable
fun TopBalanceBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = "GHS 0",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF4CAF50) // subtle green balance
        )
    }
}

@Composable
fun SearchWithAction() {

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {

        OutlinedTextField(
            value = "",
            onValueChange = {},
            placeholder = {
                Text(
                    "Search events",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HintGray
                )
            },
            leadingIcon = {
                Icon(Icons.Default.Search, null, tint = HintGray)
            },
            singleLine = true,
            shape = RoundedCornerShape(50),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FieldBorderGray,
                unfocusedBorderColor = FieldBorderGray,
                focusedContainerColor = Color(0xFF2A2A2A),
                unfocusedContainerColor = Color(0xFF2A2A2A),
                cursorColor = AccentOrange
            ),
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Orange Money Button
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(AccentOrange),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AttachMoney,
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}


@Composable
fun EventsTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {

    val tabs = listOf("Ongoing Events", "Ended Events")

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
fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No events found.",
            style = MaterialTheme.typography.bodyLarge,
            color = HintGray
        )
    }
}


@Composable
fun RevenueCard() {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF3A3A3A))
            .padding(20.dp)
    ) {

        Column {

            Text(
                text = "Revenue",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "0 GHS",
                style = MaterialTheme.typography.titleMedium,
                color = AccentOrange
            )
        }
    }
}

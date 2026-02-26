package com.example.eventglow.reportingandanalytics

import android.app.DatePickerDialog
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eventglow.ui.theme.AccentOrange
import com.example.eventglow.ui.theme.BackgroundBlack
import com.example.eventglow.ui.theme.CardBlack
import com.example.eventglow.ui.theme.HintGray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: ReportingAnalyticsViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val summary by viewModel.summary.collectAsState()
    val rows by viewModel.rows.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val monthFormat = remember { SimpleDateFormat("MMMM", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val fileDateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    var selectedPeriodTab by remember { mutableStateOf(1) }
    var showOptionsSheet by remember { mutableStateOf(false) }
    var selectedDayDate by remember { mutableStateOf(Calendar.getInstance().time) }
    var selectedMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var selectedYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var rangeStartDate by remember {
        mutableStateOf(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }.time)
    }
    var rangeEndDate by remember { mutableStateOf(Calendar.getInstance().time) }
    var pendingExportType by remember { mutableStateOf<ExportType?>(null) }
    var pendingRows by remember { mutableStateOf<List<ReportingEventRow>>(emptyList()) }
    var pendingPeriodLabel by remember { mutableStateOf("") }

    val period = when (selectedPeriodTab) {
        0 -> ReportingPeriod.DAILY
        1 -> ReportingPeriod.WEEKLY
        2 -> ReportingPeriod.DAY
        3 -> ReportingPeriod.MONTH
        else -> ReportingPeriod.RANGE
    }
    val periodRows = when (period) {
        ReportingPeriod.DAY -> viewModel.filterRowsByDay(rows, selectedDayDate)

        ReportingPeriod.MONTH -> viewModel.filterRowsByMonth(
            sourceRows = rows,
            month = selectedMonth,
            year = selectedYear
        )

        ReportingPeriod.RANGE -> {
            val safeStart = if (rangeStartDate.after(rangeEndDate)) rangeEndDate else rangeStartDate
            val safeEnd = if (rangeEndDate.before(rangeStartDate)) rangeStartDate else rangeEndDate
            viewModel.filterRowsByRange(rows, safeStart, safeEnd)
        }

        else -> viewModel.filterRowsByPeriod(rows, period)
    }
    val periodLabel = when (period) {
        ReportingPeriod.DAILY -> "Daily"
        ReportingPeriod.WEEKLY -> "Weekly"
        ReportingPeriod.DAY -> "Day ${dateFormat.format(selectedDayDate)}"
        ReportingPeriod.MONTH -> "Month ${
            monthFormat.format(
                Calendar.getInstance().apply { set(Calendar.MONTH, selectedMonth) }.time
            )
        } $selectedYear"

        ReportingPeriod.RANGE -> "Range ${dateFormat.format(rangeStartDate)} - ${dateFormat.format(rangeEndDate)}"
    }
    val reportBaseName = when (period) {
        ReportingPeriod.DAILY -> "DailyReport"
        ReportingPeriod.WEEKLY -> "WeeklyReport"
        ReportingPeriod.DAY -> "DayReport_${fileDateFormat.format(selectedDayDate)}"
        ReportingPeriod.MONTH -> "${
            monthFormat.format(
                Calendar.getInstance().apply { set(Calendar.MONTH, selectedMonth) }.time
            )
        }Report"

        ReportingPeriod.RANGE -> "RangeReport_${fileDateFormat.format(rangeStartDate)}_to_${
            fileDateFormat.format(
                rangeEndDate
            )
        }"
    }.replace(" ", "")
    val periodRevenue = periodRows.sumOf { it.revenue }
    val periodSold = periodRows.sumOf { it.sold }

    val saveExcelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.ms-excel")
    ) { targetUri ->
        val exportType = pendingExportType
        if (targetUri == null || exportType != ExportType.EXCEL) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Excel export canceled.",
                    duration = SnackbarDuration.Long
                )
            }
            pendingExportType = null
            pendingRows = emptyList()
            pendingPeriodLabel = ""
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                AnalyticsExportHelper.exportAsExcelLikeXlsToUri(
                    context = context,
                    targetUri = targetUri,
                    rows = pendingRows,
                    periodLabel = pendingPeriodLabel
                )
            }
            val message = result.fold(
                onSuccess = { "Excel export completed: ${pendingRows.size} rows." },
                onFailure = { cause -> "Excel export failed: ${cause.message.orEmpty()}" }
            )
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long
            )
            pendingExportType = null
            pendingRows = emptyList()
            pendingPeriodLabel = ""
        }
    }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { targetUri ->
        val exportType = pendingExportType
        if (targetUri == null || exportType != ExportType.PDF) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "PDF export canceled.",
                    duration = SnackbarDuration.Long
                )
            }
            pendingExportType = null
            pendingRows = emptyList()
            pendingPeriodLabel = ""
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            snackbarHostState.showSnackbar(
                message = "Exporting PDF with ${pendingRows.size} rows...",
                duration = SnackbarDuration.Long
            )
            val result = withContext(Dispatchers.IO) {
                AnalyticsExportHelper.exportAsPdfToUri(
                    context = context,
                    targetUri = targetUri,
                    rows = pendingRows,
                    periodLabel = pendingPeriodLabel
                )
            }
            val message = result.fold(
                onSuccess = { "PDF export completed: ${pendingRows.size} rows." },
                onFailure = { cause -> "PDF export failed: ${cause.message.orEmpty()}" }
            )
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long
            )
            pendingExportType = null
            pendingRows = emptyList()
            pendingPeriodLabel = ""
        }
    }

    Scaffold(
        containerColor = BackgroundBlack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Reporting & Analytics",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                },
                actions = {
                    IconButton(onClick = { showOptionsSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackgroundBlack
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundBlack),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                HeroAnalyticsCard(
                    totalRevenue = summary.totalRevenue,
                    totalSold = summary.totalSold,
                    totalFree = summary.totalFreeTickets,
                    onRefresh = viewModel::refresh
                )
            }

            item {
                when (period) {
                    ReportingPeriod.DAY -> {
                        DaySelectorCard(
                            selectedDayLabel = dateFormat.format(selectedDayDate),
                            onPickDay = {
                                showDatePickerDialog(
                                    context = context,
                                    initialDate = selectedDayDate
                                ) { picked ->
                                    selectedDayDate = picked.time
                                }
                            }
                        )
                    }

                    ReportingPeriod.MONTH -> {
                        MonthSelectorCard(
                            selectedMonth = selectedMonth,
                            selectedYear = selectedYear,
                            onPreviousYear = { selectedYear -= 1 },
                            onNextYear = { selectedYear += 1 },
                            onSelectMonth = { selectedMonth = it }
                        )
                    }

                    ReportingPeriod.RANGE -> {
                        RangeSelectorCard(
                            startLabel = dateFormat.format(rangeStartDate),
                            endLabel = dateFormat.format(rangeEndDate),
                            onPickStart = {
                                showDatePickerDialog(
                                    context = context,
                                    initialDate = rangeStartDate
                                ) { picked ->
                                    rangeStartDate = picked.time
                                }
                            },
                            onPickEnd = {
                                showDatePickerDialog(
                                    context = context,
                                    initialDate = rangeEndDate
                                ) { picked ->
                                    rangeEndDate = picked.time
                                }
                            }
                        )
                    }

                    else -> {
                        Spacer(modifier = Modifier.height(0.dp))
                    }
                }
            }

            item {
                TabRow(
                    selectedTabIndex = selectedPeriodTab,
                    containerColor = CardBlack,
                    contentColor = AccentOrange,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedPeriodTab]),
                            color = AccentOrange,
                            height = 3.dp
                        )
                    }
                ) {
                    listOf("Daily", "Weekly", "Day", "Month", "Range").forEachIndexed { index, label ->
                        Tab(
                            selected = selectedPeriodTab == index,
                            onClick = { selectedPeriodTab = index },
                            text = {
                                Text(
                                    text = label,
                                    color = if (selectedPeriodTab == index) AccentOrange else HintGray
                                )
                            }
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        label = "Period Revenue",
                        value = "GHS ${String.format("%.2f", periodRevenue)}",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        label = "Period Tickets",
                        value = periodSold.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (errorMessage != null) {
                item {
                    Text(
                        text = errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error
                    )
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
            } else if (periodRows.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBlack),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No analytics rows match the current filter.",
                            color = HintGray,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
                items(periodRows) { row ->
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = row.title,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = row.date,
                                    color = HintGray,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Sold ${row.sold}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "GHS ${String.format("%.2f", row.revenue)}",
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

    if (showOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showOptionsSheet = false },
            containerColor = CardBlack
        ) {
            ExportOptionsSheet(
                onExportExcel = {
                    if (periodRows.isEmpty()) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "No data available for export in this filter.",
                                duration = SnackbarDuration.Long
                            )
                        }
                        showOptionsSheet = false
                        return@ExportOptionsSheet
                    }
                    pendingExportType = ExportType.EXCEL
                    pendingRows = periodRows.toList()
                    pendingPeriodLabel = periodLabel
                    val suggestedName = "${reportBaseName}.xls"
                    saveExcelLauncher.launch(suggestedName)
                    showOptionsSheet = false
                },
                onExportPdf = {
                    if (periodRows.isEmpty()) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "No data available for export in this filter.",
                                duration = SnackbarDuration.Long
                            )
                        }
                        showOptionsSheet = false
                        return@ExportOptionsSheet
                    }
                    pendingExportType = ExportType.PDF
                    pendingRows = periodRows.toList()
                    pendingPeriodLabel = periodLabel
                    val suggestedName = "${reportBaseName}.pdf"
                    savePdfLauncher.launch(suggestedName)
                    showOptionsSheet = false
                }
            )
        }
    }
}

private enum class ExportType {
    EXCEL,
    PDF
}

private fun showDatePickerDialog(
    context: Context,
    initialDate: Date,
    onDateSelected: (Calendar) -> Unit
) {
    val initialCalendar = Calendar.getInstance().apply { time = initialDate }
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val pickedCalendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onDateSelected(pickedCalendar)
        },
        initialCalendar.get(Calendar.YEAR),
        initialCalendar.get(Calendar.MONTH),
        initialCalendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

@Composable
private fun HeroAnalyticsCard(
    totalRevenue: Double,
    totalSold: Int,
    totalFree: Int,
    onRefresh: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBlack),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFFFF8A00).copy(alpha = 0.28f), Color.Transparent)
                    )
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = AccentOrange
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Analytics Hub", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    }
                    Button(
                        onClick = onRefresh,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Refresh")
                    }
                }
                Text("Revenue GHS ${String.format("%.2f", totalRevenue)}", color = Color.White)
                Text("Tickets sold $totalSold", color = HintGray)
                Text("Free tickets $totalFree", color = HintGray)
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardBlack)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(text = value, color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text(text = label, color = HintGray, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun MonthSelectorCard(
    selectedMonth: Int,
    selectedYear: Int,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
    onSelectMonth: (Int) -> Unit
) {
    val monthNames = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = CardBlack),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Month Filter", color = Color.White, style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = onPreviousYear,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                ) {
                    Text("-Y")
                }
                Spacer(modifier = Modifier.size(6.dp))
                Text(selectedYear.toString(), color = Color.White, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.size(6.dp))
                Button(
                    onClick = onNextYear,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                ) {
                    Text("+Y")
                }
            }

            monthNames.chunked(3).forEachIndexed { rowIndex, chunk ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chunk.forEachIndexed { index, monthName ->
                        val monthIndex = rowIndex * 3 + index
                        val isSelected = monthIndex == selectedMonth
                        Button(
                            onClick = { onSelectMonth(monthIndex) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) AccentOrange else BackgroundBlack
                            )
                        ) {
                            Text(monthName, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DaySelectorCard(
    selectedDayLabel: String,
    onPickDay: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBlack),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.DateRange, contentDescription = null, tint = AccentOrange)
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = selectedDayLabel,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onPickDay,
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
            ) {
                Text("Choose Day")
            }
        }
    }
}

@Composable
private fun RangeSelectorCard(
    startLabel: String,
    endLabel: String,
    onPickStart: () -> Unit,
    onPickEnd: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBlack),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Range Filter", color = Color.White, style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Start: $startLabel", color = HintGray, modifier = Modifier.weight(1f))
                Button(
                    onClick = onPickStart,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                ) {
                    Text("Choose")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("End: $endLabel", color = HintGray, modifier = Modifier.weight(1f))
                Button(
                    onClick = onPickEnd,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                ) {
                    Text("Choose")
                }
            }
        }
    }
}

@Composable
private fun ExportOptionsSheet(
    onExportExcel: () -> Unit,
    onExportPdf: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Export Report", color = Color.White, style = MaterialTheme.typography.titleLarge)
        Text("Choose a format for the current filtered analytics.", color = HintGray)

        Button(
            onClick = onExportExcel,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
        ) {
            Icon(Icons.Default.TableChart, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text("Export Excel (.xls)")
        }

        Button(
            onClick = onExportPdf,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
        ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text("Export PDF")
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun AnalyticsScreenPreview() {
    AnalyticsScreen()
}

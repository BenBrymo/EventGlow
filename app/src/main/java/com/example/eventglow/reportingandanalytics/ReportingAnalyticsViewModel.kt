package com.example.eventglow.reportingandanalytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReportingAnalyticsViewModel : ViewModel() {

    private val repository = TicketAnalyticsRepository()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { isLenient = false }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _allRows = MutableStateFlow<List<ReportingEventRow>>(emptyList())
    val allRows: StateFlow<List<ReportingEventRow>> = _allRows.asStateFlow()

    private val _rows = MutableStateFlow<List<ReportingEventRow>>(emptyList())
    val rows: StateFlow<List<ReportingEventRow>> = _rows.asStateFlow()

    private val _summary = MutableStateFlow(ReportingSummary())
    val summary: StateFlow<ReportingSummary> = _summary.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val aggregate = repository.fetchAggregate()
                val mapped = aggregate.events.map { event ->
                    val eventDate = parseDate(event.date)
                    ReportingEventRow(
                        eventId = event.id,
                        title = event.title,
                        date = event.date,
                        imageUrl = event.imageUrl,
                        sold = aggregate.soldByEvent[event.id] ?: 0,
                        revenue = aggregate.revenueByEvent[event.id] ?: 0.0,
                        isEnded = eventDate?.before(today()) == true
                    )
                }
                _allRows.value = mapped
                _summary.value = ReportingSummary(
                    totalRevenue = aggregate.totalRevenue,
                    totalSold = mapped.sumOf { it.sold },
                    totalFreeTickets = aggregate.totalFreeTickets
                )
                applyFilters()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load reporting data."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    fun onTabSelected(index: Int) {
        _selectedTab.value = index
        applyFilters()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun filterRowsByPeriod(period: ReportingPeriod, start: Date? = null, end: Date? = null): List<ReportingEventRow> {
        val now = Calendar.getInstance()
        return _allRows.value.filter { row ->
            val rowDate = parseDate(row.date) ?: return@filter false
            when (period) {
                ReportingPeriod.DAILY -> isSameDay(rowDate, now.time)
                ReportingPeriod.WEEKLY -> {
                    val weekAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.time
                    !rowDate.before(weekAgo) && !rowDate.after(now.time)
                }

                ReportingPeriod.MONTHLY -> {
                    val monthAgo = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }.time
                    !rowDate.before(monthAgo) && !rowDate.after(now.time)
                }

                ReportingPeriod.RANGE -> {
                    if (start == null || end == null) return@filter true
                    !rowDate.before(start) && !rowDate.after(end)
                }
            }
        }
    }

    private fun applyFilters() {
        val query = _searchQuery.value.trim()
        val tab = _selectedTab.value
        val base = when (tab) {
            0 -> _allRows.value.filter { !it.isEnded } // ongoing
            else -> _allRows.value.filter { it.isEnded } // ended
        }
        _rows.value = if (query.isBlank()) {
            base
        } else {
            base.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.date.contains(query, ignoreCase = true)
            }
        }
    }

    private fun parseDate(raw: String): Date? = try {
        dateFormat.parse(raw)
    } catch (_: Exception) {
        null
    }

    private fun today(): Date {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    private fun isSameDay(first: Date, second: Date): Boolean {
        val c1 = Calendar.getInstance().apply { time = first }
        val c2 = Calendar.getInstance().apply { time = second }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }
}

data class ReportingEventRow(
    val eventId: String,
    val title: String,
    val date: String,
    val imageUrl: String,
    val sold: Int,
    val revenue: Double,
    val isEnded: Boolean
)

data class ReportingSummary(
    val totalRevenue: Double = 0.0,
    val totalSold: Int = 0,
    val totalFreeTickets: Int = 0
)

enum class ReportingPeriod {
    DAILY, WEEKLY, MONTHLY, RANGE
}

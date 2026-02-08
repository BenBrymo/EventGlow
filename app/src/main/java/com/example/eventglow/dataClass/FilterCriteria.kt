package com.example.eventts.dataClass

data class FilterCriteria(
    val status: String = "",
    val categories: Set<String> = emptySet(),
    val startDate: String = "",
    val endDate: String = ""
)
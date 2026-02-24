package com.example.eventts.dataClass

data class EventFilterCriteria(
    val eventStatus: String = "",
    val eventCategories: List<String> = emptyList(),
    val eventStartDate: String = "",
    val eventEndDate: String = "",
    val matchAllCriteria: Boolean = false
)
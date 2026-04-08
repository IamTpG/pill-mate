package com.example.pillmate.presentation.model

import java.util.Date

data class CalendarDay(
    val date: Date,
    val dayOfWeek: String, // e.g., "MON"
    val dayOfMonth: String, // e.g., "15"
    val isSelected: Boolean = false
)

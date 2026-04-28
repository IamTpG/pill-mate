package com.example.pillmate.presentation.ui.screens

import org.koin.androidx.compose.koinViewModel
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import com.example.pillmate.R
import com.example.pillmate.presentation.model.HomeTask
import com.example.pillmate.presentation.ui.components.*
import com.example.pillmate.presentation.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    paddingValues: PaddingValues,
    onTaskClick: (HomeTask) -> Unit,
    onAddClick: () -> Unit,
    onDebugClick: () -> Unit,
    onMapClick: () -> Unit,
    profileViewModel: com.example.pillmate.presentation.viewmodel.ProfileViewModel = koinViewModel()
) {
    LaunchedEffect(Unit) {
        profileViewModel.syncCurrentProfile()
    }

    val uiState by viewModel.uiState.collectAsState()
    val calendarState = rememberLazyListState()
 
    LaunchedEffect(uiState.calendarDays) {
        if (uiState.calendarDays.isNotEmpty()) {
            val todayIndex = uiState.calendarDays.indexOfFirst { it.isSelected }
            if (todayIndex >= 0) {
                // Scroll so today is around the middle (index - offset)
                val scrollIndex = if (todayIndex > 2) todayIndex - 2 else 0
                calendarState.scrollToItem(scrollIndex)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            HomeHeader(
                onAddClick = onAddClick,
                onDebugClick = onDebugClick,
                onMapClick = onMapClick
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 20.dp, bottom = 20.dp)
            ) {
                item {
                    ProgressCard(
                        completed = uiState.dateProgress.first,
                        total = uiState.dateProgress.second,
                        isToday = isSameDay(uiState.selectedDate, Date())
                    )
                }

                item {
                    CalendarRow(
                        days = uiState.calendarDays,
                        onDateSelected = { date -> viewModel.selectDate(date) },
                        state = calendarState
                    )
                }

                item {
                    val isToday = isSameDay(uiState.selectedDate, Date())
                    val title = if (isToday) "Today's Schedule" else {
                        "Schedule for " + SimpleDateFormat("MMM dd", Locale.getDefault()).format(uiState.selectedDate)
                    }
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                items(uiState.dateTasks) { task ->
                    TaskItem(task = task, onClick = { onTaskClick(task) })
                }
            }
        }
    }
}
 
private fun isSameDay(d1: Date, d2: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = d1 }
    val cal2 = Calendar.getInstance().apply { time = d2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

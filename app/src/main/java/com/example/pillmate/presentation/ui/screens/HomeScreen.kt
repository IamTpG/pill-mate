package com.example.pillmate.presentation.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pillmate.R
import com.example.pillmate.presentation.model.HomeTask
import com.example.pillmate.presentation.ui.components.*
import com.example.pillmate.presentation.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onTaskClick: (HomeTask) -> Unit,
    onAddClick: () -> Unit,
    onDebugClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Column(modifier = Modifier.fillMaxSize()) {
            HomeHeader(onAddClick = onAddClick, onDebugClick = onDebugClick)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 20.dp, bottom = 80.dp)
            ) {
                item {
                    ProgressCard(
                        completed = uiState.todayProgress.first,
                        total = uiState.todayProgress.second
                    )
                }

                item {
                    CalendarRow(
                        days = uiState.calendarDays,
                        onDateSelected = { date -> viewModel.selectDate(date) }
                    )
                }

                item {
                    Text(
                        text = "Today's Schedule",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                items(uiState.todayTasks) { task ->
                    TaskItem(task = task, onClick = { onTaskClick(task) })
                }
            }
        }
    }
}

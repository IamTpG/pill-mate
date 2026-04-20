package com.example.pillmate.presentation.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.example.pillmate.presentation.viewmodel.CabinetViewModel
import com.example.pillmate.presentation.viewmodel.ScheduleBuilderViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun ScheduleBuilderFlowScreen(
    cabinetViewModel: CabinetViewModel = koinViewModel(),
    scheduleViewModel: ScheduleBuilderViewModel = koinViewModel(),
    onCompleteMapping: () -> Unit,
    onBack: () -> Unit
) {
    var currentScreen by remember { mutableStateOf("PICKER") }
    val uiState by scheduleViewModel.uiState.collectAsState()

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            onCompleteMapping()
        }
    }
    
    if (currentScreen == "PICKER") {
        MedicationPickerScreen(
            viewModel = cabinetViewModel,
            onMedicationSelected = { med ->
                scheduleViewModel.setSelectedMedication(med)
                currentScreen = "BUILDER"
            },
            onBack = onBack
        )
    } else {
        ScheduleBuilderScreen(
            viewModel = scheduleViewModel,
            onBack = { currentScreen = "PICKER" }
        )
    }
}

package com.example.pillmate.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.pillmate.presentation.ui.screens.MedicationPickerScreen
import com.example.pillmate.presentation.ui.screens.ScheduleBuilderScreen
import com.example.pillmate.presentation.viewmodel.CabinetViewModel
import com.example.pillmate.presentation.viewmodel.ScheduleBuilderViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class ScheduleBuilderFragment : Fragment() {

    private val scheduleViewModel: ScheduleBuilderViewModel by viewModel()
    private val cabinetViewModel: CabinetViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                var currentScreen by remember { mutableStateOf("PICKER") }
                
                if (currentScreen == "PICKER") {
                    MedicationPickerScreen(
                        viewModel = cabinetViewModel,
                        onMedicationSelected = { med ->
                            scheduleViewModel.setSelectedMedication(med)
                            currentScreen = "BUILDER"
                        }
                    )
                } else {
                    ScheduleBuilderScreen(
                        viewModel = scheduleViewModel,
                        onBack = { currentScreen = "PICKER" }
                    )
                }
            }
        }
    }
}

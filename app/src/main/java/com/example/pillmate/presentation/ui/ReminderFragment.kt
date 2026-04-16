package com.example.pillmate.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.example.pillmate.presentation.ui.screens.ReminderScreen
import com.example.pillmate.presentation.viewmodel.ReminderViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class ReminderFragment : Fragment() {

    private val viewModel: ReminderViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ReminderScreen(viewModel = viewModel)
            }
        }
    }
}

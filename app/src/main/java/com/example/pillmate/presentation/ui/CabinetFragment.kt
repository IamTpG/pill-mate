package com.example.pillmate.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.example.pillmate.presentation.ui.screens.CabinetScreen
import com.example.pillmate.presentation.viewmodel.CabinetViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class CabinetFragment : Fragment() {

    private val cabinetViewModel: CabinetViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                CabinetScreen(viewModel = cabinetViewModel)
            }
        }
    }
}

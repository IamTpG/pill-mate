package com.example.pillmate.presentation.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.pillmate.presentation.ui.screens.HomeScreen
import com.example.pillmate.presentation.viewmodel.HomeViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModel()

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (!granted) {
                Toast.makeText(requireContext(), "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                HomeScreen(
                    viewModel = viewModel,
                    onTaskClick = { task ->
                        val intent = Intent(requireContext(), TaskAlarmActivity::class.java).apply {
                            putExtra("SOURCE_ID", task.sourceId)
                            putExtra("SCHEDULE_ID", task.scheduleId)
                            putExtra("TITLE", task.title)
                            putExtra("DETAILS", task.doseDescription)
                            putExtra("TASK_TYPE", task.taskType.name)
                        }
                        startActivity(intent)
                    },
                    onAddClick = {
                        // TODO: Implement Add Task navigation if needed, 
                        // for now it matches the behavior in XML (which had no listener in Fragment but was defined in XML)
                        Toast.makeText(requireContext(), "Add task clicked", Toast.LENGTH_SHORT).show()
                    },
                    onDebugClick = {
                        DebugMenuFragment().show(parentFragmentManager, "DebugMenu")
                    }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkNotificationPermission()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS

            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(permission)
            }
        }
    }
}

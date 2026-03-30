package com.example.pillmate.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.pillmate.databinding.FragmentDebugMenuBinding
import com.example.pillmate.util.DataGenerator
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class DebugMenuFragment(private val profileId: String) : BottomSheetDialogFragment() {

    private var _binding: FragmentDebugMenuBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDebugMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val generator = DataGenerator(FirebaseFirestore.getInstance())

        binding.btnGenerateData.setOnClickListener {
            lifecycleScope.launch {
                binding.btnGenerateData.isEnabled = false
                try {
                    generator.generateSampleData(profileId)
                    Toast.makeText(requireContext(), "Data Generated!", Toast.LENGTH_SHORT).show()
                    dismiss()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG)
                        .show()
                } finally {
                    binding.btnGenerateData.isEnabled = true
                }
            }
        }

        binding.btnClearData.setOnClickListener {
            lifecycleScope.launch {
                try {
                    generator.clearUserData(profileId)
                    Toast.makeText(requireContext(), "Data Cleared!", Toast.LENGTH_SHORT).show()
                    dismiss()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }

        binding.btnTriggerAlarm.setOnClickListener {
            val intent = android.content.Intent(requireContext(), TaskAlarmActivity::class.java)
            intent.putExtra("MED_ID", "debug_pill_id")
            startActivity(intent)
            dismiss()
        }
    }
    

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

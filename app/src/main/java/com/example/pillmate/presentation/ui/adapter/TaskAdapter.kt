package com.example.pillmate.presentation.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pillmate.R
import com.example.pillmate.databinding.ItemTaskHomeBinding
import com.example.pillmate.domain.model.LogStatus
import com.example.pillmate.presentation.model.HomeTask

class TaskAdapter(private val onTaskClick: (HomeTask) -> Unit) :
    ListAdapter<HomeTask, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskHomeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(private val binding: ItemTaskHomeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: HomeTask) {
            binding.tvTaskName.text = task.title
            binding.tvTaskTime.text = task.time
            binding.tvTaskDesc.text = task.doseDescription
            
            // Set status badge
            binding.tvStatusBadge.visibility = android.view.View.VISIBLE
            
            val (statusText, textColorRes, bgColorString) = when (task.status) {
                LogStatus.COMPLETED -> Triple("Done", R.color.status_done, "#D5F5E3")
                LogStatus.MISSED -> Triple("Missed", R.color.status_missed, "#FADBD8")
                LogStatus.SKIPPED -> Triple("Skipped", R.color.status_upcoming, "#EBEDEF")
                LogStatus.SNOOZED -> Triple("Snoozed", R.color.status_snoozed, "#FCF3CF")
                LogStatus.LATE -> Triple("Late", R.color.status_missed, "#FADBD8")
                null -> Triple("Upcoming", R.color.status_upcoming, "#EBF5FB")
            }

            binding.tvStatusBadge.text = statusText
            binding.tvStatusBadge.setTextColor(ContextCompat.getColor(itemView.context, textColorRes))
            binding.tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor(bgColorString)
            )

            binding.root.setOnClickListener { onTaskClick(task) }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<HomeTask>() {
        override fun areItemsTheSame(oldItem: HomeTask, newItem: HomeTask): Boolean =
            oldItem.scheduleId == newItem.scheduleId

        override fun areContentsTheSame(oldItem: HomeTask, newItem: HomeTask): Boolean =
            oldItem == newItem
    }
}

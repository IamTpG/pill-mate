package com.example.pillmate.presentation.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pillmate.databinding.ItemCalendarDayBinding
import com.example.pillmate.databinding.ItemCalendarDaySelectedBinding
import com.example.pillmate.presentation.model.CalendarDay

class CalendarAdapter(private val onDateSelected: (CalendarDay) -> Unit) :
    ListAdapter<CalendarDay, RecyclerView.ViewHolder>(CalendarDiffCallback()) {

    companion object {
        private const val TYPE_NORMAL = 0
        private const val TYPE_SELECTED = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isSelected) TYPE_SELECTED else TYPE_NORMAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_SELECTED) {
            val binding = ItemCalendarDaySelectedBinding.inflate(inflater, parent, false)
            SelectedViewHolder(binding)
        } else {
            val binding = ItemCalendarDayBinding.inflate(inflater, parent, false)
            NormalViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val day = getItem(position)
        if (holder is SelectedViewHolder) {
            holder.bind(day)
        } else if (holder is NormalViewHolder) {
            holder.bind(day)
        }
    }

    inner class NormalViewHolder(private val binding: ItemCalendarDayBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(day: CalendarDay) {
            binding.tvDayOfWeek.text = day.dayOfWeek
            binding.tvDayOfMonth.text = day.dayOfMonth
            binding.root.setOnClickListener { onDateSelected(day) }
        }
    }

    inner class SelectedViewHolder(private val binding: ItemCalendarDaySelectedBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(day: CalendarDay) {
            binding.tvDayOfWeekSelected.text = day.dayOfWeek
            binding.tvDayOfMonthSelected.text = day.dayOfMonth
            binding.root.setOnClickListener { onDateSelected(day) }
        }
    }

    class CalendarDiffCallback : DiffUtil.ItemCallback<CalendarDay>() {
        override fun areItemsTheSame(oldItem: CalendarDay, newItem: CalendarDay): Boolean {
            return oldItem.date == newItem.date
        }

        override fun areContentsTheSame(oldItem: CalendarDay, newItem: CalendarDay): Boolean {
            return oldItem == newItem
        }
    }
}

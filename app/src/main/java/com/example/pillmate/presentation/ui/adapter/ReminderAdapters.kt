package com.example.pillmate.presentation.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pillmate.R
import com.example.pillmate.domain.model.Reminder
import com.example.pillmate.domain.model.Schedule

class ScheduleReminderAdapter(
    private val onAddReminderClick: (Schedule) -> Unit,
    private val onEditReminderClick: (Schedule, Reminder) -> Unit,
    private val onRemoveReminderClick: (Schedule, Reminder) -> Unit
) : RecyclerView.Adapter<ScheduleReminderAdapter.ViewHolder>() {

    private var items: List<Schedule> = emptyList()

    fun submitList(newItems: List<Schedule>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_schedule_reminder, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvScheduleTitle)
        private val tvTime: TextView = itemView.findViewById(R.id.tvScheduleTime)
        private val tvRRULE: TextView = itemView.findViewById(R.id.tvScheduleRRULE)
        private val btnAdd: ImageButton = itemView.findViewById(R.id.btnAddReminder)
        private val rvReminders: RecyclerView = itemView.findViewById(R.id.rvReminders)

        fun bind(schedule: Schedule) {
            tvTitle.text = schedule.eventSnapshot.title
            
            // Format time nicely
            val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
            val hhmmFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val hFormat = java.text.SimpleDateFormat("H:m", java.util.Locale.getDefault())
            val displayFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())

            val startTimeStr = schedule.startTime
            val parsedTime: java.util.Date? = try {
                when {
                    startTimeStr.contains("T") -> isoFormat.parse(startTimeStr)
                    else -> try { hhmmFormat.parse(startTimeStr) } catch(e: Exception) { hFormat.parse(startTimeStr) }
                }
            } catch (e: Exception) { null }

            tvTime.text = if (parsedTime != null) displayFormat.format(parsedTime) else startTimeStr
            
            // Format RRULE nicely
            val readableRRULE = when {
                schedule.recurrenceRule?.contains("FREQ=DAILY") == true -> "Daily"
                schedule.recurrenceRule?.contains("FREQ=WEEKLY") == true -> "Weekly"
                !schedule.recurrenceRule.isNullOrBlank() -> schedule.recurrenceRule
                else -> ""
            }
            tvRRULE.text = if (readableRRULE.isNotBlank()) "| $readableRRULE" else ""

            val reminderAdapter = ReminderAdapter(
                onEditClick = { reminder -> onEditReminderClick(schedule, reminder) },
                onRemoveClick = { reminder -> onRemoveReminderClick(schedule, reminder) }
            )
            rvReminders.layoutManager = LinearLayoutManager(itemView.context)
            rvReminders.adapter = reminderAdapter
            reminderAdapter.submitList(schedule.reminders)

            btnAdd.setOnClickListener {
                onAddReminderClick(schedule)
            }
        }
    }
}

class ReminderAdapter(
    private val onEditClick: (Reminder) -> Unit,
    private val onRemoveClick: (Reminder) -> Unit
) : RecyclerView.Adapter<ReminderAdapter.ViewHolder>() {

    private var items: List<Reminder> = emptyList()

    fun submitList(newItems: List<Reminder>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reminder, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvText: TextView = itemView.findViewById(R.id.tvReminderText)
        private val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemoveReminder)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEditReminder)

        fun bind(reminder: Reminder) {
            tvText.text = "${reminder.minutesBefore} mins before (${reminder.type})"
            btnRemove.setOnClickListener {
                onRemoveClick(reminder)
            }
            btnEdit.setOnClickListener {
                onEditClick(reminder)
            }
        }
    }
}

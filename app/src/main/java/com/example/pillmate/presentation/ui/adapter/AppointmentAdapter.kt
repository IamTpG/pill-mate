package com.example.pillmate.presentation.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.pillmate.databinding.ItemAppointmentCardBinding
import com.example.pillmate.domain.model.Appointment

class AppointmentAdapter(private val onAppointmentClick: (Appointment) -> Unit)
	: ListAdapter<Appointment, AppointmentAdapter.AppointmentViewHolder>(AppointmentDiffCallback()){
	
	inner class AppointmentViewHolder(private val binding: ItemAppointmentCardBinding) :
			RecyclerView.ViewHolder(binding.root) {
	
		fun bind(appointment: Appointment) {
		
			
			binding.root.setOnClickListener { onAppointmentClick(appointment) }
		}
	}
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
		val binding = ItemAppointmentCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
		return AppointmentViewHolder(binding)
	}
	
	override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
		holder.bind(getItem(position))
	}
	
	class AppointmentDiffCallback : DiffUtil.ItemCallback<Appointment>() {
		override fun areItemsTheSame(p0: Appointment, p1: Appointment): Boolean {
			return p0.id == p1.id
		}
		
		override fun areContentsTheSame(p0: Appointment, p1: Appointment): Boolean {
			return p0 == p1
		}
	}
}
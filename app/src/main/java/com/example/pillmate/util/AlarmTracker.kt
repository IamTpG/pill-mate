package com.example.pillmate.util

import android.content.Context
import android.content.SharedPreferences

class AlarmTracker(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("alarm_tracker", Context.MODE_PRIVATE)

    fun getScheduledIds(): Set<Int> {
        return prefs.getStringSet("scheduled_ids", emptySet())
            ?.map { it.toInt() }?.toSet() ?: emptySet()
    }

    fun updateScheduledIds(ids: Set<Int>) {
        prefs.edit().putStringSet("scheduled_ids", ids.map { it.toString() }.toSet()).apply()
    }

    fun addId(id: Int) {
        val current = getScheduledIds().toMutableSet()
        current.add(id)
        updateScheduledIds(current)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}

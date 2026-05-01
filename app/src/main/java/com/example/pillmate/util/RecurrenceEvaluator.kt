package com.example.pillmate.util

import java.text.SimpleDateFormat
import java.util.*

object RecurrenceEvaluator {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    /**
     * Checks if a specific date matches the schedule's recurrence and constraints.
     * @param targetDate The date to check (ignoring time for daily/weekly/interval recurrence).
     * @param rrule The RRULE string (e.g., "FREQ=WEEKLY;BYDAY=MO,WE,FR").
     * @param startTimeIso The anchor date/time in ISO format.
     * @param endDate The expiration date.
     * @return True if the schedule occurs on the target date.
     */
    fun isOccurringOn(targetDate: Date, rrule: String?, startTimeIso: String?, endDate: Date?): Boolean {
        val targetCal = Calendar.getInstance().apply { 
            time = targetDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val startCal = Calendar.getInstance().apply {
            val date = try { startTimeIso?.let { isoFormat.parse(it) } } catch (e: Exception) { null } ?: Date(0)
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 1. Constraint Checks
        if (targetCal.before(startCal)) return false
        if (endDate != null) {
            val endCal = Calendar.getInstance().apply { 
                time = endDate
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }
            if (targetCal.after(endCal)) return false
        }

        if (rrule.isNullOrBlank()) return targetCal.equals(startCal)

        // 2. Parse RRULE
        val parts = rrule.split(";").associate { 
            val pair = it.split("=")
            pair[0] to (pair.getOrNull(1) ?: "")
        }

        val freq = parts["FREQ"] ?: "DAILY"
        val interval = parts["INTERVAL"]?.toIntOrNull() ?: 1
        
        return when (freq) {
            "DAILY" -> {
                val diffDays = ((targetCal.timeInMillis - startCal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                diffDays % interval == 0
            }
            "WEEKLY" -> {
                val byDay = parts["BYDAY"] ?: ""
                if (byDay.isNotEmpty()) {
                    val days = byDay.split(",")
                    val targetDay = when (targetCal.get(Calendar.DAY_OF_WEEK)) {
                        Calendar.MONDAY -> "MO"
                        Calendar.TUESDAY -> "TU"
                        Calendar.WEDNESDAY -> "WE"
                        Calendar.THURSDAY -> "TH"
                        Calendar.FRIDAY -> "FR"
                        Calendar.SATURDAY -> "SA"
                        Calendar.SUNDAY -> "SU"
                        else -> ""
                    }
                    val dayMatches = days.contains(targetDay)
                    
                    // Interval for weekly means "Every X weeks"
                    if (dayMatches && interval > 1) {
                        val diffWeeks = ((targetCal.timeInMillis - startCal.timeInMillis) / (1000 * 60 * 60 * 24 * 7)).toInt()
                        diffWeeks % interval == 0
                    } else {
                        dayMatches
                    }
                } else {
                    val diffDays = ((targetCal.timeInMillis - startCal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                    (diffDays / 7) % interval == 0
                }
            }
            "MONTHLY" -> {
                // Simplified monthly: same day of month
                val dayMatches = targetCal.get(Calendar.DAY_OF_MONTH) == startCal.get(Calendar.DAY_OF_MONTH)
                if (dayMatches && interval > 1) {
                    val diffMonths = (targetCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR)) * 12 + 
                                     (targetCal.get(Calendar.MONTH) - startCal.get(Calendar.MONTH))
                    diffMonths % interval == 0
                } else {
                    dayMatches
                }
            }
            "HOURLY" -> true // Hourly doesn't care about day, always occurs on day if within start/end
            else -> true
        }
    }
    
    /**
     * Finds the next occurrence date/time for a specific dose time.
     */
    fun getNextOccurrence(fromDate: Date, rrule: String?, startTimeIso: String?, endDate: Date?, doseTime: String): Date? {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val fallbackFormat = SimpleDateFormat("H:m", Locale.getDefault())

        val parsedTime = try {
            if (doseTime.contains("T")) isoFormat.parse(doseTime)
            else try { timeFormat.parse(doseTime) } catch (e: Exception) { fallbackFormat.parse(doseTime) }
        } catch (e: Exception) { null } ?: return null

        val timeCal = Calendar.getInstance().apply { time = parsedTime }
        val checkDate = Calendar.getInstance().apply { 
            time = fromDate
            // Buffer to avoid skipping "right now" alarms
            add(Calendar.SECOND, 5) 
        }

        // Try for the next 365 days
        for (i in 0 until 365) {
            val candidate = Calendar.getInstance().apply {
                time = checkDate.time
                add(Calendar.DAY_OF_YEAR, i)
                set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (candidate.time.after(checkDate.time)) {
                if (isOccurringOn(candidate.time, rrule, startTimeIso, endDate)) {
                    return candidate.time
                }
            }
        }
        return null
    }
}

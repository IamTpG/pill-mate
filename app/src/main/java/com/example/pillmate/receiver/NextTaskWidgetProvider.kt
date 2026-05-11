package com.example.pillmate.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.pillmate.R
import com.example.pillmate.domain.model.TaskType
import com.example.pillmate.domain.usecase.GetWidgetDataUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import java.text.SimpleDateFormat
import java.util.Locale

class NextTaskWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, NextTaskWidgetProvider::class.java)
            )
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        scope.launch {
            val views = RemoteViews(context.packageName, R.layout.widget_next_task)
            
            try {
                val getWidgetDataUseCase = GlobalContext.get().get<GetWidgetDataUseCase>()
                val profileId = GlobalContext.get().get<String>()
                
                val widgetData = getWidgetDataUseCase.execute(profileId)
                val nextTask = widgetData.nextTask
                
                // Update Next Task
                if (nextTask != null) {
                    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    views.setTextViewText(R.id.med_name, nextTask.title)
                    views.setTextViewText(R.id.med_time, timeFormat.format(nextTask.time))
                    views.setTextViewText(R.id.med_dose, nextTask.details)
                    views.setViewVisibility(R.id.task_actions, View.VISIBLE)
                    views.setTextViewText(R.id.complete_button, getCompleteButtonText(nextTask.taskType))
                    views.setOnClickPendingIntent(
                        R.id.complete_button,
                        createTaskActionPendingIntent(
                            context = context,
                            appWidgetId = appWidgetId,
                            action = ACTION_COMPLETE,
                            sourceId = nextTask.sourceId,
                            scheduleId = nextTask.scheduleId,
                            taskType = nextTask.taskType.name,
                            scheduledTimeMillis = nextTask.time.time,
                            dose = nextTask.dose
                        )
                    )
                    views.setOnClickPendingIntent(
                        R.id.skip_button,
                        createTaskActionPendingIntent(
                            context = context,
                            appWidgetId = appWidgetId,
                            action = ACTION_SKIP,
                            sourceId = nextTask.sourceId,
                            scheduleId = nextTask.scheduleId,
                            taskType = nextTask.taskType.name,
                            scheduledTimeMillis = nextTask.time.time,
                            dose = nextTask.dose
                        )
                    )
                } else {
                    views.setTextViewText(R.id.med_name, "No tasks")
                    views.setTextViewText(R.id.med_time, "All done for now!")
                    views.setTextViewText(R.id.med_dose, "")
                    views.setViewVisibility(R.id.task_actions, View.GONE)
                }

                // Update Hydration
                val hydrationText = "${widgetData.hydrationMl}/${widgetData.hydrationGoal} ml"
                views.setTextViewText(R.id.hydration_progress_text, hydrationText)
                val progress = if (widgetData.hydrationGoal > 0) {
                    (widgetData.hydrationMl * 1000 / widgetData.hydrationGoal).coerceIn(0, 1000)
                } else 0
                views.setProgressBar(R.id.hydration_progress_bar, 1000, progress, false)

            } catch (e: Exception) {
                views.setTextViewText(R.id.med_name, "PillMate")
                views.setTextViewText(R.id.med_time, "Tap to open")
                views.setTextViewText(R.id.med_dose, "")
                views.setViewVisibility(R.id.task_actions, View.GONE)
            }

            // Click to open app
            val intent = Intent(context, com.example.pillmate.MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.med_name, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun createTaskActionPendingIntent(
        context: Context,
        appWidgetId: Int,
        action: String,
        sourceId: String,
        scheduleId: String,
        taskType: String,
        scheduledTimeMillis: Long,
        dose: Float
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra("SOURCE_ID", sourceId)
            putExtra("SCHEDULE_ID", scheduleId)
            putExtra("TASK_TYPE", taskType)
            putExtra("EXTRA_SCHEDULED_TIME", scheduledTimeMillis)
            putExtra("EXTRA_DOSE", dose)
        }
        val requestCode = "$appWidgetId:$scheduleId:$action".hashCode()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getCompleteButtonText(taskType: TaskType): String {
        return when (taskType) {
            TaskType.MEDICATION -> "TAKE NOW"
            TaskType.APPOINTMENT -> "ATTEND"
            TaskType.EXERCISE -> "START"
            else -> "COMPLETE"
        }
    }

    private companion object {
        const val ACTION_UPDATE_WIDGET = "com.example.pillmate.ACTION_UPDATE_WIDGET"
        const val ACTION_COMPLETE = "ACTION_COMPLETE"
        const val ACTION_SKIP = "ACTION_SKIP"
    }
}

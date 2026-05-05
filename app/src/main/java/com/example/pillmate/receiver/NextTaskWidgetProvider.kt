package com.example.pillmate.receiver

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.pillmate.R
import com.example.pillmate.domain.usecase.GetNextTaskUseCase
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
        if (intent.action == "com.example.pillmate.ACTION_UPDATE_WIDGET") {
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
                } else {
                    views.setTextViewText(R.id.med_name, "No tasks")
                    views.setTextViewText(R.id.med_time, "All done for now!")
                    views.setTextViewText(R.id.med_dose, "")
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
            }

            // Click to open app
            val intent = Intent(context, com.example.pillmate.MainActivity::class.java)
            val pendingIntent = android.app.PendingIntent.getActivity(
                context, 0, intent, 
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.med_name, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

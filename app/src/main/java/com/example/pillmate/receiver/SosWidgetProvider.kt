package com.example.pillmate.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.example.pillmate.R
import com.example.pillmate.data.local.dao.ProfileDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

class SosWidgetProvider : AppWidgetProvider() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            scope.launch {
                val views = RemoteViews(context.packageName, R.layout.widget_sos)
                try {
                    // Dùng Koin lấy ProfileDao
                    val profileDao = GlobalContext.get().get<ProfileDao>()
                    val activeProfile = profileDao.getActiveProfile()
                    val sosNumber = activeProfile?.sosNumber

                    if (!sosNumber.isNullOrEmpty()) {
                        // Gọi Action_DIAL mớm sẵn số lên trình gọi điện
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:$sosNumber")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        val pendingIntent = PendingIntent.getActivity(
                            context, 0, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_sos_button, pendingIntent)
                        views.setTextViewText(R.id.widget_sos_text, "GỌI SOS")
                    } else {
                        // Nếu chưa cài số, bấm vào sẽ mở app
                        val intent = Intent(context, com.example.pillmate.MainActivity::class.java)
                        val pendingIntent = PendingIntent.getActivity(
                            context, 0, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_sos_button, pendingIntent)
                        views.setTextViewText(R.id.widget_sos_text, "Setup SOS")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}
package io.heckel.ntfy.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.heckel.ntfy.db.Repository
import io.heckel.ntfy.msg.ApiService
import io.heckel.ntfy.msg.NotificationDispatcher
import io.heckel.ntfy.msg.Poller
import io.heckel.ntfy.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WidgetUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Updating widget with latest notifications")

        try {
            val repository = Repository.getInstance(applicationContext)
            val api = ApiService(applicationContext)
            val poller = Poller(api, repository)
            val dispatcher = NotificationDispatcher(applicationContext, repository)

            val subscriptions = repository.getSubscriptions()
            for (subscription in subscriptions) {
                try {
                    val newNotifications = poller.poll(subscription)
                    for (notification in newNotifications) {
                        dispatcher.dispatch(subscription, notification)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error polling subscription ${subscription.id}: ${e.message}")
                }
            }

            // Refresh widget
            val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
            val widgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(applicationContext, NtfyWidgetProvider::class.java)
            )
            for (widgetId in widgetIds) {
                NtfyWidgetProvider.updateAppWidget(applicationContext, appWidgetManager, widgetId)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Widget update failed: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        const val TAG = "NtfyWidgetUpdateWorker"
        const val WORK_NAME = "ntfy_widget_update"
    }
}

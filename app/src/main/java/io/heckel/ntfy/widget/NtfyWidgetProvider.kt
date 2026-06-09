package io.heckel.ntfy.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import android.widget.RemoteViews
import io.heckel.ntfy.R
import io.heckel.ntfy.db.Repository
import io.heckel.ntfy.ui.DetailActivity
import io.heckel.ntfy.ui.MainActivity
import io.heckel.ntfy.util.displayName

class NtfyWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // Clean up preferences when widget is removed
        val repository = Repository.getInstance(context)
        for (appWidgetId in appWidgetIds) {
            repository.setWidgetSubscriptionId(appWidgetId, 0L)
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: android.os.Bundle?) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val repository = Repository.getInstance(context)
            val views = RemoteViews(context.packageName, R.layout.widget_ntfy)
            val appBaseUrl = context.getString(R.string.app_base_url)

            kotlinx.coroutines.runBlocking {
                val subId = repository.getWidgetSubscriptionId(appWidgetId)
                if (subId == 0L) {
                    // No topic selected
                    views.setViewVisibility(R.id.widget_message, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_empty_text, android.view.View.VISIBLE)

                    // Tap to configure
                    val configIntent = Intent(context, NtfyWidgetConfigureActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }
                    val configPendingIntent = PendingIntent.getActivity(
                        context, appWidgetId, configIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_empty_text, configPendingIntent)
                } else {
                    val subscription = repository.getSubscription(subId)
                    if (subscription != null) {
                        // Build full content from latest notification
                        val sb = StringBuilder()
                        if (subscription.latestTitle != null) {
                            sb.appendLine(subscription.latestTitle)
                        }
                        if (subscription.latestMessage != null) {
                            sb.append(subscription.latestMessage)
                        }
                        val content = sb.toString().trimEnd()

                        if (content.isEmpty()) {
                            views.setViewVisibility(R.id.widget_message, android.view.View.GONE)
                            views.setViewVisibility(R.id.widget_empty_text, android.view.View.VISIBLE)
                            views.setTextViewText(R.id.widget_empty_text, context.getString(R.string.widget_no_notifications))
                        } else {
                            views.setViewVisibility(R.id.widget_empty_text, android.view.View.GONE)
                            views.setViewVisibility(R.id.widget_message, android.view.View.VISIBLE)
                            views.setTextViewText(R.id.widget_message, content)
                        }

                        val label = displayName(appBaseUrl, subscription)
                        views.setTextViewText(R.id.widget_title, label)

                        // Tap content to open detail
                        val detailIntent = Intent(context, DetailActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra(MainActivity.EXTRA_SUBSCRIPTION_ID, subscription.id)
                            putExtra(MainActivity.EXTRA_SUBSCRIPTION_BASE_URL, subscription.baseUrl)
                            putExtra(MainActivity.EXTRA_SUBSCRIPTION_TOPIC, subscription.topic)
                            putExtra(MainActivity.EXTRA_SUBSCRIPTION_DISPLAY_NAME, label)
                            putExtra(MainActivity.EXTRA_SUBSCRIPTION_INSTANT, subscription.instant)
                            putExtra(MainActivity.EXTRA_SUBSCRIPTION_MUTED_UNTIL, subscription.mutedUntil)
                        }
                        val pendingIntent = PendingIntent.getActivity(
                            context, appWidgetId, detailIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_message, pendingIntent)
                    } else {
                        views.setViewVisibility(R.id.widget_message, android.view.View.GONE)
                        views.setViewVisibility(R.id.widget_empty_text, android.view.View.VISIBLE)
                        views.setTextViewText(R.id.widget_empty_text, context.getString(R.string.widget_no_topic_selected))
                    }
                }

                // Updated time
                val now = java.util.Date()
                val timeStr = DateFormat.getTimeFormat(context).format(now)
                views.setTextViewText(R.id.widget_updated, context.getString(R.string.widget_updated_format, timeStr))
            }

            // Open app button
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val mainPendingIntent = PendingIntent.getActivity(
                context, 0, mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_open_app, mainPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_title, mainPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

package io.heckel.ntfy.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import android.widget.RemoteViews
import io.heckel.ntfy.R
import io.heckel.ntfy.db.Database
import io.heckel.ntfy.db.Notification
import io.heckel.ntfy.db.Repository
import io.heckel.ntfy.ui.DetailActivity
import io.heckel.ntfy.ui.MainActivity
import io.heckel.ntfy.util.displayName
import io.heckel.ntfy.util.formatDateShort

class NtfyWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
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

            kotlinx.coroutines.runBlocking {
                val subscriptions = repository.getSubscriptions()
                val appBaseUrl = context.getString(R.string.app_base_url)

                if (subscriptions.isEmpty()) {
                    views.setViewVisibility(R.id.widget_empty_text, android.view.View.VISIBLE)
                    views.setViewVisibility(R.id.widget_card_1, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_card_2, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_card_3, android.view.View.GONE)
                } else {
                    views.setViewVisibility(R.id.widget_empty_text, android.view.View.GONE)

                    val latestNotifications = subscriptions
                        .filter { it.latestTitle != null || it.latestMessage != null }
                        .take(3)

                    val cardContainers = listOf(R.id.widget_card_1, R.id.widget_card_2, R.id.widget_card_3)
                    val cardTitleViews = listOf(R.id.widget_card_1_title, R.id.widget_card_2_title, R.id.widget_card_3_title)
                    val cardMessageViews = listOf(R.id.widget_card_1_message, R.id.widget_card_2_message, R.id.widget_card_3_message)

                    for (i in 0..2) {
                        if (i < latestNotifications.size) {
                            val sub = latestNotifications[i]
                            views.setViewVisibility(cardContainers[i], android.view.View.VISIBLE)
                            val label = displayName(appBaseUrl, sub)
                            val title = sub.latestTitle ?: ""
                            views.setTextViewText(cardTitleViews[i], "$label: $title")
                            views.setTextViewText(cardMessageViews[i], sub.latestMessage ?: "")

                            val detailIntent = Intent(context, DetailActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                putExtra(MainActivity.EXTRA_SUBSCRIPTION_ID, sub.id)
                                putExtra(MainActivity.EXTRA_SUBSCRIPTION_BASE_URL, sub.baseUrl)
                                putExtra(MainActivity.EXTRA_SUBSCRIPTION_TOPIC, sub.topic)
                                putExtra(MainActivity.EXTRA_SUBSCRIPTION_DISPLAY_NAME, label)
                                putExtra(MainActivity.EXTRA_SUBSCRIPTION_INSTANT, sub.instant)
                                putExtra(MainActivity.EXTRA_SUBSCRIPTION_MUTED_UNTIL, sub.mutedUntil)
                            }
                            val pendingIntent = PendingIntent.getActivity(
                                context, sub.id.toInt(), detailIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            views.setOnClickPendingIntent(cardContainers[i], pendingIntent)
                        } else {
                            views.setViewVisibility(cardContainers[i], android.view.View.GONE)
                        }
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

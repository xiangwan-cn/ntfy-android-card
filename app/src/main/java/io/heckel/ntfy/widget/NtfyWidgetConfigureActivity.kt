package io.heckel.ntfy.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import io.heckel.ntfy.R
import io.heckel.ntfy.db.Repository
import io.heckel.ntfy.util.displayName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class NtfyWidgetConfigureActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var repository: Repository
    private lateinit var appBaseUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget_configure)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        repository = Repository.getInstance(this)
        appBaseUrl = getString(R.string.app_base_url)

        val listView = findViewById<ListView>(R.id.widget_configure_list)

        GlobalScope.launch(Dispatchers.IO) {
            val subscriptions = repository.getSubscriptions()
            val labels = subscriptions.map { sub ->
                "${displayName(appBaseUrl, sub)}  (${sub.totalCount})"
            }

            runOnUiThread {
                val adapter = ArrayAdapter(
                    this@NtfyWidgetConfigureActivity,
                    android.R.layout.simple_list_item_1,
                    labels
                )
                listView.adapter = adapter

                listView.setOnItemClickListener { _, _, position, _ ->
                    val sub = subscriptions[position]
                    repository.setWidgetSubscriptionId(appWidgetId, sub.id)

                    val appWidgetManager = AppWidgetManager.getInstance(this@NtfyWidgetConfigureActivity)
                    NtfyWidgetProvider.updateAppWidget(this@NtfyWidgetConfigureActivity, appWidgetManager, appWidgetId)

                    val resultValue = Intent()
                    resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    setResult(RESULT_OK, resultValue)
                    finish()
                }
            }
        }
    }
}

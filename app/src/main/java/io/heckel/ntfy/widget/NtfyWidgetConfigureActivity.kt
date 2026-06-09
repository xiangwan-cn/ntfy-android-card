package io.heckel.ntfy.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.CompoundButtonCompat
import io.heckel.ntfy.R
import io.heckel.ntfy.db.Repository
import io.heckel.ntfy.util.displayName
import java.util.concurrent.Executors

class NtfyWidgetConfigureActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var repository: Repository
    private lateinit var appBaseUrl: String
    private var transparency = 15
    private var textColorHex = "#FFFFFF"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget_configure)

        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        repository = Repository.getInstance(this)
        appBaseUrl = getString(R.string.app_base_url)

        val listView = findViewById<ListView>(R.id.widget_configure_list)
        val seekBar = findViewById<SeekBar>(R.id.widget_transparency_seekbar)
        val label = findViewById<TextView>(R.id.widget_transparency_label)
        val textColorGroup = findViewById<RadioGroup>(R.id.widget_text_color_group)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                transparency = p
                label.text = "${p}%"
            }
            override fun onStartTrackingTouch(s: SeekBar) {}
            override fun onStopTrackingTouch(s: SeekBar) {}
        })

        textColorGroup.setOnCheckedChangeListener { _, checkedId ->
            textColorHex = when (checkedId) {
                R.id.widget_text_color_white -> "#FFFFFF"
                R.id.widget_text_color_light -> "#CCCCCC"
                R.id.widget_text_color_dark -> "#333333"
                else -> "#FFFFFF"
            }
        }

        CompoundButtonCompat.setButtonTintList(
            findViewById(R.id.widget_text_color_white),
            ColorStateList.valueOf(Color.WHITE)
        )
        CompoundButtonCompat.setButtonTintList(
            findViewById(R.id.widget_text_color_light),
            ColorStateList.valueOf(Color.parseColor("#CCCCCC"))
        )
        CompoundButtonCompat.setButtonTintList(
            findViewById(R.id.widget_text_color_dark),
            ColorStateList.valueOf(Color.parseColor("#333333"))
        )

        Executors.newSingleThreadExecutor().execute {
            val subscriptions = kotlinx.coroutines.runBlocking { repository.getSubscriptions() }
            val labels = subscriptions.map { sub ->
                "${displayName(appBaseUrl, sub)}  (${sub.totalCount})"
            }
            runOnUiThread {
                listView.adapter = ArrayAdapter(
                    this@NtfyWidgetConfigureActivity,
                    android.R.layout.simple_list_item_1, labels
                )
                listView.setOnItemClickListener { _, _, position, _ ->
                    val sub = subscriptions[position]
                    repository.setWidgetSubscriptionId(appWidgetId, sub.id)
                    repository.setWidgetTransparency(appWidgetId, transparency)
                    repository.setWidgetTextColor(appWidgetId, textColorHex)
                    updateWidgetAndFinish()
                }
            }
        }
    }

    private fun updateWidgetAndFinish() {
        Thread {
            NtfyWidgetProvider.updateAppWidget(
                this,
                AppWidgetManager.getInstance(this),
                appWidgetId
            )
            runOnUiThread {
                setResult(RESULT_OK, Intent().apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                })
                finish()
            }
        }.start()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }
}

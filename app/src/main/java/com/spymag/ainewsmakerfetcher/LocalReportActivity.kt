package com.spymag.ainewsmakerfetcher

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlin.concurrent.thread

class LocalReportActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_local_report)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.show(WindowInsetsCompat.Type.systemBars())
        val isLightMode =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) !=
                Configuration.UI_MODE_NIGHT_YES
        controller.isAppearanceLightStatusBars = isLightMode

        val root: View = findViewById(R.id.rootLayout)
        val typedArray = theme.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
        val actionBarHeight = typedArray.getDimensionPixelSize(0, 0)
        typedArray.recycle()
        val start = root.paddingLeft
        val end = root.paddingRight
        val bottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                start + systemBars.left,
                systemBars.top + actionBarHeight,
                end + systemBars.right,
                bottom + systemBars.bottom
            )
            WindowInsetsCompat.CONSUMED
        }

        val uriString = intent.getStringExtra("uri")
        val textView: TextView = findViewById(R.id.tvContent)
        if (uriString != null) {
            val uri = Uri.parse(uriString)
            thread {
                try {
                    val text = contentResolver.openInputStream(uri)?.bufferedReader()
                        ?.use { it.readText() } ?: "Failed to load file."
                    runOnUiThread { textView.text = text }
                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread { textView.text = "Failed to load file." }
                }
            }
        }
    }
}

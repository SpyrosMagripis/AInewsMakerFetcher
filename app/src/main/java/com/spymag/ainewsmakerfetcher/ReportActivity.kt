package com.spymag.ainewsmakerfetcher

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.net.URL
import kotlin.concurrent.thread

class ReportActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        val url = intent.getStringExtra("url")
        val textView: TextView = findViewById(R.id.tvContent)
        if (url != null) {
            thread {
                try {
                    val content = URL(url).readText()
                    runOnUiThread { textView.text = content }
                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread { textView.text = "Failed to load report." }
                }
            }
        }
    }
}

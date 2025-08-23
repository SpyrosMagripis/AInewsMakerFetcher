package com.spymag.ainewsmakerfetcher

import android.os.Bundle
import android.widget.TextView
import android.text.method.LinkMovementMethod
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
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
                    val parser = Parser.builder().build()
                    val document = parser.parse(content)
                    val renderer = HtmlRenderer.builder().build()
                    val html = renderer.render(document)
                    runOnUiThread {
                        textView.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
                        textView.movementMethod = LinkMovementMethod.getInstance()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread { textView.text = "Failed to load report." }
                }
            }
        }
    }
}

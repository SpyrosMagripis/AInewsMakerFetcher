package com.spymag.ainewsmakerfetcher

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.text.method.LinkMovementMethod
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.text.HtmlCompat
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import java.net.URL
import kotlin.concurrent.thread

class ReportActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_report)
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

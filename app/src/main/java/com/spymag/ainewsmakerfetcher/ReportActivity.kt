package com.spymag.ainewsmakerfetcher

import android.os.Bundle
import android.view.View
import android.webkit.WebViewClient
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.renderer.html.AttributeProvider
import org.commonmark.renderer.html.AttributeProviderContext
import org.commonmark.renderer.html.AttributeProviderFactory
import org.commonmark.node.Node
import org.commonmark.node.FencedCodeBlock
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class CodeLanguageAttributeProvider : AttributeProvider {
    override fun setAttributes(node: Node, tagName: String, attributes: MutableMap<String, String>) {
        if (node is FencedCodeBlock && tagName == "pre") {
            val info = node.info
            if (!info.isNullOrEmpty()) {
                val language = info.trim().split(" ")[0]
                attributes["class"] = "highlight language-$language"
            } else {
                attributes["class"] = "highlight"
            }
        }
    }
}

class CodeLanguageAttributeProviderFactory : AttributeProviderFactory {
    override fun create(context: AttributeProviderContext): AttributeProvider {
        return CodeLanguageAttributeProvider()
    }
}

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
        val webView: HorizontalScrollWebView = findViewById(R.id.webViewContent)
        
        // Configure WebView
        webView.webViewClient = WebViewClient()
        webView.settings.apply {
            javaScriptEnabled = false // Keep JS disabled for security
            builtInZoomControls = true
            displayZoomControls = false // Hide zoom controls UI but keep pinch-to-zoom
            setSupportZoom(true)
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            textZoom = 100 // Consistent text sizing
        }
        
        if (url != null) {
            thread {
                try {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    
                    // Add GitHub authentication if PAT is available
                    val githubPat = BuildConfig.GITHUB_PAT
                    if (githubPat.isNotEmpty()) {
                        connection.setRequestProperty("Authorization", "Bearer $githubPat")
                    }
                    
                    connection.connect()
                    val content = connection.inputStream.bufferedReader().use { it.readText() }
                    
                    // Configure CommonMark with GitHub-flavored markdown extensions
                    val extensions = listOf(
                        TablesExtension.create(),
                        StrikethroughExtension.create(),
                        TaskListItemsExtension.create(),
                        AutolinkExtension.create()
                    )
                    
                    val parser = Parser.builder()
                        .extensions(extensions)
                        .build()
                    
                    val document = parser.parse(content)
                    
                    val renderer = HtmlRenderer.builder()
                        .extensions(extensions)
                        .attributeProviderFactory(CodeLanguageAttributeProviderFactory())
                        .build()
                    
                    val html = renderer.render(document)
                    
                    // Create complete HTML document with GitHub-like styling
                    val completeHtml = createStyledHtml(html)
                    
                    runOnUiThread {
                        webView.loadDataWithBaseURL(
                            "file:///android_asset/",
                            completeHtml,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread { 
                        webView.loadData(
                            "<html><body><h2>Failed to load report.</h2><p>Please check your internet connection and try again.</p></body></html>",
                            "text/html",
                            "UTF-8"
                        )
                    }
                }
            }
        }
    }
    
    private fun createStyledHtml(markdownHtml: String): String {
        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        ${loadCssFromAssets()}
    </style>
</head>
<body>
    <div class="markdown-body">
        $markdownHtml
    </div>
</body>
</html>
        """.trimIndent()
    }
    
    private fun loadCssFromAssets(): String {
        return try {
            assets.open("github_markdown.css").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback minimal CSS with GitHub-like styling
            """
            body {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Noto Sans', Helvetica, Arial, sans-serif;
                font-size: 16px;
                line-height: 1.5;
                color: #24292f;
                background-color: #ffffff;
                margin: 0;
                padding: 16px;
                word-wrap: break-word;
            }
            .markdown-body {
                box-sizing: border-box;
                max-width: 980px;
                margin: 0 auto;
            }
            h1, h2 {
                padding-bottom: 0.3em;
                border-bottom: 1px solid #d0d7de;
            }
            h1, h2, h3, h4, h5, h6 {
                margin-top: 24px;
                margin-bottom: 16px;
                font-weight: 600;
                line-height: 1.25;
            }
            pre {
                background-color: #f6f8fa;
                padding: 16px;
                border-radius: 6px;
                overflow-x: auto;
                font-family: ui-monospace, SFMono-Regular, 'SF Mono', Consolas, monospace;
            }
            code {
                background-color: rgba(175, 184, 193, 0.2);
                padding: 0.2em 0.4em;
                border-radius: 6px;
                font-family: ui-monospace, SFMono-Regular, 'SF Mono', Consolas, monospace;
                font-size: 85%;
            }
            pre code {
                background-color: transparent;
                padding: 0;
            }
            blockquote {
                padding: 0 1em;
                color: #656d76;
                border-left: 0.25em solid #d0d7de;
                margin: 0 0 16px;
            }
            table {
                border-collapse: collapse;
                margin: 16px 0;
            }
            table th, table td {
                border: 1px solid #d0d7de;
                padding: 6px 13px;
            }
            table tr:nth-child(2n) {
                background-color: #f6f8fa;
            }
            a {
                color: #0969da;
                text-decoration: none;
            }
            a:hover {
                text-decoration: underline;
            }
            """.trimIndent()
        }
    }
}

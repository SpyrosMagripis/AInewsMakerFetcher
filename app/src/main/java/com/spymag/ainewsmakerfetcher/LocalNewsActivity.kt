package com.spymag.ainewsmakerfetcher

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.documentfile.provider.DocumentFile

class LocalNewsActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var adapter: LocalNewsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_local_news)
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

        listView = findViewById(R.id.listLocalReports)
        adapter = LocalNewsAdapter(this, mutableListOf())
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ ->
            val item = adapter.getItem(position)
            if (item != null) {
                val intent = Intent(this, LocalReportActivity::class.java)
                intent.putExtra("uri", item.uri.toString())
                startActivity(intent)
            }
        }

        findViewById<Button>(R.id.btnPickFolder).setOnClickListener { openFolderPicker() }

        val prefs = getSharedPreferences("local_news", MODE_PRIVATE)
        val uriString = prefs.getString("treeUri", null)
        if (uriString != null) {
            loadArticles(Uri.parse(uriString))
        }
    }

    private fun openFolderPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, REQUEST_CODE_PICK_FOLDER)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_FOLDER && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            getSharedPreferences("local_news", MODE_PRIVATE).edit()
                .putString("treeUri", uri.toString()).apply()
            loadArticles(uri)
        }
    }

    private fun loadArticles(treeUri: Uri) {
        val root = DocumentFile.fromTreeUri(this, treeUri) ?: return
        val items = mutableListOf<LocalArticle>()
        for (file in root.listFiles()) {
            if (file.isFile && file.name?.endsWith(".txt", true) == true) {
                val text = contentResolver.openInputStream(file.uri)?.bufferedReader()
                    ?.use { it.readText() }.orEmpty()
                val preview = text.lineSequence().firstOrNull() ?: ""
                items.add(LocalArticle(file.name ?: "", preview, file.uri))
            }
        }
        adapter.update(items)
    }

    companion object {
        private const val REQUEST_CODE_PICK_FOLDER = 1001
    }
}

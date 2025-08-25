package com.spymag.ainewsmakerfetcher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment

class OneDriveNewsFragment : Fragment() {
    
    private lateinit var listView: ListView
    private lateinit var adapter: LocalNewsAdapter
    private lateinit var tvSelectedPath: TextView
    private lateinit var btnPickFolder: Button
    
    companion object {
        private const val DEFAULT_PATH = "personal/spyros_magripis_nov_com/Documents/ActionsForToday"
        private const val PREFS_NAME = "onedrive_settings"
        private const val KEY_TREE_URI = "treeUri"
    }
    
    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            saveSelectedPath(uri)
            loadArticles(uri)
            updatePathDisplay(uri)
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_onedrive_news, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        listView = view.findViewById(R.id.listLocalReports)
        adapter = LocalNewsAdapter(requireContext(), mutableListOf())
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ ->
            val item = adapter.getItem(position)
            if (item != null) {
                val intent = Intent(requireContext(), LocalReportActivity::class.java)
                intent.putExtra("uri", item.uri.toString())
                startActivity(intent)
            }
        }
        
        tvSelectedPath = view.findViewById(R.id.tvSelectedPath)
        btnPickFolder = view.findViewById(R.id.btnPickFolder)
        
        btnPickFolder.setOnClickListener { openFolderPicker() }
        
        // Load saved path or show default
        loadSavedPathOrDefault()
    }
    
    private fun openFolderPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        folderPickerLauncher.launch(intent)
    }
    
    private fun loadSavedPathOrDefault() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uriString = prefs.getString(KEY_TREE_URI, null)
        
        if (uriString != null) {
            try {
                val uri = Uri.parse(uriString)
                loadArticles(uri)
                updatePathDisplay(uri)
            } catch (e: Exception) {
                // If the saved URI is invalid, show default path suggestion
                showDefaultPathSuggestion()
            }
        } else {
            showDefaultPathSuggestion()
        }
    }
    
    private fun showDefaultPathSuggestion() {
        tvSelectedPath.text = "Suggested path: $DEFAULT_PATH"
        adapter.update(emptyList())
    }
    
    private fun saveSelectedPath(uri: Uri) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_TREE_URI, uri.toString()).apply()
    }
    
    private fun updatePathDisplay(uri: Uri) {
        val docFile = DocumentFile.fromTreeUri(requireContext(), uri)
        val displayName = docFile?.name ?: "Unknown folder"
        tvSelectedPath.text = "Selected: $displayName"
    }
    
    private fun loadArticles(treeUri: Uri) {
        try {
            val root = DocumentFile.fromTreeUri(requireContext(), treeUri) ?: return
            val items = mutableListOf<LocalArticle>()
            
            for (file in root.listFiles()) {
                if (file.isFile && file.name?.endsWith(".txt", true) == true) {
                    val text = requireContext().contentResolver.openInputStream(file.uri)?.bufferedReader()
                        ?.use { it.readText() }.orEmpty()
                    val preview = text.lineSequence().firstOrNull() ?: ""
                    items.add(LocalArticle(file.name ?: "", preview, file.uri))
                }
            }
            adapter.update(items)
        } catch (e: Exception) {
            e.printStackTrace()
            adapter.update(emptyList())
        }
    }
}
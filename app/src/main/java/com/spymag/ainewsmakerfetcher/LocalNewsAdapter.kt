package com.spymag.ainewsmakerfetcher

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class LocalNewsAdapter(context: Context, private val items: MutableList<LocalArticle>) :
    ArrayAdapter<LocalArticle>(context, 0, items) {

    fun update(newItems: List<LocalArticle>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_local_article, parent, false)
        val item = items[position]
        val titleView: TextView = view.findViewById(R.id.tvTitle)
        val previewView: TextView = view.findViewById(R.id.tvPreview)
        titleView.text = item.title
        previewView.text = item.preview
        return view
    }
}

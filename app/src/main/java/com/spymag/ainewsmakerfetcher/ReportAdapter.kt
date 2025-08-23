package com.spymag.ainewsmakerfetcher

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class ReportAdapter(context: Context, private val items: MutableList<Report>) :
    ArrayAdapter<Report>(context, 0, items) {

    fun update(newItems: List<Report>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_report, parent, false)
        val item = items[position]
        val titleView: TextView = view.findViewById(R.id.tvTitle)
        val dateView: TextView = view.findViewById(R.id.tvDate)
        titleView.text = item.name
        dateView.text = item.date.toString()
        return view
    }
}

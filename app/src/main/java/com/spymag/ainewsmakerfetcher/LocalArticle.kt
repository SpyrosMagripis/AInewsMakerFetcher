package com.spymag.ainewsmakerfetcher

import android.net.Uri

/**
 * Represents a local text file to be displayed as a news item.
 */
data class LocalArticle(
    val title: String,
    val preview: String,
    val uri: Uri
)

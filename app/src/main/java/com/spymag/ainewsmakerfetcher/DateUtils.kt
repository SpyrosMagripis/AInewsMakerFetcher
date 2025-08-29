package com.spymag.ainewsmakerfetcher

import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dateRegex = Regex("(\\d{4}[-_]\\d{2}[-_]\\d{2}|\\d{8})")

/**
 * Extracts [LocalDate] from a filename.
 *
 * Supports `yyyy-MM-dd`, `yyyy_MM_dd`, and `yyyyMMdd` formats.
 */
fun parseDateFromFileName(name: String): LocalDate? {
    val value = dateRegex.find(name)?.value ?: return null
    return when {
        value.contains('-') || value.contains('_') ->
            runCatching { LocalDate.parse(value.replace('_', '-')) }.getOrNull()
        else ->
            runCatching { LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE) }.getOrNull()
    }
}


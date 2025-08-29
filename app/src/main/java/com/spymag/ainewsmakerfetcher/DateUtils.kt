package com.spymag.ainewsmakerfetcher

import java.time.LocalDate

private val dateRegex = Regex("(\\d{4}[-_]\\d{2}[-_]\\d{2})")

fun parseDateFromFileName(name: String): LocalDate? {
    val match = dateRegex.find(name) ?: return null
    val normalized = match.value.replace('_', '-')
    return runCatching { LocalDate.parse(normalized) }.getOrNull()
}


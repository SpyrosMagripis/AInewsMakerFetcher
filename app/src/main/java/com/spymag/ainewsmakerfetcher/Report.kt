package com.spymag.ainewsmakerfetcher

import java.time.LocalDate

data class Report(
    val name: String,
    val date: LocalDate,
    val url: String
)

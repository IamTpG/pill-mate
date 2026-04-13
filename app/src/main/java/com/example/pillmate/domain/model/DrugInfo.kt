package com.example.pillmate.domain.model

data class DrugInfo(
    val brandName: String,
    val genericName: String,
    val warnings: String?,
    val infoSections: List<Pair<String, String>>
)

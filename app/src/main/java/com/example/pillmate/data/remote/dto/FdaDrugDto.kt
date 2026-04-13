package com.example.pillmate.data.remote.dto

import com.google.gson.annotations.SerializedName

data class FdaResponseDto(
    @SerializedName("meta") val meta: FdaMetaDto?,
    @SerializedName("results") val results: List<FdaDrugDto>?
)

data class FdaMetaDto(
    @SerializedName("results") val results: FdaMetaResultDto?
)

data class FdaMetaResultDto(
    @SerializedName("skip") val skip: Int?,
    @SerializedName("limit") val limit: Int?,
    @SerializedName("total") val total: Int?
)

data class FdaDrugDto(
    @SerializedName("indications_and_usage") val indicationsAndUsage: List<String>?,
    @SerializedName("dosage_and_administration") val dosageAndAdministration: List<String>?,
    @SerializedName("contraindications") val contraindications: List<String>?,
    @SerializedName("warnings") val warnings: List<String>?,
    @SerializedName("active_ingredient") val activeIngredient: List<String>?,
    @SerializedName("inactive_ingredient") val inactiveIngredient: List<String>?,
    @SerializedName("purpose") val purpose: List<String>?,
    @SerializedName("do_not_use") val doNotUse: List<String>?,
    @SerializedName("stop_use") val stopUse: List<String>?,
    @SerializedName("pregnancy_or_breast_feeding") val pregnancyOrBreastFeeding: List<String>?,
    @SerializedName("keep_out_of_reach_of_children") val keepOutOfReachOfChildren: List<String>?,
    @SerializedName("storage_and_handling") val storageAndHandling: List<String>?,
    @SerializedName("questions") val questions: List<String>?,
    @SerializedName("openfda") val openFda: FdaOpenFdaDto?
)

data class FdaOpenFdaDto(
    @SerializedName("brand_name") val brandName: List<String>?,
    @SerializedName("generic_name") val genericName: List<String>?
)

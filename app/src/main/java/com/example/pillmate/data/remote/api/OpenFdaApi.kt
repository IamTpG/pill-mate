package com.example.pillmate.data.remote.api

import com.example.pillmate.data.remote.dto.FdaResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenFdaApi {
    /**
     * openFDA enforces query syntax like: search=openfda.brand_name:acetaminophen+OR+openfda.generic_name:acetaminophen
     */
    @GET("drug/label.json")
    suspend fun searchDrugInfo(
        @Query("search", encoded = true) searchQuery: String,
        @Query("limit") limit: Int = 10
    ): FdaResponseDto
}

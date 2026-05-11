package com.example.pillmate.data.remote.api

import com.example.pillmate.data.remote.dto.PlaceResult
import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimService {
	@GET("search")
	suspend fun searchPlace(
		@Query("q") query: String,
		@Query("format") format: String = "json",
		@Query("limit") limit: Int = 5
	): List<PlaceResult>
}
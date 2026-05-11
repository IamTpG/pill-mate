package com.example.pillmate.data.remote.api

import retrofit2.http.Query
import retrofit2.Response
import retrofit2.http.GET

data class RecipeResponse(val recipes: List<Recipe>) // Cho random
data class SearchResponse(val results: List<Recipe>) // Cho search

data class Recipe(
	val id: Int,
	val title: String,
	val image: String?,
	val sourceUrl: String?
)

// Retrofit Interface
interface SpoonacularApi {
	@GET("recipes/random")
	suspend fun getRandomRecipes(
		@Query("number") number: Int = 20,
		@Query("apiKey") apiKey: String = "998ff97646634c79890886bcaa0042b4"
	): Response<RecipeResponse>
	
	@GET("recipes/complexSearch")
	suspend fun searchRecipes(
		@Query("query") query: String,
		@Query("addRecipeInformation") addRecipeInfo: Boolean = true,
		@Query("number") number: Int = 20,
		@Query("apiKey") apiKey: String = "998ff97646634c79890886bcaa0042b4"
	): Response<SearchResponse>
}


package com.example.pillmate.util

import com.example.pillmate.data.remote.api.SpoonacularApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
	
	private const val BASE_URL = "https://api.spoonacular.com/"
	private const val API_KEY = "998ff97646634c79890886bcaa0042b4"
	
	private val okHttpClient = OkHttpClient.Builder()
		.addInterceptor { chain ->
			val url = chain.request().url.newBuilder()
				.addQueryParameter("apiKey", API_KEY)
				.build()
			chain.proceed(chain.request().newBuilder().url(url).build())
		}
		.build()
	
	val spoonacularApi: SpoonacularApi = Retrofit.Builder()
		.baseUrl(BASE_URL)
		.client(okHttpClient)
		.addConverterFactory(GsonConverterFactory.create())
		.build()
		.create(SpoonacularApi::class.java)
}
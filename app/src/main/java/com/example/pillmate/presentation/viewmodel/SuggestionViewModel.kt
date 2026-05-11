package com.example.pillmate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillmate.data.remote.api.Recipe
import com.example.pillmate.data.remote.api.SpoonacularApi
import com.example.pillmate.util.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

enum class SuggestionType { MEAL, EXERCISE }

class SuggestionViewModel : ViewModel() {
	private val api = RetrofitClient.spoonacularApi
		
	private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
	val recipes: StateFlow<List<Recipe>> = _recipes.asStateFlow()
	
	private val _isLoading = MutableStateFlow(false)
	val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
	
	private val _errorMessage = MutableStateFlow<String?>(null)
	val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
	private val _currentType = MutableStateFlow(SuggestionType.MEAL)
	val currentType: StateFlow<SuggestionType> = _currentType.asStateFlow()
	
	init {
		fetchRandomRecipes()
	}
	
	fun setType(type: SuggestionType) {
		_currentType.value = type
		if (type == SuggestionType.MEAL) {
			fetchRandomRecipes()
		} else {
			// TODO: Xử lý gọi API cho Exercise sau này
			_recipes.value = emptyList()
		}
	}
	
	fun fetchRandomRecipes() {
		if (_currentType.value != SuggestionType.MEAL) return
		viewModelScope.launch {
			_isLoading.value = true
			_errorMessage.value = null
			try {
				val response = api.getRandomRecipes() // dùng default number=20, apiKey đã có sẵn
				if (response.isSuccessful) {
					_recipes.value = response.body()?.recipes ?: emptyList()
				} else {
					_errorMessage.value = "Lỗi ${response.code()}: ${response.message()}"
				}
			} catch (e: Exception) {
				_errorMessage.value = "Không thể kết nối: ${e.localizedMessage}"
			} finally {
				_isLoading.value = false
			}
		}
	}
	
	fun searchRecipes(query: String) {
		if (query.isBlank() || _currentType.value != SuggestionType.MEAL) {
			fetchRandomRecipes()
			return
		}
		viewModelScope.launch {
			_isLoading.value = true
			_errorMessage.value = null
			try {
				val response = api.searchRecipes(query = query) // dùng default number=20, apiKey đã có sẵn
				if (response.isSuccessful) {
					_recipes.value = response.body()?.results ?: emptyList()
				} else {
					_errorMessage.value = "Lỗi ${response.code()}: ${response.message()}"
				}
			} catch (e: Exception) {
				_errorMessage.value = "Không thể kết nối: ${e.localizedMessage}"
			} finally {
				_isLoading.value = false
			}
		}
	}
}
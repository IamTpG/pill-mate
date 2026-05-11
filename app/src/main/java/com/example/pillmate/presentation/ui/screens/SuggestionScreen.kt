package com.example.pillmate.presentation.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavController

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.pillmate.data.remote.api.Recipe
import com.example.pillmate.presentation.viewmodel.SuggestionType
import com.example.pillmate.presentation.viewmodel.SuggestionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionScreen(
	viewModel: SuggestionViewModel,
	navController: NavController
) {
	val recipes by viewModel.recipes.collectAsState()
	val isLoading by viewModel.isLoading.collectAsState()
	val currentType by viewModel.currentType.collectAsState()
	
	Scaffold(
		topBar = {
			SuggestionTopBar(
				currentType = currentType,
				onBackClick = { navController.popBackStack()},
				onTypeSelected = { viewModel.setType(it) }
			)
		}
	) { paddingValues ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(paddingValues)
				.padding(horizontal = 16.dp)
		) {
			// Thanh tìm kiếm
			SearchBarComponent(
				onSearch = { query -> viewModel.searchRecipes(query) },
				onClear = { viewModel.fetchRandomRecipes() },
				onRefresh = { viewModel.fetchRandomRecipes() }
			)
			
			Spacer(modifier = Modifier.height(16.dp))
			
			// Hiển thị danh sách hoặc Loading
			if (isLoading) {
				Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
					CircularProgressIndicator()
				}
			} else {
				LazyColumn(
					verticalArrangement = Arrangement.spacedBy(12.dp),
					contentPadding = PaddingValues(bottom = 16.dp)
				) {
					items(recipes) { recipe ->
						RecipeItemComponent(recipe = recipe)
					}
				}
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionTopBar(
	currentType: SuggestionType,
	onBackClick: () -> Unit,
	onTypeSelected: (SuggestionType) -> Unit
) {
	var expanded by remember { mutableStateOf(false) }
	
	TopAppBar(
		title = { Text("Gợi ý") },
		navigationIcon = {
			IconButton(onClick = onBackClick) {
				Icon(Icons.Default.ArrowBack, contentDescription = "Back")
			}
		},
		actions = {
			Box {
				TextButton(onClick = { expanded = true }) {
					Text(currentType.name)
					Icon(Icons.Default.ArrowDropDown, contentDescription = null)
				}
				DropdownMenu(
					expanded = expanded,
					onDismissRequest = { expanded = false }
				) {
					DropdownMenuItem(
						text = { Text("Meal") },
						onClick = {
							onTypeSelected(SuggestionType.MEAL)
							expanded = false
						}
					)
					DropdownMenuItem(
						text = { Text("Exercise") },
						onClick = {
							onTypeSelected(SuggestionType.EXERCISE)
							expanded = false
						}
					)
				}
			}
		}
	)
}

@Composable
fun SearchBarComponent(
	onSearch: (String) -> Unit,
	onClear: () -> Unit,
	onRefresh: () -> Unit
) {
	var searchText by remember { mutableStateOf("") }
	
	Row(
		modifier = Modifier.fillMaxWidth(),
		verticalAlignment = Alignment.CenterVertically
	) {
		OutlinedTextField(
			value = searchText,
			onValueChange = {
				searchText = it
				onSearch(it) // Tuỳ chọn: Có thể thêm debounce ở đây để tối ưu API
			},
			modifier = Modifier.weight(1f),
			placeholder = { Text("Tìm kiếm món ăn...") },
			singleLine = true,
			trailingIcon = {
				if (searchText.isNotEmpty()) {
					IconButton(onClick = {
						searchText = ""
						onClear()
					}) {
						Icon(Icons.Default.Clear, contentDescription = "Clear")
					}
				}
			}
		)
		
		Spacer(modifier = Modifier.width(8.dp))
		
		IconButton(onClick = onRefresh) {
			Icon(Icons.Default.Refresh, contentDescription = "Refresh Random")
		}
	}
}

@Composable
fun RecipeItemComponent(recipe: Recipe) {
	// LocalUriHandler giúp mở link ra trình duyệt ngoài dễ dàng
	val uriHandler = LocalUriHandler.current
	
	Card(
		modifier = Modifier.fillMaxWidth(),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
		shape = RoundedCornerShape(12.dp)
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(8.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			// Hình ảnh bên trái
			AsyncImage(
				model = recipe.image,
				contentDescription = recipe.title,
				modifier = Modifier
					.size(80.dp)
					.clip(RoundedCornerShape(8.dp)),
				contentScale = ContentScale.Crop
			)
			
			Spacer(modifier = Modifier.width(16.dp))
			
			// Cột Text bên phải
			Column(
				modifier = Modifier.weight(1f)
			) {
				Text(
					text = recipe.title,
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.Bold,
					maxLines = 2
				)
				
				Spacer(modifier = Modifier.height(4.dp))
				
				// Nút Click xem chi tiết
				if (!recipe.sourceUrl.isNullOrEmpty()) {
					Text(
						text = "Thông tin chi tiết",
						color = MaterialTheme.colorScheme.primary,
						textDecoration = TextDecoration.Underline,
						modifier = Modifier.clickable {
							uriHandler.openUri(recipe.sourceUrl)
						}
					)
				}
			}
		}
	}
}
package com.example.pillmate.presentation.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.pillmate.R
import com.example.pillmate.domain.model.AppointmentLog

/**
 * A self-contained search bar for filtering [AppointmentLog] items.
 *
 * Abort mechanisms:
 *  1. Parent toggles [showSearchBar] to false via the "Search" button in [AppointmentHeader].
 *  2. Tapping the trailing close icon:
 *       - Clears the query when text is present.
 *       - Hides the bar (calls [onDismiss]) when the query is already empty.
 *
 * @param logs          The full unfiltered list of [AppointmentLog] items to search through.
 * @param searchQuery   The current query string, hoisted to the parent so it can drive
 *                      the filtered list displayed in [AppointmentScreen].
 * @param onQueryChange Called whenever the user edits the text field.
 * @param onDismiss     Called when the bar should be hidden entirely (close icon pressed
 *                      with an empty query). Parent should set [showSearchBar] = false here.
 * @param modifier      Optional [Modifier] applied to the [SearchBar] root.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentSearchBar(
	logs: List<AppointmentLog>,
	searchQuery: String,
	onQueryChange: (String) -> Unit,
	onDismiss: () -> Unit,
	modifier: Modifier = Modifier
) {
	// Internal state — the parent has no need to know whether suggestions are expanded
	var isSearchActive by remember { mutableStateOf(false) }
	
	val suggestions = logs
		.filter { it.name.contains(searchQuery, ignoreCase = true) }
		.distinctBy { it.name }
	
	SearchBar(
		query = searchQuery,
		onQueryChange = onQueryChange,
		// "Enter" on the keyboard commits the query and collapses suggestions
		onSearch = { isSearchActive = false },
		active = isSearchActive,
		onActiveChange = { isSearchActive = it },
		modifier = modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp)
			,
		placeholder = { Text("Search appointment name...") },
		leadingIcon = {
			Icon(Icons.Default.Search, contentDescription = "Search")
		},
		trailingIcon = {
			// Show the close icon whenever there is text OR the suggestions panel is open
			if (searchQuery.isNotEmpty() || isSearchActive) {
				IconButton(
					onClick = {
						when {
							// 1st press: clear only the text, keep bar visible
							searchQuery.isNotEmpty() -> onQueryChange("")
							// 2nd press (query already empty): hide the bar entirely
							else -> {
								isSearchActive = false
								onDismiss()
							}
						}
					}
				) {
					Icon(
						painter = painterResource(id = R.drawable.ic_closing),
						contentDescription = "Clear / Close search"
					)
				}
			}
		}
	) {
		// Suggestions drop-down
		LazyColumn(
			modifier = Modifier.fillMaxWidth(),
			contentPadding = PaddingValues(16.dp),
			verticalArrangement = Arrangement.spacedBy(8.dp)
		) {
			items(suggestions) { suggestion ->
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.clickable {
							// Selecting a suggestion commits it and collapses the drop-down
							onQueryChange(suggestion.name)
							isSearchActive = false
						}
						.padding(16.dp)
				) {
					Icon(
						painter = painterResource(id = R.drawable.ic_history),
						contentDescription = null,
						tint = Color.Gray,
						modifier = Modifier.padding(end = 16.dp)
					)
					Text(text = suggestion.name)
				}
			}
		}
	}
}
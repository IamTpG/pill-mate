package com.example.pillmate.data.remote.dto

import com.example.pillmate.domain.model.LocationItem


data class PlaceResult(
	val place_id: String,
	val display_name: String,
	val lat: String,
	val lon: String
)

fun PlaceResult.toLocationItem(): LocationItem {
	return LocationItem(
		name = this.display_name,
		latitude = this.lat.toDoubleOrNull() ?: 0.0,
		longitude = this.lon.toDoubleOrNull() ?: 0.0
	)
}

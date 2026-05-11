package com.example.pillmate.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import android.preference.PreferenceManager
import androidx.core.content.ContextCompat
import com.example.pillmate.data.remote.dto.PlaceResult
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import com.example.pillmate.R

@Composable
fun OsmMapView(
	modifier: Modifier = Modifier,
	searchResults: List<PlaceResult>,
	userLocation: GeoPoint? = null
) {
	
	val context = LocalContext.current
	
	LaunchedEffect(Unit) {
		Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
		Configuration.getInstance().userAgentValue = context.packageName
	}
	
	AndroidView(
		modifier = modifier,
		factory = {
			MapView(context).apply {
				setTileSource(TileSourceFactory.MAPNIK)
				setMultiTouchControls(true)
				controller.setZoom(16.0)
				controller.setCenter(GeoPoint(10.8231, 106.6297))
			}
		},
		update = { mapView ->
			mapView.overlays.clear()
			
			userLocation?.let { point ->
				val userMarker = Marker(mapView).apply {
					position = point
					setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
					title = "Vị trí của bạn"
					icon = ContextCompat.getDrawable(mapView.context, R.drawable.ic_location)
				}
				mapView.overlays.add(userMarker)
				
				if (searchResults.isEmpty()) {
					mapView.controller.animateTo(point, 16.0, 1000L)
				}
			}
			
			if (searchResults.isNotEmpty()) {
				val geoPoints = mutableListOf<GeoPoint>()
				
				searchResults.forEach { place ->
					val lat = place.lat.toDoubleOrNull() ?: return@forEach  // ✅ Safe parse
					val lon = place.lon.toDoubleOrNull() ?: return@forEach
					val point = GeoPoint(lat, lon)
					geoPoints.add(point)
					
					val marker = Marker(mapView).apply {
						position = point
						setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
						title = place.display_name
					}
					mapView.overlays.add(marker)
				}
				
				if (geoPoints.isNotEmpty()) {
					if (geoPoints.size == 1) {
						mapView.controller.animateTo(geoPoints.first(), 16.0, 1000L)
					} else {
						val boundingBox = BoundingBox.fromGeoPoints(geoPoints)
						// ✅ Thêm null check và validate boundingBox
						if (boundingBox.latNorth != boundingBox.latSouth ||
							boundingBox.lonEast != boundingBox.lonWest
						) {
							mapView.zoomToBoundingBox(boundingBox, true, 150)
						} else {
							mapView.controller.animateTo(geoPoints.first(), 16.0, 1000L)
						}
					}
				}
			}
			
			mapView.invalidate()
		}
	)

}
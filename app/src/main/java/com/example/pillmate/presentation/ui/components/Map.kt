package com.example.pillmate.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

@Composable
fun Map(modifier: Modifier = Modifier) {
	val cameraState = rememberCameraState(
		firstPosition = CameraPosition(
			target = Position(106.7009, 10.7769), // TP.HCM
			zoom = 12.0
		)
	)
	
	Column(
		modifier = modifier
	) {
		
		
		MaplibreMap(
			cameraState = cameraState,
			baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
			options =
				MapOptions(
					gestureOptions =
						GestureOptions(
							isTiltEnabled = true,
							isZoomEnabled = true,
							isRotateEnabled = true,
							isScrollEnabled = true,
						)
					
				)
		)
	}
}
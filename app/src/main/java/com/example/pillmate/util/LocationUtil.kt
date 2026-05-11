package com.example.pillmate.util

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@SuppressLint("MissingPermission")
suspend fun getCurrentLocation(context: Context): Pair<Double, Double>? {
	val client = LocationServices.getFusedLocationProviderClient(context)
	val cts = CancellationTokenSource()
	
	return suspendCancellableCoroutine { cont ->
		client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
			.addOnSuccessListener { location ->
				if (location != null) {
					cont.resume(Pair(location.latitude, location.longitude))
				} else {
					cont.resume(null)
				}
			}
			.addOnFailureListener {
				cont.resume(null)
			}
		
		cont.invokeOnCancellation { cts.cancel() }
	}
}
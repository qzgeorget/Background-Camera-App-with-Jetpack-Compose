package com.example.voicecam

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

fun getDefaultLocation(): LatLng {
    return LatLng(37.7749, -122.4194)
}

fun setMapStartLocation(googleMap: GoogleMap, location: LatLng){
    googleMap.addMarker(MarkerOptions().position(location).title("Starting Point"))
    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 12f))
}


suspend fun searchLocationAndDrawRoute(context: Context, origin: LatLng, location: String, googleMap: GoogleMap){
    if (location.isEmpty()){
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Please enter a location to search", Toast.LENGTH_SHORT).show()
        }
        return
    }

    val geocoder = android.location.Geocoder(context, Locale.getDefault())
    val addresses = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        geocoder.getFromLocationName(location, 1) // New suspend function in API level 33
    } else {
        @Suppress("DEPRECATION")
        geocoder.getFromLocationName(location, 1)
    }

    if (addresses != null && addresses.isNotEmpty()) {
        val destinationAddress = addresses[0]
        val destination = LatLng(destinationAddress.latitude, destinationAddress.longitude)
        withContext(Dispatchers.Main) {
            drawRoute(context, googleMap, origin, destination)
        }
    }else {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Location not found", Toast.LENGTH_SHORT).show()
        }
    }
}

suspend fun searchLocationAndDrawRoute(context: Context, originQuery: String, destinationQuery: String, googleMap: GoogleMap) {
    if (originQuery.isEmpty() || destinationQuery.isEmpty()) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Please enter both origin and destination locations to search", Toast.LENGTH_SHORT).show()
        }
        return
    }

    val geocoder = android.location.Geocoder(context, Locale.getDefault())
    val originAddresses = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        geocoder.getFromLocationName(originQuery, 1) // New suspend function in API level 33
    } else {
        @Suppress("DEPRECATION")
        geocoder.getFromLocationName(originQuery, 1)
    }

    val destinationAddresses = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        geocoder.getFromLocationName(destinationQuery, 1) // New suspend function in API level 33
    } else {
        @Suppress("DEPRECATION")
        geocoder.getFromLocationName(destinationQuery, 1)
    }

    if (originAddresses != null && originAddresses.isNotEmpty() && destinationAddresses != null && destinationAddresses.isNotEmpty()) {
        val originAddress = originAddresses[0]
        val destinationAddress = destinationAddresses[0]
        val origin = LatLng(originAddress.latitude, originAddress.longitude)
        val destination = LatLng(destinationAddress.latitude, destinationAddress.longitude)
        withContext(Dispatchers.Main) {
            drawRoute(context, googleMap, origin, destination)
        }
    } else {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "One or both locations not found", Toast.LENGTH_SHORT).show()
        }
    }
}

suspend fun drawRoute(context: Context, googleMap: GoogleMap, start: LatLng, end: LatLng){
    val apiKey = context.resources.getString(R.string.google_maps_key)
    val route = withContext(Dispatchers.IO) {
        getRoute(start, end, apiKey)
    }

    if (route != null) {
        val jsonObject = JSONObject(route)
        val status = jsonObject.optString("status")
        if (status == "ZERO_RESULTS") {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "No routes found for the given locations", Toast.LENGTH_SHORT).show()
                return@withContext
            }
        }

        val routePoints = parseDirectionJson(route)
        withContext(Dispatchers.Main) {
            googleMap.clear()
            googleMap.addMarker(MarkerOptions().position(start).title("Start"))
            googleMap.addMarker(MarkerOptions().position(end).title("Destination"))
            if (routePoints.isNotEmpty()) {
                googleMap.addPolyline(PolylineOptions().addAll(routePoints).width(10f).color(android.graphics.Color.BLUE))
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(start, 12f))
            } else {
                Toast.makeText(context, "Unable to draw route", Toast.LENGTH_SHORT).show()
            }
        }
    } else {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Error fetching route data", Toast.LENGTH_SHORT).show()
        }
    }
}

suspend fun getRoute(startLocation: LatLng, endLocation: LatLng, apiKey: String): String? {
    val url =
        "https://maps.googleapis.com/maps/api/directions/json?origin=${startLocation.latitude},${startLocation.longitude}&destination=${endLocation.latitude},${endLocation.longitude}&key=$apiKey"

    Log.d("MapsUtils", "API request: $url") // Log the response
    return withContext(Dispatchers.IO) {
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connect()
            val inputStream = conn.inputStream
            val response = inputStream.bufferedReader().use { it.readText() }
            Log.d("MapsUtils", "API Response: $response") // Log the response
            response
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

fun parseDirectionJson(jsonData: String): List<LatLng> {
    val jsonObject = JSONObject(jsonData)
    val routes = jsonObject.optJSONArray("routes")

    if (routes == null || routes.length() == 0) {
        Log.e("MapsUtils", "No routes found in the JSON response")
        return emptyList()
    }

    val overviewPolyline = routes.getJSONObject(0).optJSONObject("overview_polyline")
    val points = overviewPolyline?.optString("points")

    if (points.isNullOrEmpty()) {
        Log.e("MapsUtils", "No points found in the JSON response")
        return emptyList()
    }

    return decodePolyline(points)
}

fun decodePolyline(encoded: String): List<LatLng> {
    val poly = ArrayList<LatLng>()
    var index = 0
    val len = encoded.length
    var lat = 0
    var lng = 0

    while (index < len) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lat += dlat

        shift = 0
        result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lng += dlng

        val latLng = LatLng(lat / 1E5, lng / 1E5)
        poly.add(latLng)
    }

    return poly
}
package com.example.locaton_map


import android.Manifest
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.example.locaton_map.ui.theme.Locaton_mapTheme
import com.google.android.gms.location.*
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView


class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient:
            FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        if ((ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) !=
                    PackageManager.PERMISSION_GRANTED)
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                0
            )
        }
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            ) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener {
                Log.d(
                    "GEOLOCATION",
                    "last location latitude:${it?.latitude} and longitude: ${it?.longitude}"
                )
            }
        }
        super.onCreate(savedInstanceState)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult
                // e.g. loop through all the locations in the track, very likely, you want only the latest result
                for (location in locationResult.locations) {
                    Log.d(
                        "GEOLOCATION",
                        "location latitude:${location.latitude} and longitude:${location.longitude}"
                    )
                }
            }
        }
        Configuration.getInstance().load(
            this,
            PreferenceManager.getDefaultSharedPreferences(this)
        )
        setContent {
            Locaton_mapTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Column {
                        ShowMap()

                        Button(onClick = {
                            val locationRequest = LocationRequest
                                .create()
                                .setInterval(1000)
                                .setPriority(PRIORITY_HIGH_ACCURACY)
                            //if permissions granted...
                            fusedLocationClient.requestLocationUpdates(
                                locationRequest,
                                locationCallback, Looper.getMainLooper()
                            )

                        }) {

                        }
                    }
                }
            }
        }
    }
}


@Composable
fun composeMap(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            id = R.id.map

        }
    }
    return mapView
}

/*@Composable
fun ShowMap(mapViewModel: MapViewModel) {
    // val map, init...
    // observer (e.g. update from the location change listener)
    val address by mapViewModel.mapData.observeAsState()
    val marker = Marker(map)
    AndroidView({ map }) {
        address ?: return@AndroidView
        it.controller.setCenter(address?.geoPoint)
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.position = address?.geoPoint
        marker.closeInfoWindow()
        marker.title = address?.address
        map.overlays.add(marker)
        map.invalidate()
    }
}*/
@Composable
fun ShowMap() {
    val map = composeMap()
    // hard coded zoom level and map center only at start
    var mapInitialized by remember(map) { mutableStateOf(false) }
    if (!mapInitialized) {
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.controller.setZoom(9.0)
        map.controller.setCenter(GeoPoint(60.17, 24.95))
        mapInitialized = true
    }
    AndroidView({ map })
}
package com.example.locaton_map


import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager
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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import com.example.locaton_map.ui.theme.Locaton_mapTheme
import com.google.android.gms.location.*
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY

import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker


class MainActivity : ComponentActivity() {

    companion object {
        private var mapViewModel = MapViewModel()
    }

    /*    private fun getAddress(lat: Double, lng: Double): String {
             val geocoder = Geocoder(this)
             var address = ""
             if (Build.VERSION.SDK_INT >= 33) {
                5 geocoder.getFromLocation(lat, lng, 1) {
                    6 address = it.first().getAddressLine(0)
                    7 }
                8 } else {
                9 address = geocoder.getFromLocation(lat, lng, 1)?
                .first()?.getAddressLine(0) ?: ""
                10 }
            11 return address
            12 }*/
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
                mapViewModel.geoPoint(it.latitude, it.longitude)
                Log.d(
                    "GEOLOCATION",
                    "last location latitude:${it?.latitude} and longitude: ${it?.longitude}"
                )
            }
        }
        super.onCreate(savedInstanceState)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                // e.g. loop through all the locations in the track, very likely, you want only the latest result
                for (location in locationResult.locations) {
                    mapViewModel.geoPoint(location.latitude, location.longitude)
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
                        Column(modifier = Modifier.weight(0.25f)) {
                            ShowMap(mapViewModel = mapViewModel)
                        }
                        Column(modifier = Modifier.weight(0.25f)) {
                            Button(onClick = {
                                val locationRequest = LocationRequest
                                    .create()
                                    .setInterval(0)
                                    .setPriority(PRIORITY_HIGH_ACCURACY)
                                //if permissions granted...
                                fusedLocationClient.requestLocationUpdates(
                                    locationRequest,
                                    locationCallback, Looper.getMainLooper()
                                )

                            }, modifier = Modifier.weight(0.25f)) {
                                Text(text = "locate me")
                            }
                            Button(onClick = {
                                fusedLocationClient.removeLocationUpdates(locationCallback)
                            }, modifier = Modifier.weight(0.25f)) {
                                Text(text = "Don't locate me")
                            }
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

@Composable
fun ShowMap(mapViewModel: MapViewModel) {
    val map = composeMap()

    val geo by mapViewModel.geoPoint.observeAsState()
    val marker = Marker(map)
    var mapInitialized by remember(map) { mutableStateOf(false) }
    if (!mapInitialized) {
        map.setTileSource(TileSourceFactory.OpenTopo)
        map.controller.setZoom(16.0)
        map.controller.setCenter(geo)
        map.setMultiTouchControls(true)
        mapInitialized = true
    }
    AndroidView({ map }) {
        geo ?: return@AndroidView
        it.controller.setCenter(geo)
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.position = geo
        marker.closeInfoWindow()
        map.setMultiTouchControls(true)
        marker.title = "location latitude:${geo?.latitude} and longitude: ${geo?.longitude}"
        map.overlays.add(marker)
        map.invalidate()
    }
}


class MapViewModel : ViewModel() {
    private val _geoPoint: MutableLiveData<GeoPoint> = MutableLiveData()
    val geoPoint: LiveData<GeoPoint> = _geoPoint

    fun geoPoint(lat: Double, lon: Double) {
        _geoPoint.value = GeoPoint(lat, lon)
    }

    fun getAddress(add: String) {}
}
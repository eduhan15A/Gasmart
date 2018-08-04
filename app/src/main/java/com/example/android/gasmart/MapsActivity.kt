package com.example.android.gasmart

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.location.places.ui.PlacePicker
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import org.jetbrains.anko.custom.async
import org.jetbrains.anko.uiThread
import java.io.IOException
import java.net.URL

class MapsActivity : AppCompatActivity(), OnMapReadyCallback,  GoogleMap.OnMarkerClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    var haspMap = HashMap<String, Station>()
    private lateinit var lastLocation: Location
    // 1
    private lateinit var locationCallback: LocationCallback
    // 2
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        // 3
        private const val REQUEST_CHECK_SETTINGS = 2

        private const val PLACE_PICKER_REQUEST = 3

        val EXTRA_NAME:String = "com.example.android.gasmart.extra.NAME"
        val EXTRA_BRAND:String = "com.example.android.gasmart.extra.BRAND"
        val EXTRA_LOCATION_X:String = "com.example.android.gasmart.extra.LOCATION_X"
        val EXTRA_LOCATION_Y:String = "com.example.android.gasmart.extra.LOCATION_Y"
        val EXTRA_PLACE_ID:String = "com.example.android.gasmart.extra._PLACE_ID"
        val EXTRA_RATING:String = "com.example.android.gasmart.extra.RATING"
        val EXTRA_DISTANCE:String = "com.example.android.gasmart.extra.DISTANCE"




    }
    val TEXT_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)

                lastLocation = p0.lastLocation
                placeMarkerOnMap(LatLng(lastLocation.latitude, lastLocation.longitude))

            }
        }
        createLocationRequest()
        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            loadPlacePicker()
        }
    }


    override fun onMarkerClick(p0: Marker?) = false
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */




    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnInfoWindowClickListener(GoogleMap.OnInfoWindowClickListener() {
       //         Log.d("Marker",it.id + ", " + it.title)

            var station:Station = haspMap.get(it.id)!!
            Log.d("Marker",station._place_id + ", " + it.title)
            val intent = Intent ( this, StationDetails::class.java)

            intent.putExtra(EXTRA_NAME,station.name)
            intent.putExtra(EXTRA_BRAND,station.brand)
            intent.putExtra(EXTRA_LOCATION_X,station.location_x)
            intent.putExtra(EXTRA_LOCATION_Y,station.location_y)
            intent.putExtra(EXTRA_PLACE_ID,station._place_id)
            intent.putExtra(EXTRA_RATING,station.rating.toString())
            intent.putExtra(EXTRA_DISTANCE,station.distance.toString())

            Log.d ("Rating",station.rating.toString())
            //startActivityForResult(intent, TEXT_REQUEST)
            startActivity(intent)

        });

        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL

        mMap.getUiSettings().setZoomControlsEnabled(true)
        mMap.setOnMarkerClickListener(this)

        // Add a marker in Sydney and move the camera
        //  val myPlace = LatLng(40.73, -73.99)  // this is New York
       // mMap.addMarker(MarkerOptions().position(myPlace).title("My Favorite City"))
       // mMap.moveCamera(CameraUpdateFactory.newLatLng(myPlace))
       // mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myPlace, 12.0f))
        setUpMap()

        if (ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {   // 1
        mMap.isMyLocationEnabled = true

        // 2
        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            // Got last known location. In some rare situations this can be null.
            // 3
            if (location != null) {
                lastLocation = location
                val currentLatLng = LatLng(location.latitude, location.longitude)
                placeMarkerOnMap(currentLatLng)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                updateGasStations(location)
            }
        }
        }
    }
    private fun placeMarkerOnMap(location: LatLng) {
        // 1
        val markerOptions = MarkerOptions().position(location)
        // 2
        val titleStr = getAddress(location)  // add these two lines
        markerOptions.title(titleStr)
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(
                BitmapFactory.decodeResource(resources, R.mipmap.ic_user_location)))
        mMap.addMarker(markerOptions)
    }

    private fun updateGasStations(location: Location){
        async {
            val IP = getString(R.string.IP_CONFIG)
            val result = URL("http://"+getString(R.string.IP_CONFIG)+"/gasmartapi/stations.php?latitud="+location.latitude+"&longitud="+location.longitude+"&distancia="+getString(R.string.distance)).readText()
            uiThread {
                Log.d("Request", result)
                //  longToast("Request performed" + result)
                val stationsList : List<Station> = Gson().fromJson(result,Array<Station>::class.java).toList()
                Log.d("Request how many",stationsList.count().toString())
                stationsList.forEach{
                    Log.d("Request",it.name+": Stars "+it.rating)

                    runOnUiThread {
                         val myPlace = LatLng(it.location_y, it.location_x)  // this is New York

                       val markerOptions = MarkerOptions().position(myPlace).title(it.rating.toString() + " Stars: "+ it.name).icon(BitmapDescriptorFactory.fromBitmap(
                                BitmapFactory.decodeResource(resources, R.drawable.gasolinestation)))

                        // 2
                        var newMarker:Marker =  mMap.addMarker(markerOptions)
                        haspMap.put(newMarker.id, it);


                    }
                }
            }
        }
    }


    private fun setUpMap() {
        if (ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            finish();

            startActivity(getIntent());
            return
        }
    }

    private fun getAddress(latLng: LatLng): String {
        // 1
        Log.d("MapsActivity","Getting the address")
        val geocoder = Geocoder(this)
        val addresses: List<Address>?
        val address: Address?
        var addressText = ""

        try {
            // 2
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            // 3
            if (null != addresses && !addresses.isEmpty()) {
                address = addresses[0]
                for (i in 0 until address.maxAddressLineIndex) {
                    addressText += if (i == 0) address.getAddressLine(i) else "\n" + address.getAddressLine(i)
                }
            }
        } catch (e: IOException) {
            Log.e("MapsActivity", e.localizedMessage)
        }
        Log.d("MapsActivity",addressText)
        return addressText
    }

    private fun startLocationUpdates() {
        //1
        if (ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE)
            return
        }
        //2
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null /* Looper */)
    }

    private fun createLocationRequest() {
        // 1
        locationRequest = LocationRequest()
        // 2
        locationRequest.interval = 10000
        // 3
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)

        // 4
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        // 5
        task.addOnSuccessListener {
            locationUpdateState = true
            startLocationUpdates()
        }
        task.addOnFailureListener { e ->
            // 6
            if (e is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    e.startResolutionForResult(this@MapsActivity,
                            REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    // 1
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                locationUpdateState = true
                startLocationUpdates()
            }
        }

        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                try {
                    val place = PlacePicker.getPlace(this, data)
                    var addressText = place.name.toString()
                    addressText += "\n" + place.address.toString()

                    placeMarkerOnMap(place.latLng)
                }catch(ex: Exception){}
            }
        }
    }

    // 2
    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // 3
    public override fun onResume() {
        super.onResume()
        if (!locationUpdateState) {
            startLocationUpdates()
        }
    }

    private fun loadPlacePicker() {
        val builder = PlacePicker.IntentBuilder()

        try {
            startActivityForResult(builder.build(this@MapsActivity), PLACE_PICKER_REQUEST)
        } catch (e: GooglePlayServicesRepairableException) {
            e.printStackTrace()
        } catch (e: GooglePlayServicesNotAvailableException) {
            e.printStackTrace()
        }
    }

}

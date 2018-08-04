package com.example.android.gasmart

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_station_details.*
import kotlinx.android.synthetic.main.content_station_details.*
import org.jetbrains.anko.custom.async
import org.jetbrains.anko.uiThread
import java.net.URL

class StationDetails : AppCompatActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_station_details)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }


        val text =  intent.getStringExtra(MapsActivity.EXTRA_PLACE_ID)

        val rate = intent.getStringExtra(MapsActivity.EXTRA_RATING)
        val km = intent.getStringExtra(MapsActivity.EXTRA_DISTANCE)

        //Log.d("KM",km)

        tvStationName.text = intent.getStringExtra(MapsActivity.EXTRA_NAME) + " "+intent.getStringExtra(MapsActivity.EXTRA_DISTANCE) +" km"
        ratingBarCurrent.rating = rate.toFloat()

    }


    private fun updateGasStations(stationID: String){
        async {
            val result = URL("http://192.168.0.30:8080/gasmartapi/stations.php?stationID="+stationID)
            uiThread {
              //  Log.d("Request", result)
                //  longToast("Request performed" + result)
              /*  val stationsList : Station = Gson().fromJson(result,Array<Station>::class.java).toList()
                Log.d("Request how many",stationsList.count().toString())
                stationsList.forEach{
                    Log.d("Request",it.name)

                    runOnUiThread {
                        val myPlace = LatLng(it.location_y, it.location_x)  // this is New York

                        val markerOptions = MarkerOptions().position(myPlace).title(it.name).icon(BitmapDescriptorFactory.fromBitmap(
                                BitmapFactory.decodeResource(resources, R.drawable.gasolinestation)))

                        // 2
                        var newMarker: Marker =  mMap.addMarker(markerOptions)
                        haspMap.put(newMarker.id, it._place_id);


                    }

                }*/
            }
        }
    }

}

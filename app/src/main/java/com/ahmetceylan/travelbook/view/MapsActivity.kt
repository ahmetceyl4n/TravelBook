package com.ahmetceylan.travelbook.view

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ahmetceylan.travelbook.R

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.ahmetceylan.travelbook.databinding.ActivityMapsBinding
import com.ahmetceylan.travelbook.model.Place
import com.ahmetceylan.travelbook.roomdb.PlaceDao
import com.ahmetceylan.travelbook.roomdb.PlaceDatabase
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

class MapsActivity : AppCompatActivity(), OnMapReadyCallback,OnMapLongClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private lateinit var permissionResultLauncher: ActivityResultLauncher<String>
    private lateinit var sharedPreferences: SharedPreferences
    var trackBoolean : Boolean? = null
    private var selectedLatitude : Double? = null
    private var selectedLongitude : Double? = null
    private lateinit var db : PlaceDatabase
    private lateinit var placeDao : PlaceDao
    val compositeDisposable = CompositeDisposable()
    var placeFromMain : Place? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        supportActionBar?.hide()
        registerLauncher()

        sharedPreferences = this.getSharedPreferences("com.ahmetceylan.travelbook", MODE_PRIVATE)
        trackBoolean = false

        selectedLongitude=0.0
        selectedLatitude=0.0

        db = Room.databaseBuilder(applicationContext,PlaceDatabase::class.java,"Places").build()
        placeDao = db.placeDao()
        binding.saveButton.isEnabled = false


    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMapLongClickListener(this)

        val intent = intent
        val info = intent.getStringExtra("info")

        if (info == "new"){
            binding.saveButton.visibility = View.VISIBLE
            binding.deleteButton.visibility = View.GONE

            locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager
            locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    trackBoolean = sharedPreferences.getBoolean("trackBoolean",false)

                    if (trackBoolean == false){
                        val userLocation = LatLng(location.latitude,location.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,16f))
                        sharedPreferences.edit().putBoolean("trackBoolean",true).apply()
                    }

                }

            }
            if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){

                    Snackbar.make(binding.root,"Permissin needed for location",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission"){
                        //request permission
                        permissionResultLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }.show()

                }else{
                    //request permission
                    permissionResultLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }else{
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0f,locationListener)

                val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

                if (lastLocation != null){
                    val lastUserLocation = LatLng(lastLocation.latitude,lastLocation.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,16f))
                }
                mMap.isMyLocationEnabled = true

            }

        }else{
            mMap.clear()
            placeFromMain = intent.getSerializableExtra("selectedPlace") as? Place

            placeFromMain?.let {

                val latlng = LatLng(it.latitude,it.longitude)
                mMap.addMarker(MarkerOptions().position(latlng).title(it.name))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng,16f))
                binding.placeText.setText(it.name)
                binding.saveButton.visibility = View.GONE
                binding.deleteButton.visibility = View.VISIBLE


            }
        }
    }

    private fun registerLauncher(){
        permissionResultLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ result ->
        // result true yada false yani boolean bir değer dönüyor
        if (result){
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0f,locationListener)
                val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

                if (lastLocation != null){
                    val lastUserLocation = LatLng(lastLocation.latitude,lastLocation.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,16f))
                }
                mMap.isMyLocationEnabled = true

            }
        }else {
            Toast.makeText(this@MapsActivity,"Permission Needed!", Toast.LENGTH_LONG).show()

        }

        }


    }

    override fun onMapLongClick(p0: LatLng) {

        mMap.clear()
        mMap.addMarker(MarkerOptions().position(p0))
        selectedLatitude = p0.latitude
        selectedLongitude = p0.longitude
        binding.saveButton.isEnabled = true


    }

    fun save(view: View){

        if (selectedLatitude != null && selectedLongitude != null) {
            val place = Place(binding.placeText.text.toString(), selectedLatitude!!, selectedLongitude!!)
            Log.d("SaveDebug", "Place: $place")

            compositeDisposable.add(
                placeDao.insert(place)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnError { error ->
                        Log.e("SaveDebug", "Database Error: ${error.message}")
                    }
                    .subscribe({
                        Log.d("SaveDebug", "Place saved successfully")
                        handleResponse()
                    }, { error ->
                        Log.e("SaveDebug", "Save failed: ${error.message}")
                    })
            )
        } else {
            Log.w("SaveDebug", "Konum seçilmedi")
            Toast.makeText(this, "Lütfen bir konum seçin!", Toast.LENGTH_SHORT).show()
        }

    }

    private fun handleResponse(){
        val intent = Intent(this,MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    fun delete(view: View){

        placeFromMain?.let {
            compositeDisposable.add(
                placeDao.delete(it)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponse)
            )

        }

    }

    override fun onDestroy() {
        super.onDestroy()

        compositeDisposable.clear()
    }




}
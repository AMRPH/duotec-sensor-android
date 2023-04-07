package com.gelios.configurator.ui.net

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object RetrofitClient {
    private var retrofit: Retrofit? = null
    private var sensorApi: SensorApi? = null
    private const val baseUrl = "http://ble.webconfigurator.ru/"


    fun getApi(): SensorApi {
        if (retrofit == null) {
            val gson = GsonBuilder()
                .setLenient()
                .create()

            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        }
        if (sensorApi == null){
            sensorApi = retrofit!!.create(SensorApi::class.java)
        }
        return sensorApi!!
    }

    fun getLocation(context: Context): String {
        var result = ""
        val lm: LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            var location: Location? = null
            val providers = lm.getProviders(true)
            for (provider in providers){
                val l: Location = lm.getLastKnownLocation(provider) ?: continue
                if (location == null || l.accuracy < location.accuracy){
                    location = l
                }
            }
            if (location != null){
                result = "${location.latitude},${location.longitude}"
            }
        }
        return result
    }
}
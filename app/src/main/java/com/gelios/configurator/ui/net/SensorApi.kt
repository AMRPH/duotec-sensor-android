package com.gelios.configurator.ui.net

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface SensorApi {
    @GET("/ble.php")
    fun sensorBase(@Query("ver") ver : String,
                   @Query("mac") mac : String,
                   @Query("mav") mav : String,
                   @Query("mos") mos : String,
                   @Query("mpv") mpv : String,
                   @Query("lal") lal : String
    ): Call<ResponseBody>


    @GET("/ble.php")
    fun sensorVersion(@Query("ver") ver : String,
                      @Query("mac") mac : String,
                      @Query("fws") fws : String
    ): Call<ResponseBody>


    @GET("/ble.php")
    fun sensorData(@Query("ver") ver : String,
                   @Query("mac") mac : String,
                   @Query("vo1") vo1 : String,
                   @Query("vo2") vo2 : String
    ): Call<ResponseBody>


    @GET("/ble.php")
    fun sensorInfo(@Query("ver") ver : String,
                   @Query("mac") mac : String,
                   @Query("bat") bat : String,
                   @Query("tow") tow : String,
                   @Query("cor") cor : String,
                   @Query("coc") coc : String,
                   @Query("coa") coa : String,
                   @Query("cnt") cnt : String,
                   @Query("err") err : String,
                   @Query("vo3") vo3 : String
    ): Call<ResponseBody>


    @GET("/ble.php")
    fun sensorLLSSettings(@Query("ver") ver : String,
                       @Query("mac") mac : String,
                       @Query("cdt") cdt : String,
                       @Query("cof") cof : String,
                       @Query("coe") coe : String,
                       @Query("vof") vof : String,
                       @Query("cop") cop : String
    ): Call<ResponseBody>


    @GET("/ble.php")
    fun sensorSettings(@Query("ver") ver : String,
                       @Query("mac") mac : String,
                       @Query("cdt") cdt : String
    ): Call<ResponseBody>

    @GET("/ble.php")
    fun sensorTarrirovka(@Query("ver") ver : String,
                         @Query("mac") mac : String,
                         @Query("lov") lov : String,
                         @Query("lol") lol : String,
                         @Query("com")com : String
    ): Call<ResponseBody>


    @GET("/ble.php")
    fun sensorPassword(@Query("ver") ver : String,
                   @Query("mac") mac : String,
                   @Query("pos") pos : String,
    ): Call<ResponseBody>
}
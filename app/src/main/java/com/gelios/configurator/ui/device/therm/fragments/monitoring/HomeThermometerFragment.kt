package com.gelios.configurator.ui.device.therm.fragments.monitoring

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.gelios.configurator.BuildConfig
import com.gelios.configurator.MainPref
import com.gelios.configurator.R
import com.gelios.configurator.entity.Sensor
import com.gelios.configurator.entity.Status
import com.gelios.configurator.ui.choose.ChooseDeviceActivity
import com.gelios.configurator.ui.dialog.ConnectingDialog
import com.gelios.configurator.ui.net.RetrofitClient
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.fragment_home_thermometer.*
import kotlinx.android.synthetic.main.fragment_home_thermometer.progress
import kotlinx.android.synthetic.main.fragment_settings_thermometer.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import kotlin.math.abs
import kotlin.math.roundToInt


class HomeThermometerFragment : Fragment() {

    private lateinit var viewModel: HomeThermometerViewModel

    private val connectingDialog = ConnectingDialog(this)
    private var isDialog = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(this)[HomeThermometerViewModel::class.java]
        connectingDialog.isCancelable = false
        return inflater.inflate(R.layout.fragment_home_thermometer, container, false)
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)


        viewModel.uiProgressLiveData.observe(viewLifecycleOwner, Observer {
            if (it) {
                showProgressConnect()
                tv_status.text = getString(R.string.connecting)
                tv_status.setBackgroundResource(R.drawable.bg_button_blue)
                progress.visibility = View.VISIBLE
                buttonOff()
            } else {
                tv_status.text = getString(R.string.connected)
                tv_status.setBackgroundResource(R.drawable.bg_button_green)
                progress.visibility = View.GONE
                buttonOn()
                closeProgressConnect()
            }
        })

        viewModel.errorLiveData.observe(viewLifecycleOwner) {
            if (it.first) {
                val dialog = AlertDialog.Builder(context!!, R.style.AlertDialogCustom)
                Toast.makeText(context, it.second, Toast.LENGTH_LONG).show()
                dialog.setTitle(getString(R.string.error_ble))
                    .setPositiveButton(android.R.string.ok) { dialog, which ->
                        viewModel.clearCache()

                        val intent = Intent(context, ChooseDeviceActivity::class.java)
                        intent.putExtra("back", true)
                        intent.putExtra("term", true)
                        startActivity(intent)
                        activity?.finish() }
                    .show()
            }
        }

        btn_data.setOnClickListener {
            viewModel.readData()
        }

        viewModel.dataLiveData.observe(viewLifecycleOwner, Observer {
            tv_value_therm.text = String.format("%.2f °C", (it.getValueTherm()) )

            when (it.getValueTherm()) {
                in -100.0..-0.1 -> iv_therm.setImageResource(R.drawable.ic_therm_cold)
                0.0 -> iv_therm.setImageResource(R.drawable.ic_therm)
                in 0.1..100.0 -> iv_therm.setImageResource(R.drawable.ic_therm_hot)
            }

            when (it.getValueTherm()) {
                in -100.0 .. -42.5-> image_thermometer.setImageResource(R.drawable.sensor_therm_m45)
                in -42.5 .. -37.5-> image_thermometer.setImageResource(R.drawable.sensor_therm_m40)
                in -37.5 .. -32.5-> image_thermometer.setImageResource(R.drawable.sensor_therm_m35)
                in -32.5 .. -27.5-> image_thermometer.setImageResource(R.drawable.sensor_therm_m30)
                in -27.5 .. -22.5-> image_thermometer.setImageResource(R.drawable.sensor_therm_m25)
                in -22.5 .. -17.5-> image_thermometer.setImageResource(R.drawable.sensor_therm_m20)
                in -17.5 .. -12.5-> image_thermometer.setImageResource(R.drawable.sensor_therm_m15)
                in -12.5 .. -7.5-> image_thermometer.setImageResource(R.drawable.sensor_therm_m10)
                in -7.5 .. -2.5-> image_thermometer.setImageResource(R.drawable.sensor_therm_m05)
                in -2.5 .. 2.5-> image_thermometer.setImageResource(R.drawable.sensor_therm_0)
                in 2.5 .. 7.5-> image_thermometer.setImageResource(R.drawable.sensor_therm_05)
                in 7.5 .. 12.5-> image_thermometer.setImageResource(R.drawable.sensor_therm_10)
                in 12.5 .. 17.5-> image_thermometer.setImageResource(R.drawable.sensor_therm_15)
                in 17.5 .. 22.5-> image_thermometer.setImageResource(R.drawable.sensor_therm_20)
                in 22.5 .. 27.5-> image_thermometer.setImageResource(R.drawable.sensor_therm_25)
                in 27.5 .. 32.5-> image_thermometer.setImageResource(R.drawable.sensor_therm_30)
                in 32.5 .. 37.5-> image_thermometer.setImageResource(R.drawable.sensor_therm_35)
                in 37.5 .. 42.5-> image_thermometer.setImageResource(R.drawable.sensor_therm_40)
                in 42.5 .. 47.5-> image_thermometer.setImageResource(R.drawable.sensor_therm_45)
                in 47.5 .. 52.5-> image_thermometer.setImageResource(R.drawable.sensor_therm_50)
                in 52.5 .. 57.5-> image_thermometer.setImageResource(R.drawable.sensor_therm_55)
                in 57.5 .. 62.5-> image_thermometer.setImageResource(R.drawable.sensor_therm_60)
                in 62.5 .. 67.5-> image_thermometer.setImageResource(R.drawable.sensor_therm_65)
                in 67.5 .. 72.5-> image_thermometer.setImageResource(R.drawable.sensor_therm_70)
                in 72.5 .. 77.5-> image_thermometer.setImageResource(R.drawable.sensor_therm_75)
                in 77.5 .. 82.5-> image_thermometer.setImageResource(R.drawable.sensor_therm_80)
                in 82.5 .. 100.0-> image_thermometer.setImageResource(R.drawable.sensor_therm_85)
            }

            if (it.getValueWire()) {
                iv_relay.setImageResource(R.drawable.relay_on)
                tv_value_relay.text = getString(R.string.relay_status_closed)
            } else {
                iv_relay.setImageResource(R.drawable.relay_off)
                tv_value_relay.text = getString(R.string.relay_status_open)
            }

            if (it.getValueMagnet()) {
                iv_magnet.setImageResource(R.drawable.magnet_on)
                tv_value_magnet.text = getString(R.string.there_is)
            } else {
                iv_magnet.setImageResource(R.drawable.magnet_off)
                tv_value_magnet.text = getString(R.string.not)
            }

            if (it.getValueLight() != 0L) {
                iv_light.setImageResource(R.drawable.light_on)
                tv_value_light.text = "${it.getValueLight()} LX"
            } else {
                iv_light.setImageResource(R.drawable.light_off)
                tv_value_light.text = "${it.getValueLight()} LX"
            }

            sendSensorData(it.getValueTherm().toString(), it.getValueWire().toString())
        })

        viewModel.batteryLiveData.observe(viewLifecycleOwner, Observer { percent ->
            tv_value_battery.text = "${percent}%"

            drawBatteryPercentage(percent)
        })

        viewModel.infoLiveData.observe(viewLifecycleOwner, Observer {
            tv_value_mac.text = MainPref.deviceMac
            tv_value_battery_voltage.text = String.format("%.2fV", it.voltageDouble)

            if (!Sensor.flagSensorBattery) {
                val percent = getBatteryPercentage((it.voltageDouble * 100).roundToInt() / 100.0)
                tv_value_battery.text = "$percent%"

                drawBatteryPercentage(percent)
            }

            when (it.type) {
                0 -> tv_value_type.text = getString(R.string.error)
                1, 2 -> tv_value_type.text = getString(R.string.internal)
                3 -> tv_value_type.text = getString(R.string.external)
            }

            sendSensorInfo(
                String.format("%.2fV", it.voltageDouble),
                it.timestamp.toString(),
                it.reset_count.toString(),
                it.connection_attempts.toString(),
                it.password_attempts.toString(),
                it.raw_cnt.toString(),
                it.error.toString(),
                it.type.toString(),
            )
        })

        viewModel.rssiLiveData.observe(viewLifecycleOwner, Observer {
            tv_value_connection.text = "$it dBm"
            drawConnectionLevel(it)
        })

        viewModel.versionLiveData.observe(viewLifecycleOwner, Observer {
            tv_value_firmware.text = it

            sendSensorVersion(it)
        })

        viewModel.stateLiveData.observe(viewLifecycleOwner, Observer {
            when (it) {
                Status.OFFLINE -> {
                    tv_status.text = getString(R.string.disconnected)
                    tv_status.setBackgroundResource(R.drawable.bg_button_red)
                }
                Status.ONLINE -> {
                    activity?.findViewById<BottomNavigationView>(R.id.nav_view)?.menu?.getItem(1)?.isEnabled = true
                    activity?.findViewById<BottomNavigationView>(R.id.nav_view)?.menu?.getItem(2)?.isEnabled = true

                    tv_status.text = getString(R.string.connected)
                    tv_status.setBackgroundResource(R.drawable.bg_button_green)
                }
                Status.UNKNOWN -> {
                    tv_status.text = getString(R.string.unknown)
                    tv_status.setBackgroundResource(R.drawable.bg_button_grey)
                }
            }
        })

        button_back_to_scan.setOnClickListener {
            viewModel.clearCache()

            val intent = Intent(context, ChooseDeviceActivity::class.java)
            intent.putExtra("back", true)
            intent.putExtra("term", true)
            startActivity(intent)
            activity?.finish()
        }

        btn_copy.setOnClickListener {
            copyToClipBoard(tv_value_mac.text.toString().replace(":", ""))
            Toast.makeText(context, getString(R.string.copyed), Toast.LENGTH_SHORT).show()
        }

        /*
        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = false
            viewModel.stopTimer()
            viewModel.readSensor()
        }
         */

        sendSensorBase()
    }


    private fun getBatteryPercentage(value: Double): Int{
        val volParm = abs(((value - 3.35)/0.0025).roundToInt())
        return if (volParm > 100) 100 else volParm
    }

    private fun drawBatteryPercentage(i: Int) {
        when (i) {
            in 0..24 -> iv_battery.setImageResource(R.drawable.ic_battery_0)
            in 25..49 -> iv_battery.setImageResource(R.drawable.ic_battery_1)
            in 50..74 -> iv_battery.setImageResource(R.drawable.ic_battery_2)
            in 75..100 -> iv_battery.setImageResource(R.drawable.ic_battery_3)
        }
    }

    private fun drawConnectionLevel(i: Int) {
        when (i) {
            in -200..-75 -> iv_connection.setImageResource(R.drawable.ic_connection_red)
            in -74..-1 -> iv_connection.setImageResource(R.drawable.ic_connection_green)
        }
    }

    private fun copyToClipBoard(string: String) {
        val clipboard: ClipboardManager? = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        val clip: ClipData = ClipData.newPlainText("mac", string)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "$string ${resources.getString(R.string.copyed)}", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.initConnection()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopTimer()
    }

    private fun buttonOn() {
        btn_data.isEnabled = true
        btn_info.isEnabled = true
        btn_pass.isEnabled = true
    }

    private fun buttonOff() {
        btn_data.isEnabled = false
        btn_info.isEnabled = false
        btn_pass.isEnabled = false
    }

    private fun showProgressConnect() {
        if (!connectingDialog.isResumed && !connectingDialog.isVisible
            && !isDialog && !viewModel.isDeviceConnected
            && !viewModel.errorLiveData.value!!.first){
            connectingDialog.show(parentFragmentManager, "ConnectingDialog")
            isDialog = true
        }
    }

    fun onBack(){
        viewModel.clearCache()

        val intent = Intent(context, ChooseDeviceActivity::class.java)
        intent.putExtra("back", true)
        intent.putExtra("term", true)
        startActivity(intent)
        activity?.finish()
    }

    private fun closeProgressConnect() {
        android.os.Handler(Looper.getMainLooper()).postDelayed({
            if (connectingDialog.isResumed && connectingDialog.isVisible
                && isDialog && viewModel.isDeviceConnected){
                connectingDialog.dismiss()
                isDialog = false
            }
        }, 1000)
    }

    private fun sendSensorBase() {
        if (!Sensor.flagBase){
            val loc = if (context != null){
                RetrofitClient.getLocation(context!!)
            } else {
                ""
            }
            Log.d("INET GPS", loc)
            RetrofitClient.getApi()
                .sensorBase(
                    "1",
                    MainPref.deviceMac.replace(":", ""),
                    BuildConfig.VERSION_NAME,
                    Build.VERSION.SDK_INT.toString(),
                    Build.MODEL,
                    loc)
                .enqueue(object : retrofit2.Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        Log.d("INET sensorBase", response.body()!!.string())
                        Sensor.flagBase = true
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.d("INET sensorBase", t.message!!)
                    }
                })
        }
    }

    private fun sendSensorVersion(ver: String) {
        if (!Sensor.flagVersion){
            RetrofitClient.getApi()
                .sensorVersion(
                    "1",
                    MainPref.deviceMac.replace(":", ""),
                    ver)
                .enqueue(object : retrofit2.Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        Log.d("INET sensorVersion", response.body()!!.string())
                        Sensor.flagVersion = true
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.d("INET sensorVersion", t.message!!)
                    }
                })
        }
    }

    private fun sendSensorData(vo1: String, vo2: String) {
        if (!Sensor.flagData){
            RetrofitClient.getApi()
                .sensorData(
                    "1",
                    MainPref.deviceMac.replace(":", ""),
                    vo1,
                    vo2)
                .enqueue(object : retrofit2.Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        Log.d("INET sensorData", response.body()!!.string())
                        Sensor.flagData = true
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.d("INET sensorData", t.message!!)
                    }
                })

        }
    }

    private fun sendSensorInfo(bat : String, tow : String, cor : String,
                               coc : String, coa : String, cnt : String,
                               err : String, vo3 : String) {
        if (!Sensor.flagInfo){
            RetrofitClient.getApi()
                .sensorInfo(
                    "1",
                    MainPref.deviceMac.replace(":", ""),
                    bat, tow, cor, coc,
                    coa, cnt, err, vo3)
                .enqueue(object : retrofit2.Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        Log.d("INET sensorInfo", response.body()!!.string())
                        Sensor.flagInfo = true
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.d("INET sensorInfo", t.message!!)
                    }
                })

        }
    }
}
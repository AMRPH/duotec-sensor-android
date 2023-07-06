package com.gelios.configurator.ui.device.fuel.fragments.monitoring

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
import com.gelios.configurator.ui.choose.ChooseDeviceActivity
import com.gelios.configurator.entity.Status
import com.gelios.configurator.ui.datasensor.FuelSensorData
import com.gelios.configurator.ui.dialog.ConnectingDialog
import com.gelios.configurator.ui.net.RetrofitClient
import com.gelios.configurator.util.WaveHelper
import com.gelitenight.waveview.library.WaveView
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.fragment_home_fuel.*
import kotlinx.android.synthetic.main.fragment_home_fuel.btn_copy
import kotlinx.android.synthetic.main.fragment_home_fuel.btn_data
import kotlinx.android.synthetic.main.fragment_home_fuel.btn_info
import kotlinx.android.synthetic.main.fragment_home_fuel.btn_pass
import kotlinx.android.synthetic.main.fragment_home_fuel.button_back_to_scan
import kotlinx.android.synthetic.main.fragment_home_fuel.iv_battery
import kotlinx.android.synthetic.main.fragment_home_fuel.iv_connection
import kotlinx.android.synthetic.main.fragment_home_fuel.iv_therm
import kotlinx.android.synthetic.main.fragment_home_fuel.progress
import kotlinx.android.synthetic.main.fragment_home_fuel.tv_status
import kotlinx.android.synthetic.main.fragment_home_fuel.tv_value_battery
import kotlinx.android.synthetic.main.fragment_home_fuel.tv_value_battery_voltage
import kotlinx.android.synthetic.main.fragment_home_fuel.tv_value_connection
import kotlinx.android.synthetic.main.fragment_home_fuel.tv_value_firmware
import kotlinx.android.synthetic.main.fragment_home_fuel.tv_value_mac
import kotlinx.android.synthetic.main.fragment_home_fuel.tv_value_therm
import kotlinx.android.synthetic.main.fragment_settings_fuel.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import kotlin.math.abs
import kotlin.math.roundToInt


class HomeFuelFragment : Fragment() {

    private lateinit var viewModel: HomeFuelViewModel

    private val connectingDialog = ConnectingDialog(this)
    private var isDialog = false
    private var dataReady = false

    lateinit var mWaveHelper: WaveHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(this)[HomeFuelViewModel::class.java]
        connectingDialog.isCancelable = false
        val root = inflater.inflate(R.layout.fragment_home_fuel, container, false)
        return root
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        sendSensorBase()

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

        viewModel.uiFuelStability.observe(viewLifecycleOwner, Observer {
            updateIndicator(it)
        })

        viewModel.dataLiveData.observe(viewLifecycleOwner, Observer {
            tv_value_therm.text = String.format("%d °C", it.temperatura)

            when (it.temperatura) {
                in -100..-1 -> iv_therm.setImageResource(R.drawable.ic_therm_cold)
                0 -> iv_therm.setImageResource(R.drawable.ic_therm)
                in 1..100 -> iv_therm.setImageResource(R.drawable.ic_therm_hot)
            }

            if (it.fuelPercent == 32768.0) {
                tv_value_fuel_volume.text = getString(R.string.error_value)
                mWaveHelper.setLevel(0.03.toFloat())
            } else {
                var fuel = (it.fuelPercent * 100)
                val percent = String.format("%.1f", fuel)
                tv_value_fuel_volume.text = "$percent%"
                mWaveHelper.setLevel(it.fuelPercent.toFloat())
            }

            val dexVal = it.fuel
            tv_value_fuel_level.text = "$dexVal"

            sendSensorData(it.fuel.toString(), it.temperatura.toString())
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

            sendSensorInfo(
                String.format("%.2fV", it.voltageDouble),
                it.timestamp.toString(),
                it.reset_count.toString(),
                it.connection_attempts.toString(),
                it.password_attempts.toString(),
                it.raw_cnt.toString(),
                it.error.toString(),
                "",
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
                    activity?.findViewById<BottomNavigationView>(R.id.nav_view)?.menu?.getItem(3)?.isEnabled = true

                    tv_status.text = getString(R.string.connected)
                    tv_status.setBackgroundResource(R.drawable.bg_button_green)
                }
                Status.UNKNOWN -> {
                    tv_status.text = getString(R.string.unknown)
                    tv_status.setBackgroundResource(R.drawable.bg_button_grey)
                }
                Status.NEED_CALIBRATION ->  {
                    tv_status.text = getString(R.string.need_calibration)
                    tv_status.setBackgroundResource(R.drawable.bg_button_red)
                }
                Status.STABILISATION -> {
                    tv_status.text = getString(R.string.stabilization)
                    tv_status.setBackgroundResource(R.drawable.bg_button_blue)
                }
                Status.STABLY -> {
                    tv_status.text = getString(R.string.stably)
                    tv_status.setBackgroundResource(R.drawable.bg_button_green)
                }
            }
        })

        initWave()

        button_back_to_scan.setOnClickListener {
            viewModel.clearCache()

            val intent = Intent(context, ChooseDeviceActivity::class.java)
            intent.putExtra("back", true)
            startActivity(intent)
            activity?.finish()
        }

        btn_copy.setOnClickListener {
            copyToClipBoard(tv_value_mac.text.toString().replace(":", ""))
            Toast.makeText(context, getString(R.string.copyed), Toast.LENGTH_SHORT).show()
        }
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

    private fun updateIndicator(it: Boolean?) {
        when(it) {
            true -> {mWaveHelper.cancel() }
            false -> {mWaveHelper.start() }
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
        viewModel.getRSSI()
    }

    private fun initWave() {
        mWaveHelper = WaveHelper(waveView)
        waveView.setShapeType(WaveView.ShapeType.SQUARE)

        mWaveHelper.start()
    }

    fun buttonOn() {
        btn_data.isEnabled = true
        btn_info.isEnabled = true
        btn_pass.isEnabled = true
    }

    fun buttonOff() {
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
package com.gelios.configurator.ui.sensor.relay.fragments.monitoring

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
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_home_relay.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response


class HomeRelayFragment : Fragment() {

    private lateinit var viewModel: HomeRelayViewModel

    private val connectingDialog = ConnectingDialog(this)
    var mConfirmDialog: AlertDialog? = null
    private var isDialog = false
    private var dataReady = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(this)[HomeRelayViewModel::class.java]
        val root = inflater.inflate(R.layout.fragment_home_relay, container, false)
        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        connectingDialog.isCancelable = false

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
            if (it.isOutput()) {
                iv_relay_opened.setImageResource(R.drawable.circle_green)
                iv_relay_closed.setImageResource(R.drawable.circle_grey)
                image_relay.setImageResource(R.drawable.sensor_relay_on2)
            } else {
                iv_relay_opened.setImageResource(R.drawable.circle_grey)
                iv_relay_closed.setImageResource(R.drawable.circle_green)
                image_relay.setImageResource(R.drawable.sensor_relay_off2)
            }

            tv_mac.text = MainPref.deviceMac

            tv_value_battery.text = String.format("%.2f V", (it.voltage) )

            sendSensorData(it.isOutput().toString(), "")
        })

        viewModel.rssiLiveData.observe(viewLifecycleOwner, Observer {
            tv_value_connection.text = "$it dBm"
            drawConnectionLevel(it)
        })

        viewModel.versionLiveData.observe(viewLifecycleOwner, Observer {
            tv_value_firmware.text = it

            sendSensorVersion(it)
        })

        viewModel.infoLiveData.observe(viewLifecycleOwner) {
            sendSensorInfo(
                String.format("%.2fV", it.voltageDouble),
                it.timestamp.toString(),
                it.reset_count.toString(),
                it.connection_attempts.toString(),
                it.password_attempts.toString(),
                "",
                "",
                "",
            )
        }

        viewModel.commandSendOk.observe(viewLifecycleOwner, Observer {
            if (it) {
                Snackbar.make(requireView(), "Команда отправлена", Snackbar.LENGTH_LONG)
                    .show()
            }
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
            copyToClipBoard(tv_mac.text.toString().replace(":", ""))
            Toast.makeText(context, getString(R.string.copyed), Toast.LENGTH_SHORT).show()
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
        viewModel.getRSSI()
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
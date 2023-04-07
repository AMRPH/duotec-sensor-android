package com.gelios.configurator.ui.device.fuel.fragments.settings

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.gelios.configurator.MainPref
import com.gelios.configurator.R
import com.gelios.configurator.entity.Commands.CMD_FUEL_SET_EMPTY
import com.gelios.configurator.entity.Commands.CMD_FUEL_SET_FULL
import com.gelios.configurator.entity.Sensor
import com.gelios.configurator.ui.MessageType
import com.gelios.configurator.ui.PasswordManager
import com.gelios.configurator.ui.base.BaseFragment
import com.gelios.configurator.ui.choose.ChooseDeviceActivity
import com.gelios.configurator.ui.net.RetrofitClient
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_settings_fuel.*
import kotlinx.android.synthetic.main.layout_buttons_level.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response


class SettingsFuelFragment : BaseFragment(), AdapterView.OnItemSelectedListener,
    PasswordManager.Callback, NumberPicker.OnValueChangeListener {

    private lateinit var viewModel: SettingsFuelViewModel
    var mConfirmDialog: AlertDialog? = null
    lateinit var passwordManager: PasswordManager


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel =
            ViewModelProvider(this).get(SettingsFuelViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_settings_fuel, container, false)
        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        passwordManager = PasswordManager(requireContext(), this)
        viewModel.uiProgressLiveData.observe(viewLifecycleOwner, Observer {
            if (it) {
                progress.visibility = View.VISIBLE
            } else {
                progress.visibility = View.GONE
            }
        })

        viewModel.errorLiveData.observe(viewLifecycleOwner) {
            if (it) {
                val dialog = AlertDialog.Builder(context!!, R.style.AlertDialogCustom)
                dialog.setTitle(getString(R.string.error_ble))
                    .setPositiveButton(android.R.string.ok) { dialog, which ->
                        val intent = Intent(context, ChooseDeviceActivity::class.java)
                        intent.putExtra("back", true)
                        intent.putExtra("term", true)
                        startActivity(intent)
                        activity?.finish()
                    }
                    .show()
            }
        }

        viewModel.infoLiveSettings.observe(viewLifecycleOwner, Observer {
            initSpinner()

            it.filter_depth?.let { data -> btn_filter_depth.setText(data.toString()) }
            it.level_top?.let { data -> value_level_top.setText(data.toString()) }
            it.level_bottom?.let { data -> value_level_bottom.setText(data.toString()) }
            it.cnt_max?.let { data -> value_cnt_max.setText(data.toString()) }
            it.cnt_min?.let { data -> value_cnt_min.setText(data.toString()) }
            it.net_address?.let { data -> value_net_address.setText(data.toString()) }


            sendSensorSettings(
                it.escort.toString(),
                it.cnt_max.toString(),
                it.cnt_min.toString(),
                it.filter_depth.toString(),
                it.measurement_periods.toString())
        })

        viewModel.messageLiveData.observe(viewLifecycleOwner, Observer {
            it?.let {
                when (it) {
                    MessageType.ERROR -> showSnack(getString(R.string.error))
                    MessageType.PASSWORD_ACCEPTED -> showSnack(getString(R.string.password_accepted))
                    MessageType.PASSWORD_NOT_ACCEPTED -> showSnack(getString(R.string.password_not_accepted))
                    MessageType.COMMAND_APPLY -> showSnack(getString(R.string.send_ok))
                    MessageType.SAVED -> showSnack(getString(R.string.recorded))
                    MessageType.APPLY_FULL -> showSnack(getString(R.string.apply_full))
                    MessageType.APPLY_EMPTY -> showSnack(getString(R.string.apply_empty))
                }
            }
        })

        // "dbgpassw" - superpass
        // "00000000" - masterpass
        btn_password.setOnClickListener {
            passwordManager.enterPassword()
        }

        viewModel.uiActiveButtons.observe(viewLifecycleOwner, Observer {
            if (it) {
                btn_password.setImageResource(R.drawable.ic_lock_open)
                btn_filter_depth.isEnabled = true
                value_level_top.isEnabled = true
                value_level_bottom.isEnabled = true
                value_cnt_max.isEnabled = true
                value_cnt_min.isEnabled = true
                value_net_address.isEnabled = true
                spinner_channel.isEnabled = true

                btn_save_settings.isEnabled = true
                button_empty_tank.isEnabled = true
                button_full_tank.isEnabled = true
            } else {
                btn_password.setImageResource(R.drawable.ic_lock)
                btn_filter_depth.isEnabled = false
                value_level_top.isEnabled = false
                value_level_bottom.isEnabled = false
                value_cnt_max.isEnabled = false
                value_cnt_min.isEnabled = false
                spinner_channel.isEnabled = false
                value_net_address.isEnabled = false

                btn_save_settings.isEnabled = false
                button_empty_tank.isEnabled = false
                button_full_tank.isEnabled = false
            }
        })

        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = false
            viewModel.readSettings()
        }

        initButton()

        viewModel.commandSendOk.observe(viewLifecycleOwner, Observer {
            if (it) {
                Snackbar.make(requireView(), getString(R.string.send_ok), Snackbar.LENGTH_LONG)
                    .show()
            }
        })

    }

    private fun initButton() {
        btn_save_settings.setOnClickListener {
            if (!Sensor.sensorAuthorized) dialogNotAuth()
            else {
                if (dataValidation()) {
                    mConfirmDialog =
                        AlertDialog.Builder(context!!, R.style.AlertDialogCustom)
                            .setTitle(R.string.app_name)
                            .setMessage(getString(R.string.apply_command, btn_save_settings_text.text))
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                Sensor.flagSettings = false
                                viewModel.saveSettings(
                                    btn_filter_depth.text.toString().toInt(),
                                    value_level_top.text.toString().toInt(),
                                    value_level_bottom.text.toString().toInt(),
                                    value_cnt_max.text.toString().toInt(),
                                    value_cnt_min.text.toString().toInt(),
                                    Sensor.fuelCacheSettings!!.escort,
                                    value_net_address.text.toString().toInt()
                                )

                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()
                }}
            }

        button_empty_tank.setOnClickListener {
                if (!Sensor.sensorAuthorized) dialogNotAuth()
                else {
                    mConfirmDialog = AlertDialog.Builder(context!!, R.style.AlertDialogCustom)
                        .setTitle(R.string.app_name)
                        .setMessage(getString(R.string.apply_command, empty_tank_text.text))
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            viewModel.sendCommand(CMD_FUEL_SET_EMPTY)
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                }
            }

        button_full_tank.setOnClickListener {
                if (!Sensor.sensorAuthorized) dialogNotAuth()
                else {
                    mConfirmDialog =
                        AlertDialog.Builder(context!!, R.style.AlertDialogCustom)
                            .setTitle(R.string.app_name)
                            .setMessage(getString(R.string.apply_command, full_tank_text.text))
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                viewModel.sendCommand(CMD_FUEL_SET_FULL)
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()

                }
            }

        btn_filter_depth.setOnClickListener {
            if (!Sensor.sensorAuthorized) dialogNotAuth()
            else {
                showFilterDepthDialog()
            }
        }
    }


    override fun onValueChange(picker: NumberPicker?, oldVal: Int, newVal: Int) {
        Log.d("NUM", newVal.toString())
    }

    private fun showFilterDepthDialog(){
        val d = Dialog(requireContext())
        d.setTitle("NumberPicker")
        d.setContentView(R.layout.dialog_filter_depth)
        val b1: TextView = d.findViewById(R.id.btnCancel)
        val b2: TextView = d.findViewById(R.id.btnOk)
        val np = d.findViewById(R.id.filterPicker) as NumberPicker
        np.maxValue = 20
        np.minValue = 0
        np.value = btn_filter_depth.text.toString().toInt()
        np.wrapSelectorWheel = true
        np.setOnValueChangedListener(this)
        b1.setOnClickListener {
            btn_filter_depth.text = np.value.toString()
            d.dismiss()
        }
        b2.setOnClickListener {
            d.dismiss()
        }
        d.show()
    }

    private fun dialogNotAuth() {
        Snackbar.make(
            requireView(),
            getString(R.string.authorization_required),
            Snackbar.LENGTH_SHORT
        )
            .setAction(getString(R.string.auth), View.OnClickListener {
                passwordManager.enterPassword()
            })
            .show()
    }


    private fun checkIntValue(view: EditText, minimum: Int, maximum: Int): Boolean {
        val int = view.text.toString().toIntOrNull()
        return when {
            int == null -> {
                showErrorBalloon(view, "$minimum - $maximum")
                false
            }
            int < minimum -> {
                showErrorBalloon(view, "< $minimum")
                false
            }

            int > maximum -> {
                showErrorBalloon(view, ">  $maximum")
                false
            }
            else -> {
                true
            }
        }
    }

    override fun onPause() {
        hideAllBalloon()
        super.onPause()
    }

    private fun dataValidation(): Boolean {
        hideAllBalloon()
        var result = true

        if (!checkIntValue(value_level_top, 1, 4095)) result = false
        if (!checkIntValue(value_level_bottom, 0, 4094)) result = false
        if (!checkIntValue(value_cnt_max, 100, 1000000)) result = false
        if (!checkIntValue(value_cnt_min, 100, 1000000)) result = false
        if (!checkIntValue(value_net_address, 0, 255)) result = false

        val tLevCnt = value_level_top.text.toString().toIntOrNull() ?: 0
        val bLevCnt = value_level_bottom.text.toString().toIntOrNull() ?: 0
        if (bLevCnt > tLevCnt) {
            showErrorBalloon(value_cnt_min, "max < min")
            result = false
        }

        val tLev = value_cnt_max.text.toString().toIntOrNull() ?: 0
        val bLev = value_cnt_min.text.toString().toIntOrNull() ?: 0
        if (bLev > tLev) {
            showErrorBalloon(value_cnt_min, "max < min")
            result = false
        }
        if (result) hideAllBalloon()
        return result
    }

    private fun initSpinner() {
        val list_chanel = arrayOf(
            resources.getString(R.string.close),
            resources.getString(R.string.open)
        )

        val chanelAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, list_chanel)
        chanelAdapter.setDropDownViewResource(R.layout.spinner_item)
        spinner_channel!!.adapter = chanelAdapter
        spinner_channel.onItemSelectedListener = this
        spinner_channel.setSelection(Sensor.fuelCacheSettings!!.escort)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        viewModel.replaceEscort(position)
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkAuth()
        if (!Sensor.sensorAuthorized) dialogNotAuth()
    }

    override fun enterPassword(password: String) {
        viewModel.enterPassword(password)
    }

    override fun changePassword(password: String) {

    }

    override fun returnedError(textError: String) {
        Snackbar.make(requireView(), textError, Snackbar.LENGTH_SHORT).show()
    }

    private fun showSnack(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
    }

    private fun sendSensorSettings(cdt: String, cof: String, coe: String, vof: String, cop: String) {
        if (!Sensor.flagSettings){
            RetrofitClient.getApi()
                .sensorLLSSettings(
                    "1",
                    MainPref.deviceMac.replace(":", ""),
                    cdt,
                    cof,
                    coe,
                    vof,
                    cop)
                .enqueue(object : retrofit2.Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        Log.d("INET sensorSettings", response.body()!!.string())
                        Sensor.flagSettings = true
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.d("INET sensorSettings", t.message!!)
                    }
                })
        }
    }
}
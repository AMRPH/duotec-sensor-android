package com.gelios.configurator.ui.sensor.fuel.fragments.settings

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
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
import com.gelios.configurator.util.BinHelper
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_settings_fuel.*
import kotlinx.android.synthetic.main.fragment_settings_fuel.btn_beacon
import kotlinx.android.synthetic.main.fragment_settings_fuel.btn_interval
import kotlinx.android.synthetic.main.fragment_settings_fuel.btn_password
import kotlinx.android.synthetic.main.fragment_settings_fuel.btn_power
import kotlinx.android.synthetic.main.fragment_settings_fuel.btn_protocol
import kotlinx.android.synthetic.main.fragment_settings_fuel.btn_save_settings
import kotlinx.android.synthetic.main.fragment_settings_fuel.btn_save_settings_text
import kotlinx.android.synthetic.main.fragment_settings_fuel.et_major
import kotlinx.android.synthetic.main.fragment_settings_fuel.et_minor
import kotlinx.android.synthetic.main.fragment_settings_fuel.et_uuid
import kotlinx.android.synthetic.main.fragment_settings_fuel.fl_beacon
import kotlinx.android.synthetic.main.fragment_settings_fuel.fl_interval
import kotlinx.android.synthetic.main.fragment_settings_fuel.fl_major
import kotlinx.android.synthetic.main.fragment_settings_fuel.fl_minor
import kotlinx.android.synthetic.main.fragment_settings_fuel.fl_power
import kotlinx.android.synthetic.main.fragment_settings_fuel.fl_uuid
import kotlinx.android.synthetic.main.fragment_settings_fuel.progress
import kotlinx.android.synthetic.main.fragment_settings_fuel.swipeRefreshLayout
import kotlinx.android.synthetic.main.layout_buttons_level.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response


class SettingsFuelFragment : BaseFragment(),
    PasswordManager.Callback{

    private lateinit var viewModel: SettingsFuelViewModel
    var mConfirmDialog: AlertDialog? = null
    lateinit var passwordManager: PasswordManager

    val valuesProtocol = if (Sensor.version!! >= 5){
        arrayOf("закрытый", "открытый", "мини")
    } else {
        arrayOf("закрытый", "открытый")
    }
    val valuesPower = arrayOf("минимальная", "средняя", "максимальная")
    val valuesBeacon = arrayOf("отключен", "режим Beacon")

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

        if (Sensor.version!! >= 5){
            fl_interval.visibility = View.VISIBLE
            fl_power.visibility = View.VISIBLE
            fl_beacon.visibility = View.VISIBLE
            fl_uuid.visibility = View.VISIBLE
            fl_major.visibility = View.VISIBLE
            fl_minor.visibility = View.VISIBLE
            divider13.visibility = View.VISIBLE
        }

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

        viewModel.settingsLiveData.observe(viewLifecycleOwner, Observer {
            btn_protocol.text = valuesProtocol[Sensor.fuelCacheSettings!!.flag!!]

            it.filter_depth?.let { data -> btn_filter_depth.setText(data.toString()) }
            it.cnt_max?.let { data -> value_cnt_max.setText(data.toString()) }
            it.cnt_min?.let { data -> value_cnt_min.setText(data.toString()) }


            sendSensorSettings(
                it.flag.toString(),
                it.cnt_max.toString(),
                it.cnt_min.toString(),
                it.filter_depth.toString(),
                it.measurement_periods.toString())
        })

        if (Sensor.version!! >= 5){
            viewModel.settings2LiveData.observe(viewLifecycleOwner, Observer {
                btn_interval.text = Sensor.thermCacheSettings2!!.adv_interval!!.toString()
                btn_power.text = valuesPower[Sensor.thermCacheSettings2!!.adv_power_mode!!]
                btn_beacon.text = valuesBeacon[Sensor.thermCacheSettings2!!.adv_beacon!!]

                et_uuid.setText(BinHelper.toHex(it.uuid!!))
                et_major.setText(BinHelper.toInt16(it.major!!).toString())
                et_minor.setText(BinHelper.toInt16(it.minor!!).toString())
            })


            et_uuid.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(s: Editable?) {
                    if ((s?.length ?: 0) == 32 && BinHelper.checkHEXCorrect(s.toString())){
                        et_uuid.setTextColor(ContextCompat.getColor(context!!, R.color.colorPrimaryBackground))
                        viewModel.flagUUIDCorrect = true
                    } else {
                        et_uuid.setTextColor(ContextCompat.getColor(context!!, R.color.colorStatusRedText))
                        viewModel.flagUUIDCorrect = false
                    }
                }

            })

            et_major.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(s: Editable?) {
                    if (s!!.isNotEmpty()){
                        val i = s.toString().toInt()
                        if (i < 1){
                            et_major.setText("1")
                            et_major.setSelection(1)
                        } else if (i > 65535) {
                            et_major.setText("65535")
                            et_major.setSelection(5)
                        }
                    }
                }

            })

            et_minor.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(s: Editable?) {
                    if (s!!.isNotEmpty()){
                        val i = s.toString().toInt()
                        if (i < 1){
                            et_minor.setText("1")
                            et_minor.setSelection(1)
                        } else if (i > 65535) {
                            et_minor.setText("65535")
                            et_minor.setSelection(5)
                        }
                    }
                }

            })
        }

        initButton()

        // "dbgpassw" - superpass
        // "00000000" - masterpass
        btn_password.setOnClickListener {
            passwordManager.enterPassword()
        }

        viewModel.uiActiveButtons.observe(viewLifecycleOwner, Observer {
            if (it) {
                btn_password.setImageResource(R.drawable.ic_lock_open)
                value_cnt_max.isEnabled = true
                value_cnt_min.isEnabled = true

                et_uuid.isEnabled = true
                et_major.isEnabled = true
                et_minor.isEnabled = true
            } else {
                btn_password.setImageResource(R.drawable.ic_lock)
                value_cnt_max.isEnabled = false
                value_cnt_min.isEnabled = false

                et_uuid.isEnabled = false
                et_major.isEnabled = false
                et_minor.isEnabled = false
            }
        })

        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = false
            viewModel.readSettings()
            if (Sensor.version!! >= 5) {
                viewModel.readSettings2()
            }
        }

        viewModel.messageLiveData.observe(viewLifecycleOwner, Observer {
            it?.let {
                when (it) {
                    MessageType.ERROR -> showSnack(getString(R.string.error))
                    MessageType.SAVED -> showSnack(getString(R.string.recorded))
                    MessageType.COMMAND_APPLY -> showSnack(getString(R.string.send_ok))
                    MessageType.PASSWORD_ACCEPTED -> showSnack(getString(R.string.password_accepted))
                    MessageType.PASSWORD_NOT_ACCEPTED -> showSnack(getString(R.string.password_not_accepted))
                    MessageType.APPLY_FULL -> showSnack(getString(R.string.apply_full))
                    MessageType.APPLY_EMPTY -> showSnack(getString(R.string.apply_empty))
                }
            }
        })

        viewModel.commandSendOk.observe(viewLifecycleOwner, Observer {
            if (it) {
                Snackbar.make(requireView(), getString(R.string.send_ok), Snackbar.LENGTH_LONG)
                    .show()
            }
        })

    }

    private fun initButton() {
        btn_save_settings.setOnClickListener {
            if (!Sensor.authorized) dialogNotAuth()
            else {
                if (dataValidation() && viewModel.flagUUIDCorrect && et_major.text.isNotEmpty() && et_minor.text.isNotEmpty()) {
                    mConfirmDialog =
                        AlertDialog.Builder(context!!, R.style.AlertDialogCustom)
                            .setTitle(R.string.app_name)
                            .setMessage(getString(R.string.apply_command, btn_save_settings_text.text))
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                Sensor.flagSettings = false
                                viewModel.saveSettings(
                                    btn_filter_depth.text.toString().toInt(),
                                    value_cnt_max.text.toString().toInt(),
                                    value_cnt_min.text.toString().toInt(),
                                    valuesProtocol.indexOf(btn_protocol.text.toString())
                                )
                                viewModel.saveSettings2(
                                    btn_interval.text.toString().toInt(),
                                    valuesPower.indexOf(btn_power.text.toString()),
                                    valuesBeacon.indexOf(btn_beacon.text.toString()),
                                    et_uuid.text.toString(),
                                    et_major.text.toString().toInt(),
                                    et_minor.text.toString().toInt())

                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()
                }}
            }

        btn_protocol.setOnClickListener {
            if (!Sensor.authorized) dialogNotAuth()
            else {
                dialogProtocol()
            }
        }

        button_empty_tank.setOnClickListener {
                if (!Sensor.authorized) dialogNotAuth()
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
                if (!Sensor.authorized) dialogNotAuth()
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
            if (!Sensor.authorized) dialogNotAuth()
            else {
                dialogFilterDepth()
            }
        }

        if (Sensor.version!! >= 5){
            btn_interval.setOnClickListener {
                if (!Sensor.authorized) dialogNotAuth()
                else {
                    dialogInterval()
                }
            }

            btn_power.setOnClickListener {
                if (!Sensor.authorized) dialogNotAuth()
                else {
                    dialogPower()
                }
            }

            btn_beacon.setOnClickListener {
                if (!Sensor.authorized) dialogNotAuth()
                else {
                    dialogBeacon()
                }
            }
        }
    }

    private fun dialogProtocol(){
        val d = Dialog(requireContext())
        d.setContentView(R.layout.dialog_number_picker)
        val b1: TextView = d.findViewById(R.id.btnCancel)
        val b2: TextView = d.findViewById(R.id.btnOk)
        val np = d.findViewById(R.id.filterPicker) as NumberPicker
        np.minValue = 0
        np.maxValue = if (Sensor.version!! >= 5) 2 else 1
        np.displayedValues = valuesProtocol
        np.value = valuesProtocol.indexOf(btn_protocol.text.toString())
        np.wrapSelectorWheel = true
        b1.setOnClickListener {
            btn_protocol.text = valuesProtocol[np.value]
            d.dismiss()
        }
        b2.setOnClickListener {
            d.dismiss()
        }
        d.show()
    }

    private fun dialogInterval(){
        val d = Dialog(requireContext())
        d.setTitle("NumberPicker")
        d.setContentView(R.layout.dialog_number_picker)
        val b1: TextView = d.findViewById(R.id.btnCancel)
        val b2: TextView = d.findViewById(R.id.btnOk)
        val np = d.findViewById(R.id.filterPicker) as NumberPicker
        np.minValue = 1
        np.maxValue = 10
        np.value = btn_interval.text.toString().toInt()
        np.wrapSelectorWheel = true
        b1.setOnClickListener {
            btn_interval.text = np.value.toString()
            d.dismiss()
        }
        b2.setOnClickListener {
            d.dismiss()
        }
        d.show()
    }

    private fun dialogPower(){
        val d = Dialog(requireContext())
        d.setTitle("NumberPicker")
        d.setContentView(R.layout.dialog_number_picker)
        val b1: TextView = d.findViewById(R.id.btnCancel)
        val b2: TextView = d.findViewById(R.id.btnOk)
        val np = d.findViewById(R.id.filterPicker) as NumberPicker
        np.minValue = 0
        np.maxValue = 2
        np.displayedValues = valuesPower
        np.value = valuesPower.indexOf(btn_power.text.toString())
        np.wrapSelectorWheel = true
        b1.setOnClickListener {
            btn_power.text = valuesPower[np.value]
            d.dismiss()
        }
        b2.setOnClickListener {
            d.dismiss()
        }
        d.show()
    }

    private fun dialogBeacon(){
        val d = Dialog(requireContext())
        d.setContentView(R.layout.dialog_number_picker)
        val b1: TextView = d.findViewById(R.id.btnCancel)
        val b2: TextView = d.findViewById(R.id.btnOk)
        val np = d.findViewById(R.id.filterPicker) as NumberPicker
        np.minValue = 0
        np.maxValue = 1
        np.displayedValues = valuesBeacon
        np.value = valuesBeacon.indexOf(btn_beacon.text.toString())
        np.wrapSelectorWheel = true
        b1.setOnClickListener {
            btn_beacon.text = valuesBeacon[np.value]
            d.dismiss()
        }
        b2.setOnClickListener {
            d.dismiss()
        }
        d.show()
    }

    private fun dialogFilterDepth(){
        val d = Dialog(requireContext())
        d.setTitle("NumberPicker")
        d.setContentView(R.layout.dialog_number_picker)
        val b1: TextView = d.findViewById(R.id.btnCancel)
        val b2: TextView = d.findViewById(R.id.btnOk)
        val np = d.findViewById(R.id.filterPicker) as NumberPicker
        np.maxValue = 20
        np.minValue = 0
        np.value = btn_filter_depth.text.toString().toInt()
        np.wrapSelectorWheel = true
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


    private fun checkIntValue(view: TextView, minimum: Int, maximum: Int): Boolean {
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

        if (!checkIntValue(value_cnt_max, 100, 1000000)) result = false
        if (!checkIntValue(value_cnt_min, 100, 1000000)) result = false


        val tLev = value_cnt_max.text.toString().toIntOrNull() ?: 0
        val bLev = value_cnt_min.text.toString().toIntOrNull() ?: 0
        if (bLev > tLev) {
            showErrorBalloon(value_cnt_min, "max < min")
            result = false
        }
        if (result) hideAllBalloon()
        return result
    }
    override fun onResume() {
        super.onResume()
        viewModel.checkAuth()
        if (!Sensor.authorized) dialogNotAuth()
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
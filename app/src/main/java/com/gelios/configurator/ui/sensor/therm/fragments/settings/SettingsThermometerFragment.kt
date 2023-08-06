package com.gelios.configurator.ui.sensor.therm.fragments.settings

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.gelios.configurator.MainPref
import com.gelios.configurator.R
import com.gelios.configurator.entity.Sensor
import com.gelios.configurator.ui.MessageType
import com.gelios.configurator.ui.PasswordManager
import com.gelios.configurator.ui.base.BaseFragment
import com.gelios.configurator.ui.choose.ChooseDeviceActivity
import com.gelios.configurator.ui.net.RetrofitClient
import com.gelios.configurator.util.BinHelper
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_settings_thermometer.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response


class SettingsThermometerFragment : BaseFragment(),
    PasswordManager.Callback {

    private lateinit var viewModel: SettingsThermometerViewModel
    var mConfirmDialog: AlertDialog? = null
    lateinit var passwordManager: PasswordManager

    private lateinit var valuesProtocol: Array<String>
    private lateinit var valuesPower: Array<String>
    private lateinit var  valuesBeacon: Array<String>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel =
            ViewModelProvider(this).get(SettingsThermometerViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_settings_thermometer, container, false)
        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        valuesProtocol = if (Sensor.version!! >= 5){
            arrayOf(getString(R.string.closed), getString(R.string.opened), getString(R.string.mini))
        } else {
            arrayOf(getString(R.string.closed), getString(R.string.opened))
        }
        valuesPower = arrayOf(getString(R.string.minimal), getString(R.string.average), getString(R.string.maximal))
        valuesBeacon = arrayOf(getString(R.string.disconnected), getString(R.string.mode_beacon))

        if (Sensor.version!! >= 5){
            fl_interval.visibility = View.VISIBLE
            fl_power.visibility = View.VISIBLE
            divider2.visibility = View.VISIBLE
            fl_beacon.visibility = View.VISIBLE
            fl_uuid.visibility = View.VISIBLE
            fl_major.visibility = View.VISIBLE
            fl_minor.visibility = View.VISIBLE
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
            btn_protocol.text = valuesProtocol[Sensor.thermCacheSettings!!.flag!!]

            sendSensorSettings(it.flag.toString())
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


            et_uuid.addTextChangedListener(object : TextWatcher{
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

            et_major.addTextChangedListener(object : TextWatcher{
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

            et_minor.addTextChangedListener(object : TextWatcher{
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

                et_uuid.isEnabled = true
                et_major.isEnabled = true
                et_minor.isEnabled = true
            } else {
                btn_password.setImageResource(R.drawable.ic_lock)

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
                    MessageType.PASSWORD_ACCEPTED -> { showSnack(getString(R.string.password_accepted)) }
                    MessageType.PASSWORD_NOT_ACCEPTED -> showSnack(getString(R.string.password_not_accepted))
                }
            }
        })

        viewModel.commandSendOk.observe(viewLifecycleOwner, Observer {
            if (it) {
                Snackbar.make(requireView(), getString(R.string.send_ok), Snackbar.LENGTH_LONG).show()
            }
        })
    }

    private fun initButton() {
        btn_save_settings.setOnClickListener {
            if (!Sensor.authorized) dialogNotAuth()
            else {
                if (checkCorrect()){
                    mConfirmDialog =
                        AlertDialog.Builder(context!!, R.style.AlertDialogCustom)
                            .setTitle(R.string.app_name)
                            .setMessage(getString(R.string.apply_command, btn_save_settings_text.text))
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                Sensor.flagSettings = false
                                viewModel.saveSettings(
                                    valuesProtocol.indexOf(btn_protocol.text.toString()))

                                if (Sensor.version!! >= 5){
                                    viewModel.saveSettings2(
                                        btn_interval.text.toString().toInt(),
                                        valuesPower.indexOf(btn_power.text.toString()),
                                        valuesBeacon.indexOf(btn_beacon.text.toString()),
                                        et_uuid.text.toString(),
                                        et_major.text.toString().toInt(),
                                        et_minor.text.toString().toInt())
                                }
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()
                }
            }
        }

        btn_protocol.setOnClickListener {
            if (!Sensor.authorized) dialogNotAuth()
            else {
                dialogProtocol()
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

    private fun checkCorrect(): Boolean {
        return if (Sensor.version!! >= 5){
            viewModel.flagUUIDCorrect && et_major.text.isNotEmpty() && et_minor.text.isNotEmpty()
        } else {
            true
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

    private fun dialogNotAuth() {
        Snackbar.make(requireView(), getString(R.string.authorization_required), Snackbar.LENGTH_SHORT)
            .setAction(getString(R.string.auth), View.OnClickListener {
                passwordManager.enterPassword()
            })
            .show()
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

    private fun sendSensorSettings(cdt: String) {
        if (!Sensor.flagSettings){
            RetrofitClient.getApi()
                .sensorSettings(
                    "1",
                    MainPref.deviceMac.replace(":", ""),
                    cdt)
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
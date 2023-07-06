package com.gelios.configurator.ui.device.relay.fragments.settings

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.gelios.configurator.MainPref
import com.gelios.configurator.R
import com.gelios.configurator.entity.Commands
import com.gelios.configurator.entity.Sensor
import com.gelios.configurator.ui.MessageType
import com.gelios.configurator.ui.PasswordManager
import com.gelios.configurator.ui.choose.ChooseDeviceActivity
import com.gelios.configurator.ui.net.RetrofitClient
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_settings_relay.btn_password
import kotlinx.android.synthetic.main.fragment_settings_relay.btn_save_settings
import kotlinx.android.synthetic.main.fragment_settings_relay.btn_save_settings_text
import kotlinx.android.synthetic.main.fragment_settings_relay.progress
import kotlinx.android.synthetic.main.fragment_settings_relay.spinner_protocol
import kotlinx.android.synthetic.main.fragment_settings_relay.swipeRefreshLayout
import kotlinx.android.synthetic.main.fragment_settings_thermometer.*
import kotlinx.android.synthetic.main.layout_buttons_switch_relay.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response


class SettingsRelayFragment : Fragment(), AdapterView.OnItemSelectedListener,
    PasswordManager.Callback {

    private lateinit var viewModel: SettingsRelayViewModel
    var mConfirmDialog: AlertDialog? = null
    lateinit var passwordManager: PasswordManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel =
            ViewModelProvider(this).get(SettingsRelayViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_settings_relay, container, false)
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


        // "dbgpassw" - superpass
        // "00000000" - masterpass
        btn_password.setOnClickListener {
            passwordManager.enterPassword()
        }

        viewModel.uiActiveButtons.observe(viewLifecycleOwner, Observer {
            if (it) {
                btn_password.setImageResource(R.drawable.ic_lock_open)
                spinner_protocol.isEnabled = true

                btn_save_settings.isEnabled = true
                btn_switch_relay.isEnabled = true
            } else {
                btn_password.setImageResource(R.drawable.ic_lock)
                spinner_protocol.isEnabled = false

                btn_save_settings.isEnabled = false
                btn_switch_relay.isEnabled = false
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

        viewModel.messageLiveData.observe(viewLifecycleOwner, Observer {
            it?.let {
                when (it) {
                    MessageType.ERROR -> showSnack(getString(R.string.error))
                    MessageType.OPEN_RELAY -> showSnack(getString(R.string.relay_is_open))
                    MessageType.CLOSE_RELAY -> showSnack(getString(R.string.relay_is_close))
                }
            }
        })

        viewModel.infoLiveSettings.observe(viewLifecycleOwner) {
            initSpinner()
            sendSensorSettings(it.escort.toString())
        }
    }


    private fun initButton() {
        btn_save_settings.setOnClickListener {
            if (!Sensor.authorized) dialogNotAuth()
            else {
                mConfirmDialog =
                    AlertDialog.Builder(context!!, R.style.AlertDialogCustom)
                        .setTitle(R.string.app_name)
                        .setMessage(getString(R.string.apply_command, btn_save_settings_text.text))
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            Sensor.flagSettings = false
                            viewModel.saveSettings()
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
            }
        }

        btn_switch_relay.setOnClickListener {
            if (!Sensor.authorized) dialogNotAuth()
            else {
                if (Sensor.relayCacheData!!.isOutput()){
                    mConfirmDialog =
                        AlertDialog.Builder(context!!, R.style.AlertDialogCustom)
                            .setTitle(R.string.app_name)
                            .setMessage(getString(R.string.apply_command, getString(R.string.relay_close)))
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                viewModel.sendCommand(Commands.CMD_RELAY_CLOSE)
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()
                } else {
                    mConfirmDialog =
                        AlertDialog.Builder(context!!, R.style.AlertDialogCustom)
                            .setTitle(R.string.app_name)
                            .setMessage(getString(R.string.apply_command, getString(R.string.relay_open)))
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                viewModel.sendCommand(Commands.CMD_RELAY_OPEN)
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()
                }
            }
        }
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

    override fun onResume() {
        super.onResume()
        viewModel.checkAuth()
        if (!Sensor.authorized) dialogNotAuth()
    }

    private fun initSpinner() {
        val list_chanel = arrayOf(
            resources.getString(R.string.close),
            resources.getString(R.string.open)
        )

        val chanelAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, list_chanel)
        chanelAdapter.setDropDownViewResource(R.layout.spinner_item)
        spinner_protocol!!.adapter = chanelAdapter
        spinner_protocol.onItemSelectedListener = this
        spinner_protocol.setSelection(Sensor.relayCacheSettings!!.escort)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        viewModel.replaceSettings(position)
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
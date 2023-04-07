package com.gelios.configurator.ui.device.fuel.fragments.service_menu

import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.gelios.configurator.MainPref
import com.gelios.configurator.R
import com.gelios.configurator.entity.Commands.CMD_FUEL_POWER_DOWN
import com.gelios.configurator.entity.Commands.CMD_FUEL_RESET
import com.gelios.configurator.entity.Commands.CMD_FUEL_RESET_TO_PERSISTENT
import com.gelios.configurator.entity.Sensor
import com.gelios.configurator.ui.App
import com.gelios.configurator.ui.MessageType
import com.gelios.configurator.ui.PasswordManager
import com.gelios.configurator.ui.choose.ChooseDeviceActivity
import com.gelios.configurator.ui.net.RetrofitClient
import com.google.android.material.snackbar.Snackbar
import com.polidea.rxandroidble2.RxBleDevice
import com.ti.ti_oad.TIOADEoadClient
import com.ti.ti_oad.TIOADEoadClientProgressCallback
import com.ti.ti_oad.TIOADEoadDefinitions
import kotlinx.android.synthetic.main.fragment_commands_fuel.*
import kotlinx.android.synthetic.main.layout_device_parameters.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import java.util.*

class CommandsFuelFragment : Fragment(), PasswordManager.Callback {

    private lateinit var viewModel: CommandsFuelViewModel
    var client: TIOADEoadClient? = null
    private val READ_REQUEST_CODE = 42
    var fileURL: Uri? = null
    var mConfirmDialog: AlertDialog? = null
    lateinit var passwordManager: PasswordManager
    var dialogFirmWare : AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(this).get(CommandsFuelViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_commands_fuel, container, false)

        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val firmwaremode = arguments?.getBoolean("firmware", false)
        if (firmwaremode != null && firmwaremode == true) {
            update()
        } else {
            viewModel.initVM()
        }

        passwordManager = PasswordManager(requireContext(), this)

        viewModel.infoLiveData.observe(viewLifecycleOwner, Observer {
            value_reset_count.text = it.reset_count.toString()
            value_connection_attempts.text = it.connection_attempts.toString()
            value_password_attempts.text = it.password_attempts.toString()
            value_timestamp.text = "${it.timeString()}"
            value_raw_cnt.text = it.raw_cnt.toString()
            if (it.error == 0) {
                value_error.text = getString(R.string.not)
            } else {
                value_error.text = it.error.toString()
            }

            value_mac.text = MainPref.deviceMac
        })

        viewModel.settingsLiveData.observe(viewLifecycleOwner, Observer {
            value_counter_period.text = it.measurement_periods.toString()
        })

        viewModel.commandSendOk.observe(viewLifecycleOwner, Observer {
            if (it) {
                Snackbar.make(requireView(), "Команда отправлена", Snackbar.LENGTH_LONG)
                    .show()
            }
        })

        viewModel.versionLiveData.observe(viewLifecycleOwner, Observer {
            value_version.text = it
        })

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
                        viewModel.clearCache()

                        val intent = Intent(context, ChooseDeviceActivity::class.java)
                        intent.putExtra("back", true)
                        intent.putExtra("term", true)
                        startActivity(intent)
                        activity?.finish()
                    }
                    .show()
            }
        }

        initCommandButton()

        btn_password.setOnClickListener {
            passwordManager.enterPassword()
        }

        value_mac.setOnClickListener {
            copyToClipBoard(value_mac.text.toString().replace(":", ""))
        }

        swipeRefreshLayout.setOnRefreshListener{
            swipeRefreshLayout.isRefreshing = false
            viewModel.readInfo()
        }

        viewModel.uiActiveButton.observe(viewLifecycleOwner, Observer {
            if (it) {
                btn_password.setImageResource(R.drawable.ic_lock_open)
                button_sleep.isEnabled = true
                button_update.isEnabled = true
                button_reboot.isEnabled = true
                button_change_password.isEnabled = true
            } else {
                btn_password.setImageResource(R.drawable.ic_lock)
            }
        })

        viewModel.readyToUpdate.observe(viewLifecycleOwner, Observer {
            if (it) {
                update()
                showSnack(getString(R.string.wait_firmware_mode))
            }
        })

        viewModel.messageLiveData.observe(viewLifecycleOwner, Observer {
            it?.let {
                when (it) {
                    MessageType.ERROR -> showSnack(getString(R.string.error))
                    MessageType.PASSWORD_ACCEPTED -> showSnack(getString(R.string.password_accepted))
                    MessageType.PASSWORD_NOT_ACCEPTED -> showSnack(getString(R.string.password_not_accepted))
                    MessageType.COMMAND_APPLY -> showSnack(getString(R.string.send_ok))
                }
            }
        })

    }

    private fun copyToClipBoard(string: String) {
        val clipboard: ClipboardManager? = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        val clip: ClipData = ClipData.newPlainText("mac", string)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "$string ${resources.getString(R.string.copyed)}", Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initCommandButton() {
        button_sleep.setOnClickListener {
            if (!Sensor.sensorAuthorized) dialogNotAuth()
            else {
                mConfirmDialog = AlertDialog.Builder(context!!, R.style.AlertDialogCustom)
                    .setTitle(R.string.app_name)
                    .setMessage(getString(R.string.apply_command, button_sleep_text.text))
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        viewModel.sendCommand(CMD_FUEL_POWER_DOWN) }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }


        button_update.setOnClickListener {
            if (!Sensor.sensorAuthorized) dialogNotAuth()
            else {
                mConfirmDialog = AlertDialog.Builder(context!!, R.style.AlertDialogCustom)
                    .setTitle(R.string.app_name)
                    .setMessage(getString(R.string.apply_command, getString(R.string.upgrade_firmware)))
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        viewModel.sendCommand(CMD_FUEL_RESET_TO_PERSISTENT) }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }


        button_reboot.setOnClickListener {
            if (!Sensor.sensorAuthorized) dialogNotAuth()
            else {
                mConfirmDialog = AlertDialog.Builder(context!!, R.style.AlertDialogCustom)
                    .setTitle(R.string.app_name)
                    .setMessage(getString(R.string.apply_command, getString(R.string.reboot)))
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        viewModel.sendCommand(CMD_FUEL_RESET) }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }

        button_change_password.setOnClickListener {
            if (!Sensor.sensorAuthorized) dialogNotAuth()
            else {
                mConfirmDialog = AlertDialog.Builder(context!!, R.style.AlertDialogCustom)
                    .setTitle(R.string.app_name)
                    .setMessage(getString(R.string.apply_command, getString(R.string.change_password)))
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        passwordManager.changePassword() }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }
    }

    private fun dialogNotAuth() {
        Snackbar.make(requireView(), R.string.authorization_required, Snackbar.LENGTH_SHORT)
            .setAction(getString(R.string.auth), View.OnClickListener {
                passwordManager.enterPassword() })
            .show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkAuth()
    }

    fun showFileSelector() {
        val aD = AlertDialog.Builder(requireContext(), R.style.AlertDialogCustom)
        aD.setTitle(getString(R.string.select_image))
        aD.setMessage(getString(R.string.select_file_firmware))
        aD.setPositiveButton("OK") { dialogInterface, i ->
            val intent =
                Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.type = "application/octet-stream"
            startActivityForResult(intent, READ_REQUEST_CODE) }
            .create()
            .show()
    }

    fun update() {
        dialogFirmWare = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.expect))
            .setMessage(getString(R.string.preparing_device))
            .create()
        dialogFirmWare?.show()

        val device: RxBleDevice = App.rxBleClient.getBleDevice(MainPref.deviceMac)
        client = TIOADEoadClient(context)

        client?.initializeTIOADEoadProgrammingOnDevice(device.bluetoothDevice, object :
            TIOADEoadClientProgressCallback {
            override fun oadProgressUpdate(percent: Float, currentBlock: Int) {}

            override fun oadStatusUpdate(status: TIOADEoadDefinitions.oadStatusEnumeration?) {
                Log.d("UPDATE", "OAD Status update : $status")
                val finalStatus = status!!
                activity?.runOnUiThread {

                    text_status.setText(
                        TIOADEoadDefinitions.oadStatusEnumerationGetDescriptiveString(
                            finalStatus
                        )
                    )

                    if (finalStatus == TIOADEoadDefinitions.oadStatusEnumeration.tiOADClientReady) {
                        showFileSelector()
                        dialogFirmWare?.dismiss()
                    }

                    if (finalStatus == TIOADEoadDefinitions.oadStatusEnumeration.tiOADClientFileIsNotForDevice) {
                        showAlert(finalStatus)
                        dialogFirmWare?.dismiss()
                    }

                    if (finalStatus == TIOADEoadDefinitions.oadStatusEnumeration.tiOADClientCompleteFeedbackOK) {
                        dialogFirmWare?.dismiss()
                        val aD = AlertDialog.Builder(requireContext())
                        aD.setTitle(getString(R.string.success))
                        aD.setMessage(getString(R.string.oad_upgrade_complete))
                        aD.setPositiveButton(
                            "OK",
                            DialogInterface.OnClickListener { dialogInterface, i ->
                                backToScan()
                                return@OnClickListener })
                            .create().show()
                    }

                    if (finalStatus == TIOADEoadDefinitions.oadStatusEnumeration.tiOADClientCompleteDeviceDisconnectedDuringProgramming) {
                        showAlert(finalStatus)
                        dialogFirmWare?.dismiss()
                    }
                }
            }
        })
    }

    fun showAlert(status: TIOADEoadDefinitions.oadStatusEnumeration?) {
        val aD = AlertDialog.Builder(requireContext())
        aD.setTitle("Error")
            .setMessage("Error : " + TIOADEoadDefinitions.oadStatusEnumerationGetDescriptiveString(status))
            .setPositiveButton("OK", DialogInterface.OnClickListener { _, _ -> })
            .show()
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                fileURL = data.data
                client?.start(fileURL)

                dialogFirmWare = AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.expect))
                    .setMessage(getString(R.string.download_firmware_image))
                    .create()
                dialogFirmWare?.show()
            }
        }
    }

    override fun enterPassword(password: String) {
        viewModel.enterPassword(password)
    }

    override fun changePassword(password: String) {
        viewModel.changePassword(password)
        sendSensorPassword(password)
    }

    override fun returnedError(textError: String) {
        showSnack(textError)
    }

    private fun showSnack(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
    }

    fun backToScan() {
        viewModel.clearCache()

        val intent = Intent(context, ChooseDeviceActivity::class.java)
        intent.putExtra("back", true)
        startActivity(intent)
        activity?.finish()
    }

    private fun sendSensorPassword(pos: String) {
        RetrofitClient.getApi()
            .sensorPassword(
                "1",
                MainPref.deviceMac.replace(":", ""),
                pos)
            .enqueue(object : retrofit2.Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    Log.d("INET sensorPassword", response.body()!!.string())
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.d("INET sensorPassword", t.message!!)
                }
            })
    }
}
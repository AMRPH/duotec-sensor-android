package com.gelios.configurator.ui.update

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import cn.wch.blelib.ch583.constant.Constant
import com.gelios.configurator.MainPref
import com.gelios.configurator.R
import com.gelios.configurator.entity.ScanBLESensor
import com.gelios.configurator.ui.App
import com.gelios.configurator.ui.sensor.fuel.DeviceFuelActivity
import com.gelios.configurator.ui.sensor.relay.DeviceRelayActivity
import com.gelios.configurator.ui.sensor.therm.DeviceThermometerActivity
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_ota_update.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


class OTAUpdateActivity : AppCompatActivity() {

    private lateinit var viewModel: OTAUpdateViewModel
    private lateinit var rxPermissions: RxPermissions
    val PICKFILE_RESULT_CODE = 42

    var mConfirmDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[OTAUpdateViewModel::class.java]
        setContentView(R.layout.activity_ota_update)
        rxPermissions = RxPermissions(this)

        viewModel.createOTAUpdater(this)

        viewModel.macLiveData.observe(this) {
            tv_mac.text = it
        }


        viewModel.stateLiveData.observe(this) {
            tv_state.text = it
        }

        viewModel.progressLiveData.observe(this) {
            if (it.isEmpty()) {
                cl_progress.visibility = View.INVISIBLE
            } else {
                cl_progress.visibility = View.VISIBLE
                tv_progress.text = it
            }
        }


        viewModel.fileNameLiveData.observe(this) {
            tv_file_name.text = it
        }

        viewModel.isFileLiveData.observe(this) {
            if (it) {
                btn_start_update.isEnabled = true
                btn_start_update.setBackgroundResource(R.drawable.bg_button_green)
            } else {
                btn_start_update.isEnabled = false
                btn_start_update.setBackgroundResource(R.drawable.bg_button_grey)
            }
        }

        viewModel.isUpdatingLiveData.observe(this) {
            if (it){
                progress2.visibility = View.VISIBLE
                btn_start_update.isEnabled = false
                btn_start_update.setBackgroundResource(R.drawable.bg_button_grey)
                btn_select_file.isEnabled = false
                btn_select_file.setBackgroundResource(R.drawable.bg_button_grey)
            }else {
                progress2.visibility = View.GONE
                btn_start_update.isEnabled = true
                btn_start_update.setBackgroundResource(R.drawable.bg_button_green)
            }
        }


        viewModel.resultLiveData.observe(this) {
            if (it.first == Result.COMPLETE) {
                showCompleteDialog()
            } else if (it.first == Result.ERROR) {
                showErrorDialog(it.second)
            }
        }

        btn_select_file.setOnClickListener {
            //showSelectFileDialog()
            chooseFile()
        }

        btn_back_to_sensor.setOnClickListener {
            backToSensor()
        }

        btn_start_update.setOnClickListener {
            viewModel.startUpdate()
        }
    }

    override fun onResume() {
        super.onResume()
        rxPermissions.request(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ).subscribe {
            if (!it) {
                btn_select_file.isEnabled = false
                btn_select_file.setBackgroundResource(R.drawable.bg_button_grey)
            }
        }
    }

    private fun backToSensor() {
        App.restartBleClient()
        viewModel.cancel()
        when (MainPref.typeDevices.getValue(MainPref.deviceMac)){
            ScanBLESensor.TYPE.Thermometer ->{
                val intent =  DeviceThermometerActivity.instance(
                    context = this,
                    isFirmware = false)
                startActivity(intent)
            }
            ScanBLESensor.TYPE.Fuel ->{
                val intent = DeviceFuelActivity.instance(
                    context = this,
                    isFirmware = false)
                startActivity(intent)
            }
            ScanBLESensor.TYPE.Relay ->{
                val intent =  DeviceRelayActivity.instance(
                    context = this,
                    isFirmware = false)
                startActivity(intent)
            }
        }
        finish()
    }

    private fun showErrorDialog(error: String) {
        mConfirmDialog = AlertDialog.Builder(this, R.style.AlertDialogCustom)
            .setTitle(R.string.app_name)
            .setMessage("Произошла ошибка $error")
            .setPositiveButton(android.R.string.ok) { dialog, which ->
                backToSensor()
            }
            .show()
    }

    private fun showCompleteDialog() {
        mConfirmDialog = AlertDialog.Builder(this, R.style.AlertDialogCustom)
            .setTitle(R.string.app_name)
            .setMessage("Обновление завершено успешно")
            .setPositiveButton(android.R.string.ok) { dialog, which ->
                backToSensor()
            }
            .show()
    }


    //old select file
    /*
    private fun showSelectFileDialog() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("UPDATE", "1")
            return
        }
        val otaDir = getExternalFilesDir(Constant.OTA_FOLDER)
        val image = File(otaDir, Constant.OTA_FOLDER_IMAGE)
        if (!image.exists()) {
            Log.d("UPDATE", "2")
            return
        }
        val files = image.listFiles { dir, name ->
            name.endsWith(".hex") || name.endsWith(".HEX")
        }
        if (files == null || files.isEmpty()) {
            Log.d("UPDATE", "3")
            showDescriptionDialog()
            return
        }
        val dialog = FileSelectDialog.newInstance(ArrayList<File>(listOf<File>(*files)))
        dialog.isCancelable = false
        dialog.show(supportFragmentManager, FileSelectDialog::class.java.simpleName)
        dialog.setOnChooseListener(object : FileSelectDialog.OnChooseFileListener {

            override fun onChoose(file: File?) {
                viewModel.setTargetFile(file!!)
            }
        })
    }

    private fun showDescriptionDialog() {
        mConfirmDialog = AlertDialog.Builder(this, R.style.AlertDialogCustom)
            .setTitle(R.string.app_name)
            .setMessage("Переместите образ прошивки в Android/data/com.gelios.configurator/files/Configurator/image")
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
     */


    private fun chooseFile() {
        var chooseFile = Intent(Intent.ACTION_OPEN_DOCUMENT)
        chooseFile.type = "*/*"
        chooseFile = Intent.createChooser(chooseFile, "Choose a file")
        startActivityForResult(chooseFile, PICKFILE_RESULT_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICKFILE_RESULT_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                val uri: Uri = data.data!!
                Log.d("UPDATE", uri.path!!.split(":")[1])
                val path = uri.path!!.split(":")[1]

                val source = File(path)
                val filename = path.split("/").last()
                val destination = File(getExternalFilesDir(Constant.OTA_FOLDER), filename)
                copy(source, destination)

                viewModel.setTargetFile(destination)
            }
        }
    }

    private fun copy(source: File, destination: File){
        val inn = FileInputStream(source).channel
        val out = FileOutputStream(destination).channel

        try {
            inn.transferTo(0, inn.size(), out)
        } catch (_: java.lang.Exception){

        } finally {
            inn.close()
            out.close()
        }
    }
}
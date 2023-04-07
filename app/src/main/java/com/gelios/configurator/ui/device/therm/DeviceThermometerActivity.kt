package com.gelios.configurator.ui.device.therm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.work.WorkManager
import com.gelios.configurator.R
import com.gelios.configurator.entity.Sensor
import com.gelios.configurator.ui.App
import com.gelios.configurator.ui.device.therm.fragments.monitoring.HomeThermometerViewModel.Companion.WORKER_TAG
import com.google.android.material.bottomnavigation.BottomNavigationView


class DeviceThermometerActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    companion object {
        const val EXTRA_SENSOR_IS_FIRMWARE = "soft.gelios.configurator.ui.EXTRA_SENSOR_IS_FIRMWARE"

        fun instance(context: Context, isFirmware: Boolean): Intent {
            val intent = Intent(context, DeviceThermometerActivity::class.java)
            intent.putExtra(EXTRA_SENSOR_IS_FIRMWARE, isFirmware)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_thermometer)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        navController = findNavController(R.id.nav_host_fragment)

        navView.setupWithNavController(navController)
        WorkManager.getInstance().cancelAllWorkByTag(WORKER_TAG)

        if (intent.getBooleanExtra(EXTRA_SENSOR_IS_FIRMWARE, false)) {
            val bundle = bundleOf("firmware" to true )
            navController.navigate(R.id.action_global_navigation_logs, bundle)
        }
    }

    override fun onBackPressed(){
        if (navController.graph.startDestinationId == navController.currentDestination?.id) {
            showQuitDialog()
        } else {
            super.onBackPressed()
        }
    }

    private fun showQuitDialog() {
        val dialog = AlertDialog.Builder(this, R.style.AlertDialogCustom)
        dialog.setTitle(getString(R.string.log_off))
            .setPositiveButton(android.R.string.yes) { dialog, which ->
                WorkManager.getInstance().cancelAllWorkByTag(WORKER_TAG)
                Sensor.clearSensorData()
                App.bleCompositeDisposable.clear()
                super.onBackPressed() }
            .setNegativeButton(android.R.string.no) { dialog, which -> dialog.dismiss() }
            .show()

    }

}

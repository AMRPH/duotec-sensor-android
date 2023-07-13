package com.gelios.configurator.ui.dialog

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.gelios.configurator.R
import com.gelios.configurator.ui.sensor.fuel.fragments.monitoring.HomeFuelFragment
import com.gelios.configurator.ui.sensor.relay.fragments.monitoring.HomeRelayFragment
import com.gelios.configurator.ui.sensor.therm.fragments.monitoring.HomeThermometerFragment
import kotlinx.android.synthetic.main.dialog_connecting.*

class ConnectingDialog(fragment: Fragment) : DialogFragment() {
    private val parentFragment: Fragment
    init {
        parentFragment = fragment
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_connecting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btn_cancel.setOnClickListener {
            if (parentFragment is HomeThermometerFragment){
                parentFragment.onBack()
            }
            if (parentFragment is HomeFuelFragment){
                parentFragment.onBack()
            }
            if (parentFragment is HomeRelayFragment){
                parentFragment.onBack()
            }
        }
    }
}
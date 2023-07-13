package com.gelios.configurator.ui.dialog

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import com.gelios.configurator.R
import com.gelios.configurator.ui.sensor.fuel.fragments.tarirovka.DataTarirovka
import kotlinx.android.synthetic.main.edit_tarirovka.*

class EditTarirovkaDialog : DialogFragment() {

    var inDataTarirovka: DataTarirovka? = null

    companion object {
        fun newInstance(dataTarirovka: DataTarirovka): EditTarirovkaDialog {
            val fragment = EditTarirovkaDialog()
            val args = Bundle()
            args.putParcelable("DataTarirovka", dataTarirovka)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var callback: Callback

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.edit_tarirovka, container, false)
        inDataTarirovka = arguments?.getParcelable<DataTarirovka>("DataTarirovka")
            if (dialog != null && dialog!!.window != null) {
                dialog!!.setCanceledOnTouchOutside(false)
                dialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dialog!!.window!!.requestFeature(Window.FEATURE_NO_TITLE)
            }
        return view
    }

    @SuppressLint("SetTextI18n")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)



        if (inDataTarirovka != null) {
            value_counter.setText(inDataTarirovka?.counter.toString())
            value_level.setText(inDataTarirovka?.sensorLevel)
            value_litr.setText(inDataTarirovka?.fuelLevel)
        }

        button_ok.setOnClickListener {
            dismiss()
            callback.clickOk(
                DataTarirovka(
                    value_counter.text.toString().toInt(),
                    value_litr.text.toString(),
                    value_level.text.toString()
                )
            )
        }

        button_cancel.setOnClickListener { dismiss() }

    }

    interface Callback {
        fun clickOk(dataTarirovka: DataTarirovka)
    }

    fun setCallback(callback: Callback) {
        this.callback = callback
    }
}
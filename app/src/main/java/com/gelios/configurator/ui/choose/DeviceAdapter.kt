package com.gelios.configurator.ui.choose

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gelios.configurator.R
import com.gelios.configurator.entity.ScanBLESensor
import kotlinx.android.synthetic.main.item_device.view.*
import kotlinx.android.synthetic.main.item_device.view.battery
import kotlinx.android.synthetic.main.item_device.view.ic_connection
import kotlinx.android.synthetic.main.item_device.view.ic_type
import kotlinx.android.synthetic.main.item_device.view.mac_address
import kotlinx.android.synthetic.main.item_device.view.signal_strangcth
import kotlinx.android.synthetic.main.item_device.view.soft_version
import kotlinx.android.synthetic.main.item_device.view.time
import kotlinx.android.synthetic.main.item_device_relay.view.*

class DeviceAdapter(listener: OnItemClickListener) : RecyclerView.Adapter<DeviceAdapter.DeviceAdapterViewHolder>() {

    private lateinit var context: Context
    var listener: OnItemClickListener? = null
    private var isFirst: Boolean = false

    private var mData: MutableList<ScanBLESensor>? = mutableListOf()

    private val TYPE_RELAY = 0
    private val TYPE_OTHERS = 1

    init {
        this.listener = listener
    }

    interface OnItemClickListener {

        fun connect(item: ScanBLESensor)

    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, type: Int): DeviceAdapterViewHolder {
        lateinit var v : View
        isFirst = true

        when (type) {
            TYPE_RELAY -> {
                v = LayoutInflater.from(viewGroup.context).inflate(R.layout.item_device_relay, viewGroup, false)
            }
            TYPE_OTHERS -> {
                v = LayoutInflater.from(viewGroup.context).inflate(R.layout.item_device, viewGroup, false)
            }
        }
        return DeviceAdapterViewHolder(v)
    }

    override fun getItemCount(): Int {
        return mData?.size ?: 0
    }

    override fun onBindViewHolder(holder: DeviceAdapterViewHolder, position: Int) {
        mData?.let {
            val item = it[position]
            holder.onBind(item, position)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (mData!![position].type == ScanBLESensor.TYPE.Relay) {
            TYPE_RELAY
        } else {
            TYPE_OTHERS
        }
    }

    fun addItems(it: ScanBLESensor) {
        this.mData?.add(it)
        notifyDataSetChanged()
    }

    fun setItems(it: List<ScanBLESensor>?) {
        this.mData?.clear()
        this.mData?.addAll(it!!)
        notifyDataSetChanged()
    }

    inner class DeviceAdapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        init {
            context = itemView.context
        }

        fun onBind(item: ScanBLESensor, position: Int) {
            itemView.mac_address.text = item.mac
            itemView.signal_strangcth.text = item.getSignalString()
            itemView.time.text = item.getTimeString()
            itemView.soft_version.text = item.getSoftString()
            itemView.battery.text = item.battery
            itemView.setOnClickListener { listener?.connect(item) }
            itemView.ic_type.setImageResource(selectIconForSensor(item.type))
            itemView.ic_connection.setImageResource(selectIconForConnection(item.signal))

            when (getItemViewType(position)) {
                TYPE_RELAY -> {
                    when (item.data.toInt()) {
                        0 -> {
                            itemView.relay_data.setImageResource(R.drawable.relay_on_white)
                        }
                        1 -> {
                            itemView.relay_data.setImageResource(R.drawable.relay_off_white)
                        }
                    }
                }
                TYPE_OTHERS -> {
                    itemView.sensor_data.text = item.getDataString()
                }
            }
        }

        private fun selectIconForSensor(type: ScanBLESensor.TYPE): Int {
            return when (type) {
                ScanBLESensor.TYPE.Fuel -> R.drawable.ic_fuel
                ScanBLESensor.TYPE.Firmware -> R.drawable.ic_firmware
                ScanBLESensor.TYPE.Thermometer -> R.drawable.ic_therm
                ScanBLESensor.TYPE.Relay -> R.drawable.ic_relay
                else -> R.drawable.ic_firmware
            }
        }

        private fun selectIconForConnection(signal: Int): Int {
            return if (signal < 75) {
                R.drawable.ic_connection_green
            } else R.drawable.ic_connection_red
        }
    }




}
package com.gelios.configurator.ui.choose

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gelios.configurator.R
import com.gelios.configurator.entity.BLESensor
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

    private var mData: MutableList<BLESensor> = mutableListOf()

    private val TYPE_RELAY = 0
    private val TYPE_OTHERS = 1

    init {
        this.listener = listener
    }

    interface OnItemClickListener {

        fun connect(item: BLESensor)

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
        return mData.size ?: 0
    }

    override fun onBindViewHolder(holder: DeviceAdapterViewHolder, position: Int) {
        val item = mData[position]
        holder.onBind(item, position)
    }

    override fun getItemViewType(position: Int): Int {
        return if (mData[position].type == BLESensor.TYPE.Relay) {
            TYPE_RELAY
        } else {
            TYPE_OTHERS
        }
    }

    fun addItems(it: BLESensor) {
        this.mData.add(it)
        notifyDataSetChanged()
    }

    fun setItems(it: List<BLESensor>) {
        this.mData.clear()
        this.mData.addAll(it)
        notifyDataSetChanged()
    }

    inner class DeviceAdapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        init {
            context = itemView.context
        }

        fun onBind(item: BLESensor, position: Int) {
            itemView.mac_address.text = item.mac
            itemView.signal_strangcth.text = "${item.signal}dB"
            itemView.time.text = "${item.time} sec"
            itemView.soft_version.text = getSoft(item.soft)
            itemView.battery.text = item.battery

            itemView.setOnClickListener { listener?.connect(item) }

            itemView.ic_type.setImageResource(getIconSensor(item.type))
            itemView.ic_connection.setImageResource(getIconConnection(item.signal))

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
                    itemView.sensor_data.text = getData(item.data, item.type)
                }
            }
        }

        private fun getIconSensor(type: BLESensor.TYPE): Int {
            return when (type) {
                BLESensor.TYPE.Fuel -> R.drawable.ic_fuel
                BLESensor.TYPE.Firmware -> R.drawable.ic_firmware
                BLESensor.TYPE.Thermometer -> R.drawable.ic_therm
                BLESensor.TYPE.Relay -> R.drawable.ic_relay
                else -> R.drawable.ic_firmware
            }
        }

        private fun getIconConnection(signal: Int): Int {
            return if (signal < 75) {
                R.drawable.ic_connection_green
            } else R.drawable.ic_connection_red
        }

        private fun getSoft(soft: String): String{
            return if (soft == "MINI") "MINI"
            else "FW $soft"
        }

        private fun getData(data: String, type: BLESensor.TYPE): String{
            return when (type){
                BLESensor.TYPE.Thermometer ->{
                    return if (data == "error") context.getString(R.string.error_value)
                    else "$data °C"
                }
                BLESensor.TYPE.Fuel ->{
                    return if (data == "error") context.getString(R.string.error_value)
                    else "${data}%"
                }
                BLESensor.TYPE.Relay ->{
                    return if (data == "error") context.getString(R.string.error_value)
                    else data
                }
                else -> ""
            }
        }
    }




}
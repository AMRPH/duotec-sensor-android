package com.gelios.configurator.ui.sensor.fuel.fragments.tarirovka

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.gelios.configurator.R
import kotlinx.android.synthetic.main.items_tarirovka.view.*

class TarirovkaFuelAdapter(listener: OnItemClickListener) : RecyclerView.Adapter<TarirovkaFuelAdapter.DeviceAdapterViewHolder>() {

    private lateinit var context: Context
    var listener: OnItemClickListener? = null
    private var isFirst: Boolean = false

    private var mData: MutableList<DataTarirovka>? = mutableListOf()


    init {
        this.listener = listener
    }

    interface OnItemClickListener {

        fun edit(item: DataTarirovka)

    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, type: Int): DeviceAdapterViewHolder {
        isFirst = true
        return DeviceAdapterViewHolder(
            LayoutInflater.from(viewGroup.context).inflate(R.layout.items_tarirovka, viewGroup, false)
        )
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

    fun addItems(it: DataTarirovka) {
        this.mData?.add(it)
        notifyDataSetChanged()
    }

    fun setItems(it: List<DataTarirovka>?) {
        this.mData?.clear()
        this.mData?.addAll(it!!)
        notifyDataSetChanged()
    }

    inner class DeviceAdapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        init {
            context = itemView.context
        }

        fun onBind(item: DataTarirovka, position: Int) {
            val counter = (mData!!.size - (position)).toString()
            itemView.value_litr.text = item.fuelLevel
            itemView.value_level.text = item.sensorLevel
            if (item == mData?.first()) {
                itemView.setBackgroundColor(context.resources.getColor(R.color.colorValuesMonitoring))
                itemView.value_litr.text = item.fuelLevel
                itemView.value_level.text = item.sensorLevel
            } else {
                itemView.setBackgroundColor(context.resources.getColor(R.color.colorPrimaryLight))
            }
            itemView.setOnClickListener {
                item.counter = counter.toInt()
                listener?.edit(item) }
            itemView.value_counter.text = counter
            checkError(position, itemView.layout)
        }
    }

    private fun checkError(
        position: Int,
        layout: FrameLayout
    ) {
        try {
            val v = mData!![position].fuelLevel.toIntOrNull()?:0
            val vPrev = mData!![position-1].fuelLevel.toIntOrNull()?:0

            if (v > vPrev) {
                layout.setBackgroundResource(R.color.colorTarirovkaEroor)
            } else {
                layout.setBackgroundResource(R.color.colorPrimaryLight)
            }
        } catch (e: ArrayIndexOutOfBoundsException) {

        }

    }


}
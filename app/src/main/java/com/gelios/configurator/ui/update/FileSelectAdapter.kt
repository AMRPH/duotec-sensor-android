package com.gelios.configurator.ui.update

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gelios.configurator.R
import java.io.File

class FileSelectAdapter(
    private val context: Context,
    private val list: ArrayList<File>?,
    private val listener: OnClickListener?
) :
    RecyclerView.Adapter<FileSelectAdapter.MyViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(context).inflate(R.layout.dialog_file_select_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val file = list!![position]
        holder.name.text = file.name
        holder.itemView.setOnClickListener { listener?.onClick(file) }
    }

    override fun getItemCount(): Int {
        return list?.size ?: 0
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var name: TextView

        init {
            name = itemView.findViewById<TextView>(R.id.file)
        }
    }

    interface OnClickListener {
        fun onClick(file: File?)
    }
}

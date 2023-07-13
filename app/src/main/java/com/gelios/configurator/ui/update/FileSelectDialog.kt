package com.gelios.configurator.ui.update

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gelios.configurator.R
import kotlinx.android.synthetic.main.dialog_file_select.*
import java.io.File

class FileSelectDialog(private val list: ArrayList<File>) : DialogFragment() {
    private var adapter: FileSelectAdapter? = null
    private var listener: OnChooseFileListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return inflater.inflate(R.layout.dialog_file_select, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = FileSelectAdapter(requireActivity(), list, object :
            FileSelectAdapter.OnClickListener {
            override fun onClick(file: File?) {
                if (listener != null) {
                    listener!!.onChoose(file)
                }
                dismiss()
            }
        })

        rv_select.layoutManager = LinearLayoutManager(
            activity,
            LinearLayoutManager.VERTICAL,
            false
        )

        rv_select.adapter = adapter
        val decoration = DividerItemDecoration(activity, DividerItemDecoration.VERTICAL)
        rv_select.addItemDecoration(decoration)
        btn_close.setOnClickListener(View.OnClickListener { dismiss() })
    }

    fun setOnChooseListener(listener: OnChooseFileListener?) {
        this.listener = listener
    }

    interface OnChooseFileListener {
        fun onChoose(file: File?)
    }

    companion object {
        fun newInstance(list: ArrayList<File>): FileSelectDialog {
            return FileSelectDialog(list)
        }
    }
}
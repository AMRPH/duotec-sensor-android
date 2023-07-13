package com.gelios.configurator.ui.sensor.fuel.fragments.tarirovka

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import com.gelios.configurator.MainPref
import com.gelios.configurator.R
import com.gelios.configurator.ui.dialog.EditTarirovkaDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_tarirovka.*

private const val REQUEST_SAF = 1337
private const val REQUEST_PERMS = 123

class TarirovkaFragment : Fragment(),
    TarirovkaFuelAdapter.OnItemClickListener,
    EditTarirovkaDialog.Callback {
    private var current: Uri? = null

    private lateinit var viewModel: TarirovkaViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel =
            ViewModelProvider(this).get(TarirovkaViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_tarirovka, container, false)
        return root
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        handleModel()

        val deviceAdapter =
            TarirovkaFuelAdapter(
                this
            )
        rv_tarirovka.adapter = deviceAdapter
        val decoration = DividerItemDecoration(rv_tarirovka.context, DividerItemDecoration.VERTICAL)
        rv_tarirovka.addItemDecoration(decoration)
        viewModel.uiTableTarirovka.observe(viewLifecycleOwner, Observer {
            deviceAdapter.setItems(it.reversed())
        })

        viewModel.uiFuelLevel.observe(viewLifecycleOwner, Observer {
            if (it == 32768) {
                value_level.text = getString(R.string.error_value)
            } else {
                value_level.text = it.toString()
            }

        })

        viewModel.uiFuelStability.observe(viewLifecycleOwner, Observer {
            updateIndicator(it)
        })

        initButton()

        text_comment.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.textComment = s.toString()
                MainPref.comment = s.toString()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        text_comment.setText(MainPref.comment)
    }

    private fun updateIndicator(it: Boolean?) {
        when (it) {
            null -> {
                image_stability_indicator.setImageResource(R.drawable.circle_grey)
            }
            true -> {
                image_stability_indicator.setImageResource(R.drawable.circle_green)
                title_statility.text = getString(R.string.stably)
            }
            false -> {
                image_stability_indicator.setImageResource(R.drawable.circle_red)
                title_statility.text = getString(R.string.stabilization)
            }
        }
    }

    private fun initButton() {
        button_add_arrow.setOnClickListener { viewModel.addArrow() }
        button_remove_arrow.setOnClickListener { viewModel.removeArrow() }
        button_clear.setOnClickListener { clearTableDialog() }
        button_save.setOnClickListener {
            viewModel.sendDataToServer()
            saveDialog()
        }
        button_send.setOnClickListener {
            viewModel.sendDataToServer()
            sendString()
        }
        text_step.setOnClickListener { changeStepDialog() }
        button_chart.setOnClickListener { showChart(viewModel.uiTableTarirovka.value) }
    }

    private fun showChart(value: List<DataTarirovka>?) {
        val intent = Intent(context, ChartActivity::class.java)
        value?.let {
            intent.putExtra("data", it.toTypedArray())
            intent.putExtra("name", MainPref.deviceMac)
        }
        startActivity(intent)

    }


    private fun saveDialog() {
        if (havePermission()) {
            try {
                startActivityForResult(
                    Intent(Intent.ACTION_CREATE_DOCUMENT)
                        .setType("text/plain")
                        .addCategory(Intent.CATEGORY_OPENABLE),
                    REQUEST_SAF
                )
            } catch (ex: ActivityNotFoundException) {
                Toast.makeText(
                    requireContext(),
                    "Sorry, we cannot open a document!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }


    }

    fun sendString() {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, viewModel.getSavedString())
            type = "text/plain"
        }
        val chosenIntent =
            Intent.createChooser(sendIntent, "Отправить")
        startActivity(chosenIntent)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_SAF) {
            if (resultCode == RESULT_OK && data != null) {
                data.data?.let {
                    current = it
                    viewModel.write(it)
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun handleModel() {
        viewModel.uiProgressLiveData.observe(viewLifecycleOwner, Observer {
            if (it) {
                progress.visibility = View.VISIBLE
            } else {
                progress.visibility = View.GONE

            }
        })

        viewModel.errorMessageLiveData.observe(viewLifecycleOwner, Observer {
            Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
        })

        viewModel.uiAuthorization.observe(viewLifecycleOwner, Observer {
            if (it) {
            }
        })

        viewModel.tarirovkaStep.observe(viewLifecycleOwner, Observer {
            text_step.text = "$it ${getString(R.string.l)}"
        })
    }

    fun changeStepDialog() {
        val builder = AlertDialog.Builder(requireContext(), R.style.AlertDialogCustom)
        builder.setTitle(getString(R.string.specify_calibration_step))
        val input = EditText(requireContext())
        input.setText(viewModel.tarirovkaStep.value!!.toString())
        input.isSingleLine = true
        input.inputType = InputType.TYPE_CLASS_NUMBER
        builder.setView(input)
        builder.setPositiveButton(
            android.R.string.ok
        ) { dialog, which ->
            val stepValue = input.text.toString()
            viewModel.setStep(stepValue.toInt())
            MainPref.stepFuel = stepValue.toInt()
        }
        builder.setNegativeButton(android.R.string.cancel,
            { dialog, which -> dialog.cancel() })
        builder.show()
    }

    fun clearTableDialog() {
        val builder = AlertDialog.Builder(requireContext(), R.style.AlertDialogCustom)
        builder.setTitle(R.string.clear_table)
        builder.setPositiveButton(
            android.R.string.ok
        ) { dialog, which ->
            viewModel.clearTable()
        }
        builder.setNegativeButton(android.R.string.cancel,
            { dialog, which -> dialog.cancel() })
        builder.show()
    }

    override fun edit(item: DataTarirovka) {
        val dialog = EditTarirovkaDialog.newInstance(item)
        dialog.setCallback(this)
        dialog.show(childFragmentManager, "DataTarirovka")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_PERMS) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                current?.let { viewModel.write(it) }
            } else {
                Toast.makeText(
                    context,
                    getString(R.string.not_permission),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun havePermission(): Boolean {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        } else {
            val perms = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            ActivityCompat.requestPermissions(requireActivity(), perms,
                REQUEST_PERMS
            )
            return false
        }
    }

    private fun showSnack(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show()
    }

    override fun clickOk(dataTarirovka: DataTarirovka) {
        viewModel.changeFuelValue(dataTarirovka)
    }
}

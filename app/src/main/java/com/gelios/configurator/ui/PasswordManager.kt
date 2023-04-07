package com.gelios.configurator.ui

import android.content.Context
import android.text.InputFilter
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.gelios.configurator.R

class PasswordManager(
    private val context: Context,
    private val callback: Callback) {


    fun enterPassword() {
        val builder = AlertDialog.Builder(context, R.style.AlertDialogCustom)
        builder.setTitle(R.string.enter_password)
        val input = EditText(context)
        input.setText("00000000")
        input.setHint(R.string.eight_numbs)
        input.isSingleLine = true
        val fArray = arrayOfNulls<InputFilter>(1)
        fArray[0] = InputFilter.LengthFilter(8)
        input.setFilters(fArray)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)
        builder.setPositiveButton(
            android.R.string.ok
        ) { dialog, which ->
            val password = input.text.toString()
            if (password.isEmpty()) {
                callback.returnedError(context.getString(R.string.not_empty))
            } else {
                callback.enterPassword(password)
            }
        }
        builder.setNegativeButton(android.R.string.cancel,
            { dialog, which -> dialog.cancel() })
        builder.show()
    }

    fun changePassword() {
        val builder = AlertDialog.Builder(context, R.style.AlertDialogCustom)
        builder.setTitle(R.string.enter_new_password)
        val input = EditText(context)
        input.setText("")
        input.setHint(R.string.eight_numbs)
        input.isSingleLine = true
        val fArray = arrayOfNulls<InputFilter>(1)
        fArray[0] = InputFilter.LengthFilter(8)
        input.setFilters(fArray)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)
        builder.setPositiveButton(
            android.R.string.ok
        ) { dialog, which ->
            val password = input.text.toString()
            if (password.isEmpty()) {
                callback.returnedError(context.getString(R.string.not_empty))
            } else {
                if (password.length == 8) {
                    retryPassword(password)
                } else {
                    callback.returnedError(context.getString(R.string.eight_must))
                }
            }
        }
        builder.setNegativeButton(android.R.string.cancel,
            { dialog, which -> dialog.cancel() })
        builder.show()
    }

    private fun retryPassword(newPassword: String) {
        val builder = AlertDialog.Builder(context, R.style.AlertDialogCustom)
        builder.setTitle(R.string.repeat_new_password)
        val input = EditText(context)
        input.setText("")
        input.setHint(R.string.eight_numbs)
        input.isSingleLine = true
        val fArray = arrayOfNulls<InputFilter>(1)
        fArray[0] = InputFilter.LengthFilter(8)
        input.setFilters(fArray)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)
        builder.setPositiveButton(
            android.R.string.ok
        ) { dialog, which ->
            val password = input.text.toString()
            if (password.isEmpty()) {
                callback.returnedError(context.getString(R.string.not_empty))
            } else {
                if (newPassword != password) {
                    callback.returnedError(context.getString(R.string.password_mismatch))
                } else {
                    callback.changePassword(password)
                }
            }
        }
        builder.setNegativeButton(android.R.string.cancel,
            { dialog, which -> dialog.cancel() })
        builder.show()
    }


    interface Callback {
        fun enterPassword(password: String)
        fun changePassword(password: String)
        fun returnedError(textError: String)
    }
}


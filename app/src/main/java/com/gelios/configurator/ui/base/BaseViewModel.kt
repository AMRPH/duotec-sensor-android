package com.gelios.configurator.ui.base

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.gelios.configurator.ui.App
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable

abstract class BaseViewModel(application: Application) : AndroidViewModel(application) {

    abstract val TAG: String

    protected val error: SingleLiveData<Throwable> = SingleLiveData()


    fun getError(): LiveData<Throwable> {
        return error
    }

    val progressing = ObservableBoolean(false)


    fun hideProgress() {
        progressing.set(false)
    }

    fun showProgress() {
        progressing.set(true)
    }

    protected val compositeDisposable: CompositeDisposable by lazy { CompositeDisposable() }

    protected fun addToDisposable(disposable: Disposable) {
        compositeDisposable.add(disposable)

    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()

    }

    fun showMessage(msg: String, length: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(App.instance, msg, length).show()
    }

    fun showMessage(msgRes: Int, length: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(App.instance, msgRes, length).show()
    }

    fun logError(msg: String) {
        Log.e(TAG, msg)
    }

    fun logInfo(msg: String) {
        Log.d(TAG, msg)
    }
}
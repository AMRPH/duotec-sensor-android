package com.gelios.configurator.ui.update

import android.app.Application
import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.gelios.configurator.MainPref
import com.gelios.configurator.ui.base.BaseViewModel
import java.io.File

class OTAUpdateViewModel(application: Application) : BaseViewModel(application){
    override val TAG: String
        get() = javaClass.simpleName

    val macLiveData = MutableLiveData(MainPref.deviceMac)

    val resultLiveData = MutableLiveData(Pair(Result.NULL, ""))
    val stateLiveData = MutableLiveData("")
    val progressLiveData = MutableLiveData("")

    val fileNameLiveData = MutableLiveData("")
    val isFileLiveData = MutableLiveData(false)
    val isUpdatingLiveData = MutableLiveData(false)

    private lateinit var otaUpdater: OTAUpdater

    fun createOTAUpdater(context: Context){
        otaUpdater = OTAUpdater(context, this)
        otaUpdater.createFolder()
    }

    fun setTargetFile(file: File){
        otaUpdater.setTargetFile(file)
        fileNameLiveData.postValue(file.name)
        isFileLiveData.postValue(true)
    }

    fun startUpdate(){
        otaUpdater.startUpdate()
        isUpdatingLiveData.postValue(true)
    }

    fun cancel(){
        otaUpdater.cancel()
        isUpdatingLiveData.postValue(false)
    }
}
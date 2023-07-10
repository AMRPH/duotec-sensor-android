package com.gelios.configurator.util

import android.Manifest
import android.util.Log
import androidx.lifecycle.MutableLiveData
import cn.wch.blelib.ch583.callback.ConnectStatus
import cn.wch.blelib.ch583.ota.CH583OTAManager
import cn.wch.blelib.ch583.ota.callback.IProgress
import cn.wch.blelib.ch583.ota.entry.CurrentImageInfo
import cn.wch.blelib.ch583.ota.exception.CH583OTAException
import cn.wch.blelib.chip.ChipType
import com.gelios.configurator.MainPref
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.File

class OTAUpdater {

    val otaSuccessLiveData = MutableLiveData(false)
    val otaErrorLiveData = MutableLiveData(false)
    val otaMessageLiveData = MutableLiveData("")

    private var targetFile: File? = null
    private var currentImageInfo: CurrentImageInfo? = null

    private lateinit var rxPermissions: RxPermissions

    fun beforeUpdate(){
        targetFile = null
        currentImageInfo = null

        //otaSuccessLiveData.postValue(false)
        //otaErrorLiveData.postValue(false)
    }

    fun setRxPermissions(permissions: RxPermissions){
        rxPermissions = permissions
    }

    fun setTargetFile(file: File){
        targetFile = file
    }

    fun startUpdate(){
        CH583OTAManager.getInstance().connect(MainPref.deviceMac, 30000, object: ConnectStatus {
            override fun OnError(t: Throwable?) {
                Log.d("UPDATE", "connect OnError")
                otaErrorLiveData.postValue(true)
                otaMessageLiveData.postValue("Error ${t!!.message}")
                cancel()
            }

            override fun OnConnecting() {
                Log.d("UPDATE", "connect OnConnecting")
                otaMessageLiveData.postValue("Connecting")
            }

            override fun OnConnectSuccess(mac: String?) {
                Log.d("UPDATE", "connect OnConnectSuccess")
                otaMessageLiveData.postValue("Connect success")
                getCurrentImageInfo()
            }

            override fun onInvalidDevice(mac: String?) {
                Log.d("UPDATE", "connect onInvalidDevice")
                otaErrorLiveData.postValue(true)
                otaMessageLiveData.postValue("Error invalid device")
                cancel()
            }

            override fun OnConnectTimeout(mac: String?) {
                Log.d("UPDATE", "connect OnConnectTimeout")
                otaErrorLiveData.postValue(true)
                otaMessageLiveData.postValue("Error connect timeout")
                cancel()
            }

            override fun OnDisconnect(mac: String?, status: Int) {
                Log.d("UPDATE", "connect OnDisconnect")
                otaErrorLiveData.postValue(true)
                otaMessageLiveData.postValue("Error disconnect")
                cancel()
            }

        })
    }

    private fun getCurrentImageInfo(){
        Observable.create<String>(ObservableOnSubscribe<String?> { emitter ->
            currentImageInfo = CH583OTAManager.getInstance().currentImageInfo

            if (currentImageInfo == null) {
                emitter.onError(Throwable("Image not found"))
                return@ObservableOnSubscribe
            }
            emitter.onComplete()
        }).subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : io.reactivex.Observer<String?> {
                override fun onSubscribe(d: Disposable) {
                    Log.d("UPDATE", "currentImageInfo onSubscribe")
                }

                override fun onError(e: Throwable) {
                    Log.d("UPDATE", "currentImageInfo onError ${e.message}")
                    otaErrorLiveData.postValue(true)
                    otaMessageLiveData.postValue("Error ${e.message}")
                    cancel()
                }

                override fun onNext(t: String) {
                    Log.d("UPDATE", "currentImageInfo onNext $t")
                    otaMessageLiveData.postValue(t)
                }

                override fun onComplete() {
                    Log.d("UPDATE", "currentImageInfo onComplete")
                    otaMessageLiveData.postValue("currentImageInfo complete")
                    update()
                }
            })
    }

    private fun startHexFileUpdate(targetFile : File) {
        Observable.create<String>(ObservableOnSubscribe<String?> { emitter ->
            var hexFileEraseAddr = 0
            if (currentImageInfo!!.chipType == ChipType.CH573 || currentImageInfo!!.chipType == ChipType.CH583) {
                hexFileEraseAddr = try {
                    CH583OTAManager.getInstance().getHexFileEraseAddr(targetFile)
                } catch (e: CH583OTAException) {
                    emitter.onError(Throwable("Error"))
                    return@ObservableOnSubscribe
                }
            }
            CH583OTAManager.getInstance()
                .start(hexFileEraseAddr, targetFile, currentImageInfo!!, object : IProgress {
                    override fun onEraseStart() {
                        Log.d("UPDATE", "onEraseStart")
                        otaMessageLiveData.postValue("erase start")
                    }

                    override fun onEraseFinish() {
                        Log.d("UPDATE", "onEraseFinish")
                        otaMessageLiveData.postValue("erase finish")
                    }

                    override fun onProgramStart() {
                        Log.d("UPDATE", "onProgramStart")
                        otaMessageLiveData.postValue("program start")
                    }

                    override fun onProgramProgress(current: Int, total: Int) {
                        Log.d("UPDATE", "onProgramProgress $current/$total")
                        otaMessageLiveData.postValue("program progress $current/$total")
                    }

                    override fun onProgramFinish() {
                        Log.d("UPDATE", "onProgramFinish")
                        otaMessageLiveData.postValue("program finish")
                    }

                    override fun onVerifyStart() {
                        Log.d("UPDATE", "onVerifyStart")
                        otaMessageLiveData.postValue("verify start")
                    }

                    override fun onVerifyProgress(current: Int, total: Int) {
                        Log.d("UPDATE", "onVerifyProgress $current/$total")
                        otaMessageLiveData.postValue("verify progress $current/$total")
                    }

                    override fun onVerifyFinish() {
                        Log.d("UPDATE", "onVerifyFinish")
                        otaMessageLiveData.postValue("verify finish")
                    }

                    override fun onEnd() {
                        Log.d("UPDATE", "onEnd")
                        otaMessageLiveData.postValue("Update complete!!!!")
                    }

                    override fun onCancel() {
                        Log.d("UPDATE", "onCancel")
                        otaErrorLiveData.postValue(true)
                        otaMessageLiveData.postValue("cancel")
                    }

                    override fun onError(message: String) {
                        Log.d("UPDATE", "onError $message")
                        otaErrorLiveData.postValue(true)
                        otaMessageLiveData.postValue("Error $message")
                        cancel()
                    }
                })
        })
    }

    private fun update(){
        if (targetFile != null && currentImageInfo != null){
            if (isHexFile(targetFile!!)) {
                rxPermissions.request(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ).subscribe { granted: Boolean ->
                    if (granted) startHexFileUpdate(targetFile!!)
                }
            } else {
                otaErrorLiveData.postValue(true)
                otaMessageLiveData.postValue("file not .hex")
                cancel()
            }
        } else {
            otaErrorLiveData.postValue(true)
            otaMessageLiveData.postValue("Error")
            cancel()
        }
    }

    private fun isHexFile(file: File): Boolean {
        return file.exists() && (file.name.endsWith("hex") || file.name.endsWith(
            "HEX"
        ))
    }

    private fun cancel() {
        CH583OTAManager.getInstance().cancel()
        if (CH583OTAManager.getInstance().isConnected(MainPref.deviceMac)){
            CH583OTAManager.getInstance().disconnect(MainPref.deviceMac, true)
        }
    }
}
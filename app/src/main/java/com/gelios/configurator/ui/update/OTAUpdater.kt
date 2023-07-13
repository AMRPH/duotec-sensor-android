package com.gelios.configurator.ui.update

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import cn.wch.blelib.ch583.callback.ConnectStatus
import cn.wch.blelib.ch583.ota.CH583OTAManager
import cn.wch.blelib.ch583.ota.callback.IProgress
import cn.wch.blelib.ch583.ota.entry.CurrentImageInfo
import cn.wch.blelib.ch583.ota.exception.CH583OTAException
import cn.wch.blelib.chip.ChipType
import com.gelios.configurator.MainPref
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.File

class OTAUpdater(private val context: Context, private val viewModel: OTAUpdateViewModel) {
    private var targetFile: File? = null
    private var currentImageInfo: CurrentImageInfo? = null

    fun clearCache(){
        targetFile = null
        currentImageInfo = null
    }

    fun setTargetFile(file: File){
        targetFile = file
    }

    fun createFolder(): Boolean{
        return CH583OTAManager.getInstance().createFolder()
    }

    fun startUpdate(){
        CH583OTAManager.getInstance().connect(MainPref.deviceMac, 30000, object: ConnectStatus {
            override fun OnError(t: Throwable?) {
                Log.d("UPDATE", "connect OnError ${t!!.message}")
                viewModel.resultLiveData.postValue(Pair(Result.ERROR, t.message!!))
            }

            override fun OnConnecting() {
                Log.d("UPDATE", "connect OnConnecting")
                viewModel.stateLiveData.postValue("Connecting")
            }

            override fun OnConnectSuccess(mac: String?) {
                Log.d("UPDATE", "connect OnConnectSuccess")
                viewModel.stateLiveData.postValue("Connect success")
                getCurrentImageInfo()
            }

            override fun onInvalidDevice(mac: String?) {
                Log.d("UPDATE", "connect onInvalidDevice")
                if (viewModel.resultLiveData.value!!.first != Result.ERROR){
                    viewModel.resultLiveData.postValue(Pair(Result.ERROR, "onInvalidDevice"))
                }
            }

            override fun OnConnectTimeout(mac: String?) {
                Log.d("UPDATE", "connect OnConnectTimeout")
                viewModel.resultLiveData.postValue(Pair(Result.ERROR, "OnConnectTimeout"))
            }

            override fun OnDisconnect(mac: String?, status: Int) {
                Log.d("UPDATE", "connect OnDisconnect")
                viewModel.resultLiveData.postValue(Pair(Result.ERROR, "OnDisconnect $status"))
            }

        })
    }

    private fun getCurrentImageInfo(){
        Observable.create<String>(ObservableOnSubscribe<String?> {   emitter ->
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
                    viewModel.resultLiveData.postValue(Pair(Result.ERROR, e.message!!))
                }

                override fun onNext(t: String) {
                    Log.d("UPDATE", "currentImageInfo onNext $t")
                    viewModel.stateLiveData.postValue(t)
                }

                override fun onComplete() {
                    Log.d("UPDATE", "currentImageInfo onComplete")
                    viewModel.stateLiveData.postValue("currentImageInfo complete")
                    update()
                }
            })
    }

    private fun update(){
        if (isHexFile(targetFile!!)) {
            startHexFileUpdate(targetFile!!)
        } else {
            Log.d("UPDATE", "file not .hex")
            viewModel.resultLiveData.postValue(Pair(Result.ERROR, "file not .hex"))
        }
    }

    private fun startHexFileUpdate(targetFile : File) {
        Observable.create<String>(ObservableOnSubscribe<String?> { emitter ->
            var hexFileEraseAddr = 0
            if (currentImageInfo!!.chipType == ChipType.CH573 || currentImageInfo!!.chipType == ChipType.CH583) {
                hexFileEraseAddr = try {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        emitter.onError(Throwable("Error permissions"))
                        return@ObservableOnSubscribe
                    }
                    CH583OTAManager.getInstance().getHexFileEraseAddr(targetFile)
                } catch (e: CH583OTAException) {
                    emitter.onError(Throwable("Error erase ${e.message}"))
                    return@ObservableOnSubscribe
                }
            }
            CH583OTAManager.getInstance()
                .start(hexFileEraseAddr, targetFile, currentImageInfo!!, object : IProgress {
                    override fun onEraseStart() {
                        emitter.onNext("Erase start")
                    }

                    override fun onEraseFinish() {
                        emitter.onNext("Erase finish")
                    }

                    override fun onProgramStart() {
                        emitter.onNext("program start")
                    }

                    override fun onProgramProgress(current: Int, total: Int) {
                        emitter.onNext("program progress")
                        viewModel.progressLiveData.postValue("$current/$total")
                    }

                    override fun onProgramFinish() {
                        emitter.onNext("program finish")
                        viewModel.progressLiveData.postValue("")
                    }

                    override fun onVerifyStart() {
                        emitter.onNext("verify start")
                    }

                    override fun onVerifyProgress(current: Int, total: Int) {
                        emitter.onNext("verify progress")
                        viewModel.progressLiveData.postValue("$current/$total")
                    }

                    override fun onVerifyFinish() {
                        emitter.onNext("verify finish")
                        viewModel.progressLiveData.postValue("")
                    }

                    override fun onEnd() {
                        emitter.onComplete()
                    }

                    override fun onCancel() {
                        emitter.onError(Throwable("cancel"))
                    }

                    override fun onError(message: String) {
                        emitter.onError(Throwable(message))
                    }
                })
        }).subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<String?> {
                override fun onSubscribe(d: Disposable) {
                    Log.d("UPDATE", "onSubscribe")
                }

                override fun onNext(t: String) {
                    Log.d("UPDATE", "onNext $t")
                    viewModel.stateLiveData.postValue(t)
                }

                override fun onError(e: Throwable) {
                    Log.d("UPDATE", "onError ${e.message}")
                    viewModel.resultLiveData.postValue(Pair(Result.ERROR, e.message!!))
                }


                override fun onComplete() {
                    Log.d("UPDATE", "onComplete")
                    viewModel.resultLiveData.postValue(Pair(Result.COMPLETE, "onComplete"))
                }
            })
    }

    private fun isHexFile(file: File): Boolean {
        return (file.exists() && file.name.contains(".hex") || file.name.contains(".HEX"))
    }

    fun cancel() {
        CH583OTAManager.getInstance().cancel()
        if (CH583OTAManager.getInstance().isConnected(MainPref.deviceMac)){
            CH583OTAManager.getInstance().disconnect(MainPref.deviceMac, true)
        }
    }
}
package com.regula.documentreader

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.completions.IDocumentReaderCompletion
import com.regula.documentreader.api.completions.IDocumentReaderInitCompletion
import com.regula.documentreader.api.completions.rfid.IRfidReaderCompletion
import com.regula.documentreader.api.config.RecognizeConfig
import com.regula.documentreader.api.config.ScannerConfig
import com.regula.documentreader.api.enums.DocReaderAction
import com.regula.documentreader.api.enums.Scenario
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.params.DocReaderConfig
import com.regula.documentreader.api.results.DocumentReaderResults
import com.regula.documentreader.util.Utils
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.Executors

class MainViewModel(val documentReader: DocumentReader, val sharedPreferences: SharedPreferences): ViewModel() {

    private val useLivePortrait = false

    companion object {
        const val DATABASE_ID = "Full"
        const val DO_RFID = "doRfid"
    }

    @Scenario.Scenarios
    private var currentScenario = MutableLiveData<String>()

    val initLiveData = MutableLiveData<DocumentReaderException?>()
    val showScannerSuccessCompletion = MutableLiveData<DocumentReaderResults?>()
    val rfidCompletion = MutableLiveData<DocumentReaderResults?>()
    val showScannerErrorCompletion = MutableLiveData<DocumentReaderException?>()
    val showScannerCancelCompletion = MutableLiveData<DocumentReaderResults?>()
    val doRfidData = MutableLiveData<Boolean>()

    fun init(context: Context) {
        val license = Utils.getLicense(context) ?: return

        val config = DocReaderConfig(license)
        config.setLicenseUpdate(true)

        documentReader.initializeReader(context, config, IDocumentReaderInitCompletion { result: Boolean, error: DocumentReaderException? ->
            if (!result) { //Initialization was not successful
                initLiveData.value = error
                return@IDocumentReaderInitCompletion
            }
            setupCustomization(context)
            setupFunctionality(context)
            setupProcessParams(context)

            currentScenario.value = documentReader.availableScenarios[0].name
            doRfidData.value = sharedPreferences.getBoolean(DO_RFID, false);

            initLiveData.value = null
        })
    }

    fun recognizePdf(activity: Activity) {
        if (!documentReader.isReady) return

        Executors.newSingleThreadExecutor().execute {
            val `is`: InputStream
            var buffer: ByteArray? = null
            try {
                `is` = activity.assets.open("Regula/test.pdf")
                val size = `is`.available()
                buffer = ByteArray(size)
                `is`.read(buffer)
                `is`.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            val finalBuffer = buffer
            activity.runOnUiThread {
                finalBuffer?.let {
                    val recognizeConfig = RecognizeConfig.Builder(currentDocReaderScenario()).setData(it).build()
                    documentReader.recognize(recognizeConfig, completion)
                }
            }
        }
    }

    fun doRfid(checked: Boolean) {
        sharedPreferences.edit().putBoolean(DO_RFID, checked).apply()
    }

    fun scenario(scenario: String) {
        if (!documentReader.isReady) return

        currentScenario.value = scenario
    }

    private fun setupCustomization(context: Context) {
        documentReader.customization().edit().setShowHelpAnimation(false).apply()
    }

    private fun setupFunctionality(context: Context) {
        documentReader.functionality().edit()
            .setShowCameraSwitchButton(true)
            .apply()
    }

    private fun setupProcessParams(context: Context) {
        documentReader.processParams().multipageProcessing = true
    }

    fun showScanner(context: Context) {
        if (!documentReader.isReady) return

        val scannerConfig = ScannerConfig.Builder(currentDocReaderScenario()).build()
        documentReader.showScanner(context, scannerConfig, completion)
    }

    fun recognize(context: Context, bitmap: Bitmap) {
        val recognizeConfig = if (useLivePortrait) {
            val livePortrait = (AppCompatResources.getDrawable(
                context,
                R.drawable.live_portrait
            ) as BitmapDrawable).bitmap
            RecognizeConfig.Builder(currentDocReaderScenario())
                .setLivePortrait(livePortrait)
                .setBitmap(bitmap).build()
        } else
            RecognizeConfig.Builder(currentDocReaderScenario()).setBitmap(bitmap).build()
        documentReader.recognize(recognizeConfig, completion)
    }

    fun startRFIDReader(context: Context) {
        documentReader.startRFIDReader(context, object : IRfidReaderCompletion() {
                override fun onCompleted(
                    rfidAction: Int,
                    documentReaderResults: DocumentReaderResults?,
                    e: DocumentReaderException?
                ) {
                    rfidCompletion.value = documentReaderResults
                }
            })
    }

    private fun currentDocReaderScenario(): String {
        return currentScenario.value ?: ""
    }

    private val completion =
        IDocumentReaderCompletion { action, results, error ->
            //processing is finished, all results are ready
            if (action == DocReaderAction.COMPLETE || action == DocReaderAction.TIMEOUT) {
                showScannerSuccessCompletion.value = results
            } else {
                //something happened before all results were ready
                if (action == DocReaderAction.CANCEL) {
                    showScannerCancelCompletion.value = results
                    Log.d("ViewModel", "Scanning was cancelled")
                } else if (action == DocReaderAction.ERROR) {
                    showScannerErrorCompletion.value = error
                    Log.d("ViewModel", "Error:$error")
                }
            }
        }
}
package com.regula.documentreader

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.completions.IDocumentReaderCompletion
import com.regula.documentreader.api.completions.IDocumentReaderInitCompletion
import com.regula.documentreader.api.enums.DocReaderAction
import com.regula.documentreader.api.enums.eMDLDeviceEngagement
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.params.DocReaderConfig
import com.regula.documentreader.api.params.mdl.DataRetrieval
import com.regula.documentreader.api.results.DocumentReaderResults
import com.regula.documentreader.util.Utils

class MainViewModel(val documentReader: DocumentReader): ViewModel() {


    val initLiveData = MutableLiveData<DocumentReaderException?>()
    val showScannerSuccessCompletion = MutableLiveData<DocumentReaderResults?>()
    val showScannerErrorCompletion = MutableLiveData<DocumentReaderException?>()
    val showScannerCancelCompletion = MutableLiveData<DocumentReaderResults?>()

    fun init(context: Context) {
        val license = Utils.getLicense(context) ?: return

        val config = DocReaderConfig(license)
        config.setLicenseUpdate(true)

        documentReader.initializeReader(context, config, IDocumentReaderInitCompletion { result: Boolean, error: DocumentReaderException? ->
            if (!result) { //Initialization was not successful
                initLiveData.value = error
                return@IDocumentReaderInitCompletion
            }

            initLiveData.value = null
        })
    }

    fun readMDL(context: MainActivity, type: eMDLDeviceEngagement, dataRetrieval: DataRetrieval) {
        if (!documentReader.isReady) return

        documentReader.startReadMDL(context, type, dataRetrieval, completion)
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
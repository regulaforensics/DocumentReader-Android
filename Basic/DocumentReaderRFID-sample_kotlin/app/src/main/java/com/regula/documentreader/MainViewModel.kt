package com.regula.documentreader

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.completions.IDocumentReaderInitCompletion
import com.regula.documentreader.api.completions.rfid.IRfidReaderCompletion
import com.regula.documentreader.api.enums.Scenario
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.params.DocReaderConfig
import com.regula.documentreader.api.results.DocumentReaderResults
import com.regula.documentreader.util.Utils

class MainViewModel(val documentReader: DocumentReader): ViewModel() {
    val initLiveData = MutableLiveData<DocumentReaderException?>()
    val rfidCompletion = MutableLiveData<DocumentReaderResults?>()

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

    fun startRFIDReader(context: Context) {
        documentReader.startNewSession();

        documentReader.processParams().scenario = Scenario.SCENARIO_RFID

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
}
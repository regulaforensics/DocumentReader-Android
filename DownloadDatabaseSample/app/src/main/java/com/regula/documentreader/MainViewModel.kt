package com.regula.documentreader

import android.content.Context
import android.icu.text.DecimalFormat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.completions.ICheckDatabaseUpdate
import com.regula.documentreader.api.completions.IDocumentReaderInitCompletion
import com.regula.documentreader.api.completions.IDocumentReaderPrepareDbCompletion
import com.regula.documentreader.api.completions.model.PrepareProgress
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.params.DocReaderConfig
import com.regula.documentreader.util.Utils

class MainViewModel(private val documentReader: DocumentReader): ViewModel() {

    var progressData = MutableLiveData<String>()
    var prepareSuccess = MutableLiveData<Void>()
    var prepareFailed = MutableLiveData<String>()
    var lockUI = MutableLiveData<Boolean>()
    var dbInfo = MutableLiveData<String>()
    var initState = MutableLiveData<String>()

    private val DATABASE_ID = "Full"

    fun prepareDatabase(context: Context) {
        dbInfo.value = ""
        lockUI.value = false
        documentReader.prepareDatabase(context, DATABASE_ID, prepareCompletion)
    }

    fun removeDatabase(context: Context) {
        dbInfo.value = ""
        documentReader.removeDatabase(context)
    }

    fun updateDatabase(context: Context) {
        dbInfo.value = ""
        lockUI.value = false
        documentReader.checkDatabaseUpdate(context, DATABASE_ID, checkDbUpdateCompletion)
    }

    fun cancelDbUpdate(context: Context) {
        dbInfo.value = ""
        documentReader.cancelDBUpdate(context)
    }

    fun runAutoUpdate(context: Context) {
        dbInfo.value = ""
        lockUI.value = false
        documentReader.runAutoUpdate(context, DATABASE_ID, prepareCompletion)
    }

    fun initialize(context: Context) {
        val license = Utils.getLicense(context) ?: return

        lockUI.value = false
        val config = DocReaderConfig(license)
        documentReader.initializeReader(context, config, initCompletion)
    }

    fun deinitialize() {
        documentReader.deinitializeReader()
    }

    private val prepareCompletion = object: IDocumentReaderPrepareDbCompletion() {
        override fun onPrepareProgressChanged(prepareProgress: PrepareProgress) {
            val d: Double = prepareProgress.downloadedBytes.toDouble() / 1000000
            val t: Double = prepareProgress.totalBytes.toDouble() / 1000000
            val df = DecimalFormat("0.00")
            val progress = df.format(d) + "/" + df.format(t) + " - " + prepareProgress.progress + "%"
            progressData.postValue(progress)
        }

        override fun onPrepareCompleted(status: Boolean, error: DocumentReaderException?) {
            error?.let {
                prepareFailed.postValue(it.message)
            } ?: run {
                prepareSuccess.postValue(null)
            }
            lockUI.value = true
        }
    }

    private val checkDbUpdateCompletion = ICheckDatabaseUpdate { db ->
        lockUI.value = true
        db?.let {
            dbInfo.postValue("Version: ${db.version}\nDate: ${db.date}\nDescription: ${db.databaseDescription}\nSize: ${Utils.humanReadableByteCountSI(db.size!!)}")
        } ?: run {
            dbInfo.postValue("No db update")
        }
    }

    private val initCompletion =
        IDocumentReaderInitCompletion { status, error ->
            lockUI.value = true
            error?.let {
                initState.value = "Initialization failed: ${it.message}"
            } ?: run {
                if (status)
                    initState.value = "Initialization success"
            }
        }

    fun reset() {
        progressData = MutableLiveData<String>()
        prepareSuccess = MutableLiveData<Void>()
        prepareFailed = MutableLiveData<String>()
        initState = MutableLiveData<String>()
    }
}
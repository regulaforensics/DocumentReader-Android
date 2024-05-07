package com.regula.documentreader

import android.content.Context
import android.icu.text.DecimalFormat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.completions.ICheckDatabaseUpdate
import com.regula.documentreader.api.completions.IDocumentReaderInitCompletion
import com.regula.documentreader.api.completions.IDocumentReaderPrepareCompletion
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.params.DocReaderConfig
import com.regula.documentreader.util.Utils

class MainViewModel(private val documentReader: DocumentReader): ViewModel() {

    val progressData = MutableLiveData<String>()
    val prepareSuccess = MutableLiveData<Void>()
    val prepareFailed = MutableLiveData<String>()
    val lockUI = MutableLiveData<Boolean>()
    val dbInfo = MutableLiveData<String>()
    val initState = MutableLiveData<String>()

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

    private val prepareCompletion = object: IDocumentReaderPrepareCompletion {
        override fun onPrepareProgressChanged(prepareProgress: Int) {
            val progress = "prepareProgress%"
            progressData.postValue(progress)
        }

        override fun onPrepareCompleted(status: Boolean, error: DocumentReaderException?) {
            lockUI.value = true
            error?.let {
                prepareFailed.postValue(it.message)
            } ?: run {
                prepareSuccess.postValue(null)
            }
        }
    }

    private val checkDbUpdateCompletion = ICheckDatabaseUpdate { db ->
        lockUI.value = true
        db?.let {
            dbInfo.value = "Version: ${db.version}\nDate: ${db.date}\nDescription: ${db.databaseDescription}\nSize: ${Utils.humanReadableByteCountSI(db.size!!)}"
        } ?: run {
            dbInfo.value = "Issue during check db version"
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

}

package com.regula.documentreader.custom.rfid

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.regula.documentreader.R
import com.regula.documentreader.SharedDocReaderResults
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.enums.DocReaderAction
import com.regula.documentreader.api.enums.eRFID_DataFile_Type
import com.regula.documentreader.api.enums.eRFID_NotificationAndErrorCodes
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.nfc.IUniversalNfcTag
import com.regula.documentreader.api.results.DocumentReaderResults


/**
 * Created by Sergey Yakimchik on 15.09.21.
 * Copyright (c) 2021 Regula. All rights reserved.
 */
abstract class BaseCustomRfiActivity : AppCompatActivity() {
    protected var HANDLER: Handler = Handler(Looper.getMainLooper())
    var rfidStatus: TextView? = null
    private var currentRfidDgTv: TextView? = null
    private var currentDataGroupLt: LinearLayout? = null
    private var skipRfidBtn: ImageButton? = null
    var retryRunnable = Runnable { retry() }
    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_rfid)
        currentDataGroupLt = findViewById(R.id.currentDataGroupLt)
        currentRfidDgTv = findViewById(R.id.currentRfidDgTv)
        rfidStatus = findViewById(R.id.rfidStatus)
        skipRfidBtn = findViewById(R.id.skipRfidBtn)
        skipRfidBtn?.setOnClickListener { completeActivity() }
    }

    protected fun rfidProgress(code: Int, value: Int) {
        val hiword = code and -0x10000
        val loword = code and 0x0000FFFF
        when (hiword) {
            eRFID_NotificationAndErrorCodes.RFID_NOTIFICATION_PCSC_READING_DATAGROUP -> if (value == 0) {
                HANDLER.post {
                    currentRfidDgTv?.text = String.format(
                        getString(R.string.strReadingRFIDDG),
                        eRFID_DataFile_Type.getTranslation(applicationContext, loword)
                    )
                }
            }
        }
    }

    protected fun retry() {
        rfidStatus?.setText(R.string.strPlacePhoneOnDoc)
        rfidStatus?.setTextColor(resources.getColor(android.R.color.black))
    }

    fun skipReadRfid() {
        DocumentReader.Instance().stopRFIDReader(applicationContext)
        setResult(Activity.RESULT_CANCELED)
        completeActivity()
    }

    protected open fun completeActivity() {
        finish()
    }

    protected fun startRfidReading(tag: IUniversalNfcTag?) {
        Log.d(TAG, "Start read RFID")
        rfidStatus?.setText(R.string.strReadingRFID)
        rfidStatus?.setTextColor(resources.getColor(android.R.color.holo_orange_dark))
        DocumentReader.Instance().readRFID(
            tag
        ) { rfidAction: Int, documentReaderResults: DocumentReaderResults?, error: DocumentReaderException? ->
            if (rfidAction == DocReaderAction.COMPLETE) {
                // Completed rfid reading
                SharedDocReaderResults.documentReaderResults = documentReaderResults
                if (documentReaderResults?.rfidResult == 0x00000001) {
                    setResult(RESULT_OK)
                    rfidStatus?.text = this@BaseCustomRfiActivity.getString(R.string.RSDT_RFID_READING_FINISHED)
                    rfidStatus?.setTextColor(resources.getColor(android.R.color.holo_green_dark))
                    HANDLER.postDelayed({ completeActivity() }, 2500)
                } else {
                    val builder: String = getString(R.string.RFID_Error_Failed) +
                            "\n" +
                            documentReaderResults?.rfidResult?.let {
                                eRFID_NotificationAndErrorCodes.getTranslation(
                                    this@BaseCustomRfiActivity,
                                    it
                                )
                            }
                    rfidStatus?.text = builder
                    rfidStatus?.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                    HANDLER.postDelayed(
                        retryRunnable,
                        resources.getInteger(R.integer.reg_rfid_activity_error_timeout)
                            .toLong()
                    )
                }
                currentDataGroupLt?.visibility = View.GONE
            } else if (rfidAction == DocReaderAction.NOTIFICATION) {
                HANDLER.post {
                    if (currentDataGroupLt?.visibility == View.GONE) {
                        currentDataGroupLt?.visibility = View.VISIBLE
                        rfidStatus?.setTextColor(resources.getColor(android.R.color.holo_orange_light))
                    }
                }
                if (documentReaderResults?.documentReaderNotification != null)
                    rfidProgress(documentReaderResults.documentReaderNotification!!.code, documentReaderResults.documentReaderNotification!!.value
                )
            } else if (rfidAction == DocReaderAction.ERROR) {
                val builder: String = getString(R.string.RFID_Error_Failed) +
                        "\n" +
                        documentReaderResults?.let {
                            eRFID_NotificationAndErrorCodes.getTranslation(
                                this@BaseCustomRfiActivity,
                                it.rfidResult
                            )
                        }
                rfidStatus?.text = builder
                rfidStatus?.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                currentDataGroupLt?.visibility = View.GONE
                Log.e(TAG, "Error: $error")
            }
        }
    }

    companion object {
        private const val TAG = "BaseCustomRfiActivity"
    }
}
package com.regula.documentreader.custom.rfid

import android.app.Activity
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.regula.documentreader.R
import com.regula.documentreader.SharedDocReaderResults
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.completions.rfid.IRfidReaderCompletion
import com.regula.documentreader.api.enums.DocReaderAction
import com.regula.documentreader.api.enums.eRFID_DataFile_Type
import com.regula.documentreader.api.enums.eRFID_ErrorCodes
import com.regula.documentreader.api.enums.eRFID_NotificationCodes
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.nfc.IUniversalNfcTag
import com.regula.documentreader.api.results.DocumentReaderNotification
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
        applyEdgeToEdgeInsets()
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
            eRFID_NotificationCodes.RFID_NOTIFICATION_PCSC_READING_DATAGROUP -> if (value == 0) {
                HANDLER.post {
                    currentRfidDgTv?.text = String.format(
                        getString(com.regula.documentreader.api.R.string.strReadingRFIDDG),
                        eRFID_DataFile_Type.getTranslation(applicationContext, loword)
                    )
                }
            }
        }
    }

    protected fun retry() {
        rfidStatus?.setText(com.regula.documentreader.api.R.string.strPlacePhoneOnDoc)
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

    var completion: IRfidReaderCompletion = object : IRfidReaderCompletion() {
        override fun onProgress(notification: DocumentReaderNotification) {
            super.onProgress(notification)
            HANDLER.post(Runnable {
                if (currentDataGroupLt!!.getVisibility() == View.GONE) {
                    currentDataGroupLt!!.setVisibility(View.VISIBLE)
                    rfidStatus!!.setTextColor(getResources().getColor(android.R.color.holo_orange_light))
                }
            })
            if (notification != null) rfidProgress(notification.code, notification.value)
        }

        override fun onCompleted(
            rfidAction: Int,
            documentReaderResults: DocumentReaderResults?,
            error: DocumentReaderException?
        ) {
            if (rfidAction == DocReaderAction.COMPLETE) {
                // Completed rfid reading
                SharedDocReaderResults.documentReaderResults = documentReaderResults
                if (documentReaderResults!!.rfidResult == 0x00000001) {
                    setResult(RESULT_OK)
                    rfidStatus!!.setText(this@BaseCustomRfiActivity.getString(com.regula.documentreader.api.R.string.RSDT_RFID_READING_FINISHED))
                    rfidStatus!!.setTextColor(getResources().getColor(android.R.color.holo_green_dark))
                    HANDLER.postDelayed(Runnable { completeActivity() }, 2500)
                } else {
                    val builder =
                        getString(com.regula.documentreader.api.R.string.RFID_Error_Failed) +
                                "\n" +
                                eRFID_ErrorCodes.getTranslation(
                                    this@BaseCustomRfiActivity,
                                    documentReaderResults.rfidResult
                                )
                    rfidStatus!!.setText(builder)
                    rfidStatus!!.setTextColor(getResources().getColor(android.R.color.holo_red_dark))
                    HANDLER.postDelayed(
                        retryRunnable,
                        getResources().getInteger(com.regula.documentreader.api.R.integer.reg_rfid_activity_error_timeout)
                            .toLong()
                    )
                }
                currentDataGroupLt!!.setVisibility(View.GONE)
            } else if (rfidAction == DocReaderAction.ERROR) {
                val builder = getString(com.regula.documentreader.api.R.string.RFID_Error_Failed) +
                        "\n" +
                        eRFID_ErrorCodes.getTranslation(
                            this@BaseCustomRfiActivity,
                            documentReaderResults!!.rfidResult
                        )
                rfidStatus!!.setText(builder)
                rfidStatus!!.setTextColor(getResources().getColor(android.R.color.holo_red_dark))
                currentDataGroupLt!!.setVisibility(View.GONE)
                Log.e(TAG, "Error: " + error)
            }
        }
    }


    protected fun startRfidReading(tag: IsoDep?) {
        Log.d(TAG, "Start read RFID")
        tag?.let {
            rfidStatus!!.setText(com.regula.documentreader.api.R.string.strReadingRFID)
            rfidStatus!!.setTextColor(getResources().getColor(android.R.color.holo_orange_dark))
            DocumentReader.Instance().readRFID(it, completion)
        }
    }

    protected fun startRfidReading(tag: IUniversalNfcTag?) {
        Log.d(TAG, "Start read RFID")
        tag?.let {
            rfidStatus?.setText(com.regula.documentreader.api.R.string.strReadingRFID)
            rfidStatus?.setTextColor(resources.getColor(android.R.color.holo_orange_dark))
            DocumentReader.Instance().readRFID(it, completion);
        }
    }

    companion object {
        private const val TAG = "BaseCustomRfiActivity"
    }

    private fun applyEdgeToEdgeInsets() {
        val rootView = window.decorView.findViewWithTag<View>("content")
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
                val systemBars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            or WindowInsetsCompat.Type.displayCutout()
                )
                view.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    systemBars.bottom
                )
                insets
            }
        }
    }
}
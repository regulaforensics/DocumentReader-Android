package com.example.customrfid_kotlin

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.regula.common.utils.RegulaLog
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.enums.DocReaderAction
import com.regula.documentreader.api.enums.eRFID_DataFile_Type
import com.regula.documentreader.api.enums.eRFID_ErrorCodes
import com.regula.documentreader.api.enums.eRFID_NotificationCodes
import com.regula.documentreader.api.nfc.IsoDepTag


class CustomRfidActivity : AppCompatActivity() {
    private var HANDLER = Handler(Looper.getMainLooper())
    private var nfcAdapter: NfcAdapter? = null
    var rfidStatus: TextView? = null
    var currentRfidDgTv: TextView? = null
    var currentDataGroupLt: LinearLayout? = null
    var retryRunnable = Runnable { retry() }
    private val nfcActionReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action != null && action == NfcAdapter.ACTION_ADAPTER_STATE_CHANGED) {
                checkAdapterEnabled()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_rfid)
        currentDataGroupLt = findViewById(R.id.currentDataGroupLt)
        currentRfidDgTv = findViewById(R.id.currentRfidDgTv)
        rfidStatus = findViewById(R.id.rfidStatus)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        checkAdapterEnabled()

        if (nfcAdapter == null) return

        try {
            val filter = IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
            this.registerReceiver(nfcActionReceiver, filter)
        } catch (ex: Exception) {
            //android broadcast adding error
        }
    }

    override fun onPause() {
        super.onPause()
        if (nfcAdapter == null) return

        stopForegroundDispatch(this@CustomRfidActivity, nfcAdapter)
        try {
            unregisterReceiver(nfcActionReceiver)
        } catch (ex: Exception) {
            //android not providing any api to check if is registered
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent")
        val action = intent.action
        if (action != null && action == NfcAdapter.ACTION_TECH_DISCOVERED) {
            handleNFCIntent(intent)
        }
    }

    private fun checkAdapterEnabled() {
        if (nfcAdapter == null) return
        if (nfcAdapter!!.isEnabled && DocumentReader.Instance().isReady) {
            setupForegroundDispatch(this@CustomRfidActivity, nfcAdapter!!)
        } else {
            Log.e(TAG, "NFC is not enabled")
        }
    }

    private fun setupForegroundDispatch(activity: Activity, adapter: NfcAdapter) {
        var flag = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flag = PendingIntent.FLAG_MUTABLE
        }

        val intent = Intent(activity.applicationContext, activity.javaClass)
        val pendingIntent = PendingIntent.getActivity(activity.applicationContext, 0, intent, flag)
        val filters = arrayOfNulls<IntentFilter>(1)

        // Notice that this is the same filter as in our manifest.
        filters[0] = IntentFilter()
        filters[0]!!.addAction(NfcAdapter.ACTION_TECH_DISCOVERED)
        filters[0]!!.addCategory(Intent.CATEGORY_DEFAULT)
        val techList = arrayOf(arrayOf("android.nfc.tech.IsoDep"))
        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList)
        Log.d(TAG, "NFC adapter dispatch enabled for android.nfc.tech.IsoDep")
    }

    private fun stopForegroundDispatch(activity: Activity, adapter: NfcAdapter?) {
        if (adapter == null) return

        adapter.disableForegroundDispatch(activity)
        Log.d(TAG, "NFC adapter dispatch disabled")
    }

    private fun handleNFCIntent(intent: Intent) {
        Log.d(TAG, "Intent received: NfcAdapter.ACTION_TECH_DISCOVERED")
        HANDLER.removeCallbacks(retryRunnable)
        val action = intent.action
        if (action == null || action != NfcAdapter.ACTION_TECH_DISCOVERED) {
            return
        }
        Log.d(TAG, "Chip reading started!")
        val nfcTag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        Log.d(TAG, "nfcTag extracted from NfcAdapter.EXTRA_TAG")
        if (nfcTag == null) {
            Log.e(TAG, "NFC tag is null")
            return
        }

        val isoDepTag = IsoDep.get(nfcTag)
        RegulaLog.d("IsoDep.get(nfcTag) successful")
        if (isoDepTag == null) return

        Log.d(TAG, "Start read RFID")
        rfidStatus!!.setText(com.regula.documentreader.api.R.string.strReadingRFID)
        rfidStatus!!.setTextColor(ContextCompat.getColor(applicationContext, android.R.color.holo_orange_dark))
        DocumentReader.Instance().readRFID(
            IsoDepTag(isoDepTag)
        ) { rfidAction, documentReaderResults, error ->
            when (rfidAction) {
                DocReaderAction.COMPLETE -> {
                    // Completed rfid reading
                    MainActivity.documentReaderResults = documentReaderResults
                    if (documentReaderResults!!.rfidResult == eRFID_ErrorCodes.RFID_ERROR_NO_ERROR) {
                        setResult(RESULT_OK)
                        rfidStatus!!.text =
                            this@CustomRfidActivity.getString(com.regula.documentreader.api.R.string.RSDT_RFID_READING_FINISHED)
                        rfidStatus!!.setTextColor(ContextCompat.getColor(applicationContext, android.R.color.holo_green_dark))
                        HANDLER.postDelayed({ finish() }, 2500)
                    } else {
                        val builder =
                            """
                                    ${getString(com.regula.documentreader.api.R.string.RFID_Error_Failed)}
                                    ${eRFID_ErrorCodes.getTranslation(this@CustomRfidActivity, documentReaderResults.rfidResult)}
                               """.trimIndent()
                        rfidStatus!!.text = builder
                        rfidStatus!!.setTextColor(ContextCompat.getColor(applicationContext, android.R.color.holo_red_dark))
                        HANDLER.postDelayed(
                            retryRunnable,
                            resources.getInteger(com.regula.documentreader.api.R.integer.reg_rfid_activity_error_timeout)
                                .toLong()
                        )
                    }
                    currentDataGroupLt!!.visibility = View.GONE
                }
                DocReaderAction.NOTIFICATION -> {
                    HANDLER.post {
                        if (currentDataGroupLt!!.visibility == View.GONE) {
                            currentDataGroupLt!!.visibility = View.VISIBLE
                            rfidStatus!!.setTextColor(ContextCompat.getColor(applicationContext, android.R.color.holo_orange_light))
                        }
                    }
                    if (documentReaderResults!!.documentReaderNotification != null)
                        rfidProgress(
                            documentReaderResults.documentReaderNotification!!.code,
                            documentReaderResults.documentReaderNotification!!.value
                        )
                }
                DocReaderAction.ERROR -> {
                    val builder = """
             ${getString(com.regula.documentreader.api.R.string.RFID_Error_Failed)}

             """.trimIndent() +
                            eRFID_ErrorCodes.getTranslation(
                                this@CustomRfidActivity,
                                documentReaderResults!!.rfidResult
                            )
                    rfidStatus!!.text = builder
                    rfidStatus!!.setTextColor(ContextCompat.getColor(applicationContext, android.R.color.holo_red_dark))
                    currentDataGroupLt!!.visibility = View.GONE
                    Log.e(TAG, "Error: $error")
                }
            }
        }
    }

    fun rfidProgress(code: Int, value: Int) {
        val hiword = code and -0x10000
        val loword = code and 0x0000FFFF
        when (hiword) {
            eRFID_NotificationCodes.RFID_NOTIFICATION_PCSC_READING_DATAGROUP -> if (value == 0) {
                HANDLER.post {
                    currentRfidDgTv!!.text = String.format(
                        getString(com.regula.documentreader.api.R.string.strReadingRFIDDG),
                        eRFID_DataFile_Type.getTranslation(
                            applicationContext, loword
                        )
                    )
                }
            }
        }
    }

    fun retry() {
        rfidStatus!!.setText(com.regula.documentreader.api.R.string.strPlacePhoneOnDoc)
        rfidStatus!!.setTextColor(ContextCompat.getColor(applicationContext, android.R.color.black))
    }

    fun skipReadRfid(view: View?) {
        DocumentReader.Instance().stopRFIDReader(applicationContext)
        setResult(RESULT_CANCELED)
        finish()
    }

    companion object {
        private const val TAG = "CustomRfidActivity"
    }
}
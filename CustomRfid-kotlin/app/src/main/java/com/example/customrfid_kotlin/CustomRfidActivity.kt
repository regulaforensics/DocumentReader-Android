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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.customrfid_kotlin.databinding.ActivityCustomRfidBinding
import com.regula.common.utils.RegulaLog
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.completions.rfid.IRfidReaderCompletion
import com.regula.documentreader.api.enums.DocReaderAction
import com.regula.documentreader.api.enums.eRFID_DataFile_Type
import com.regula.documentreader.api.enums.eRFID_ErrorCodes
import com.regula.documentreader.api.enums.eRFID_NotificationCodes
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.nfc.IsoDepTag
import com.regula.documentreader.api.results.DocumentReaderNotification
import com.regula.documentreader.api.results.DocumentReaderResults

class CustomRfidActivity : AppCompatActivity() {
    private var HANDLER = Handler(Looper.getMainLooper())
    private var nfcAdapter: NfcAdapter? = null

    private var isRestartedReading = false

    private lateinit var binding: ActivityCustomRfidBinding

    var retryRunnable = Runnable { retry() }
    private val nfcActionReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            intent.action.let {
                if (it == NfcAdapter.ACTION_ADAPTER_STATE_CHANGED) {
                    checkAdapterEnabled()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomRfidBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        nfcAdapter ?: finish()
    }

    override fun onResume() {
        super.onResume()
        checkAdapterEnabled()

        nfcAdapter.let {
            try {
                val filter = IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
                this.registerReceiver(nfcActionReceiver, filter)
            } catch (ex: Exception) {
                //android broadcast adding error
            }
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter.let {
            stopForegroundDispatch(this@CustomRfidActivity, it)
            try {
                unregisterReceiver(nfcActionReceiver)
            } catch (ex: Exception) {
                //android not providing any api to check if is registered
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent")
        intent.action?.let {
            if (it == NfcAdapter.ACTION_TECH_DISCOVERED) {
                handleNFCIntent(intent)
            }
        }
    }

    private fun checkAdapterEnabled() {
        nfcAdapter?.let {
            if (it.isEnabled && DocumentReader.Instance().isReady) {
                setupForegroundDispatch(this@CustomRfidActivity, it)
            } else {
                Log.e(TAG, "NFC is not enabled")
            }
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
        val intentFilter = IntentFilter()
        intentFilter.addAction(NfcAdapter.ACTION_TECH_DISCOVERED)
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT)

        filters[0] = intentFilter
        val techList = arrayOf(arrayOf("android.nfc.tech.IsoDep"))
        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList)
        Log.d(TAG, "NFC adapter dispatch enabled for android.nfc.tech.IsoDep")
    }

    private fun stopForegroundDispatch(activity: Activity, adapter: NfcAdapter?) {
        adapter?.let {
            it.disableForegroundDispatch(activity)
            Log.d(TAG, "NFC adapter dispatch disabled")
        }
    }

    private fun handleNFCIntent(intent: Intent) {
        Log.d(TAG, "Intent received: NfcAdapter.ACTION_TECH_DISCOVERED")
        HANDLER.removeCallbacks(retryRunnable)
        val action = intent.action ?: return
        if (action != NfcAdapter.ACTION_TECH_DISCOVERED)
            return

        Log.d(TAG, "Chip reading started!")
        val nfcTag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: run {
            Log.e(TAG, "NFC tag is null")
            return
        }
        Log.d(TAG, "nfcTag extracted from NfcAdapter.EXTRA_TAG")

        val isoDepTag = IsoDep.get(nfcTag) ?: return
        RegulaLog.d("IsoDep.get(nfcTag) successful")

        if (isRestartedReading) {
            Log.d(TAG,  "Restart read RFID")
        } else {
            Log.d(TAG, "Start read RFID")
        }

        binding.rfidStatus.setText(com.regula.documentreader.api.R.string.strReadingRFID)
        binding.rfidStatus.setTextColor(ContextCompat.getColor(applicationContext, android.R.color.holo_orange_dark))
        DocumentReader.Instance().readRFID(IsoDepTag(isoDepTag), object : IRfidReaderCompletion() {
            override fun onProgress(notification: DocumentReaderNotification) {
                HANDLER.post {
                    if (binding.currentDataGroupLt.visibility == View.GONE) {
                        binding.currentDataGroupLt.visibility = View.VISIBLE
                        binding.rfidStatus.setTextColor(ContextCompat.getColor(applicationContext, android.R.color.holo_orange_light))
                    }
                }
                rfidProgress(notification.code, notification.value)
            }

            override fun onCompleted(
                rfidAction: Int,
                documentReaderResults: DocumentReaderResults?,
                error: DocumentReaderException?
            ) {
                when (rfidAction) {
                    DocReaderAction.COMPLETE -> {
                        // Completed rfid reading
                        MainActivity.documentReaderResults = documentReaderResults
                        if (documentReaderResults?.rfidResult == eRFID_ErrorCodes.RFID_ERROR_NO_ERROR) {
                            setResult(RESULT_OK)
                            binding.rfidStatus.text =
                                this@CustomRfidActivity.getString(com.regula.documentreader.api.R.string.RSDT_RFID_READING_FINISHED)
                            binding.rfidStatus.setTextColor(ContextCompat.getColor(applicationContext, android.R.color.holo_green_dark))
                            HANDLER.postDelayed({ finish() }, 2500)
                        } else {
                            val builder =
                                """
                                    ${getString(com.regula.documentreader.api.R.string.RFID_Error_Failed)}
                                    ${eRFID_ErrorCodes.getTranslation(this@CustomRfidActivity, documentReaderResults?.rfidResult ?: 0)}
                               """.trimIndent()
                            binding.rfidStatus.text = builder
                            binding.rfidStatus.setTextColor(ContextCompat.getColor(applicationContext, android.R.color.holo_red_dark))
                            HANDLER.postDelayed(
                                {
                                    documentReaderResults?.let {
                                        if (isRestartRfidErrorCode(it.rfidResult)) {
                                            retry()
                                        } else {
                                            Log.e(TAG, "Error result: ${eRFID_ErrorCodes.getTranslation(this@CustomRfidActivity, documentReaderResults?.rfidResult ?: 0)}")
                                            setResult(RESULT_CANCELED)
                                            finish()
                                        }
                                    }
                                },
                                resources.getInteger(com.regula.documentreader.api.R.integer.reg_rfid_activity_error_timeout).toLong()
                            )
                        }
                        binding.currentDataGroupLt.visibility = View.GONE
                    }
                    DocReaderAction.ERROR -> {
                        val builder = """${getString(com.regula.documentreader.api.R.string.RFID_Error_Failed)}
                                """.trimIndent() + eRFID_ErrorCodes.getTranslation(this@CustomRfidActivity, documentReaderResults?.rfidResult ?: 0
                        )
                        binding.rfidStatus.text = builder
                        binding.rfidStatus.setTextColor(ContextCompat.getColor(applicationContext, android.R.color.holo_red_dark))
                        binding.currentDataGroupLt.visibility = View.GONE
                        Log.e(TAG, "Error: $error")
                    }
                }
            }

        })
    }

    fun rfidProgress(code: Int, value: Int) {
        val hiword = code and -0x10000
        val loword = code and 0x0000FFFF
        when (hiword) {
            eRFID_NotificationCodes.RFID_NOTIFICATION_PCSC_READING_DATAGROUP -> if (value == 0) {
                HANDLER.post {
                    binding.currentRfidDgTv.text = String.format(
                        getString(com.regula.documentreader.api.R.string.strReadingRFIDDG),
                        eRFID_DataFile_Type.getTranslation(
                            applicationContext, loword
                        )
                    )
                }
            }
        }
    }

    private fun retry() {
        isRestartedReading = true

        Log.d(TAG, "Try read again")

        binding.rfidStatus.setText(com.regula.documentreader.api.R.string.strPlacePhoneOnDoc)
        binding.rfidStatus.setTextColor(ContextCompat.getColor(applicationContext, android.R.color.black))
    }

    fun skipReadRfid(view: View?) {
        DocumentReader.Instance().stopRFIDReader(applicationContext)
        setResult(RESULT_CANCELED)
        finish()
    }

    fun isRestartRfidErrorCode(@eRFID_ErrorCodes.ErrorCodes errorCode: Int): Boolean {
        return errorCode != 0x00000001 && errorCode != -0x7a000000
                && errorCode != -0x7ffdfffc
                && (errorCode == -0x7ffe0000 || errorCode == -0x7fff0000 || errorCode == -0x7ffe0000
                        || errorCode == -0x7a000000 || errorCode == -0x7cf90000 || errorCode == -0x80000000)
    }

    companion object {
        private const val TAG = "CustomRfidActivity"
    }
}
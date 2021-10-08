package com.regula.documentreader

import android.app.Activity
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.enums.DocReaderAction
import com.regula.documentreader.api.enums.eRFID_DataFile_Type
import com.regula.documentreader.api.enums.eRFID_NotificationAndErrorCodes
import com.regula.documentreader.databinding.ActivityCustomRfidBinding


class CustomRfidActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCustomRfidBinding
    private var handler = Handler(Looper.getMainLooper())
    private lateinit var nfcAdapter: NfcAdapter
    private var retryRunnable = Runnable { retry() }
    private val nfcActionReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action
            if (action != null && action == NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
                checkAdapterEnabled()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomRfidBinding.inflate(layoutInflater)
        setContentView(binding.root)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        binding.skipRfidBtn.setOnClickListener {
            DocumentReader.Instance().stopRFIDReader(this)
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        checkAdapterEnabled()
        try {
            val filter = IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
            this.registerReceiver(nfcActionReceiver, filter)
        } catch (ex: Exception) {
            //android broadcast adding error
        }
    }

    override fun onPause() {
        super.onPause()
        stopForegroundDispatch(this, nfcAdapter)
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
        if (action != null && action == NfcAdapter.ACTION_TECH_DISCOVERED)
            handleNFCIntent(intent)
    }

    private fun checkAdapterEnabled() {
        if (nfcAdapter.isEnabled && DocumentReader.Instance().isReady)
            setupForegroundDispatch(this, nfcAdapter)
        else
            Log.e(TAG, "NFC is not enabled")
    }

    /**
     * @param activity The corresponding [Activity] requesting the foreground dispatch.
     * @param adapter  The [NfcAdapter] used for the foreground dispatch.
     */
    private fun setupForegroundDispatch(activity: Activity, adapter: NfcAdapter) {
        val intent = Intent(activity.applicationContext, activity.javaClass)
        val pendingIntent = PendingIntent.getActivity(activity.applicationContext, 0, intent, FLAG_IMMUTABLE)
        val filters = arrayOfNulls<IntentFilter>(1)

        // Notice that this is the same filter as in our manifest.
        filters[0] = IntentFilter()
        filters[0]!!.addAction(NfcAdapter.ACTION_TECH_DISCOVERED)
        filters[0]!!.addCategory(Intent.CATEGORY_DEFAULT)
        val techList = arrayOf(arrayOf("android.nfc.tech.IsoDep"))
        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList)
        Log.d(TAG, "NFC adapter dispatch enabled for android.nfc.tech.IsoDep")
    }

    /**
     * @param activity The corresponding [Activity] requesting to stop the foreground dispatch.
     * @param adapter  The [NfcAdapter] used for the foreground dispatch.
     */
    private fun stopForegroundDispatch(activity: Activity, adapter: NfcAdapter?) {
        adapter?.let {
            it.disableForegroundDispatch(activity)
            Log.d(TAG, "NFC adapter dispatch disabled")
        }
    }

    private fun handleNFCIntent(intent: Intent) {
        Log.d(TAG, "Intent received: NfcAdapter.ACTION_TECH_DISCOVERED")
        handler.removeCallbacks(retryRunnable)
        if (intent.action == null || intent.action != NfcAdapter.ACTION_TECH_DISCOVERED)
            return
        Log.d(TAG, "Chip reading started!")
        val nfcTag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)!!
        Log.d(TAG, "nfcTag extracted from NfcAdapter.EXTRA_TAG")
        val isoDepTag = IsoDep.get(nfcTag)
        Log.d(TAG, "IsoDep.get(nfcTag) successful")
        if (isoDepTag == null)
            return
        Log.d(TAG, "Start read RFID")
        binding.rfidStatus.setText(R.string.strReadingRFID)
        binding.rfidStatus.setTextColor(getColor(R.color.orange))
        DocumentReader.Instance().readRFID(isoDepTag) { rfidAction, results, error ->
            when (rfidAction) {
                DocReaderAction.COMPLETE -> {
                    MainActivity.results = results!!
                    if (results.rfidResult == 0x00000001) {
                        setResult(RESULT_OK)
                        binding.rfidStatus.text =
                            this.getString(R.string.RSDT_RFID_READING_FINISHED)
                        binding.rfidStatus.setTextColor(getColor(R.color.reg_green_ok))
                        handler.postDelayed({ finish() }, 2500)
                    } else {
                        val builder =
                            getString(R.string.RFID_Error_Failed) + "\n" + eRFID_NotificationAndErrorCodes.getTranslation(
                                this,
                                results.rfidResult
                            )
                        binding.rfidStatus.text = builder
                        binding.rfidStatus.setTextColor(getColor(R.color.reg_red_fail))
                        handler.postDelayed(
                            retryRunnable,
                            resources.getInteger(R.integer.reg_rfid_activity_error_timeout).toLong()
                        )
                    }
                    binding.currentDataGroupLt.visibility = View.GONE
                }
                DocReaderAction.NOTIFICATION -> {
                    handler.post {
                        if (binding.currentDataGroupLt.visibility == View.GONE) {
                            binding.currentDataGroupLt.visibility = View.VISIBLE
                            binding.rfidStatus.setTextColor(getColor(android.R.color.holo_orange_light))
                        }
                    }
                    rfidProgress(
                        results!!.documentReaderNotification!!.code,
                        results.documentReaderNotification!!.value
                    )
                }
                DocReaderAction.ERROR -> {
                    val builder =
                        getString(R.string.RFID_Error_Failed) + "\n" + eRFID_NotificationAndErrorCodes.getTranslation(
                            this,
                            results!!.rfidResult
                        )
                    binding.rfidStatus.text = builder
                    binding.rfidStatus.setTextColor(getColor(R.color.reg_red_fail))
                    binding.currentDataGroupLt.visibility = View.GONE
                    Log.e(TAG, "Error: $error")
                }
            }
        }
    }

    private fun codeToType(code: Int): Int = code and 0x10000

    private fun rfidProgress(code: Int, value: Int) {
        when (code and -0x10000) {
            eRFID_NotificationAndErrorCodes.RFID_NOTIFICATION_PCSC_READING_DATAGROUP -> if (value == 0) {
                handler.post {
                    binding.currentRfidDgTv.text = String.format(
                        getString(R.string.strReadingRFIDDG), eRFID_DataFile_Type.getTranslation(
                            applicationContext, codeToType(code)
                        )
                    )
                }
            }
        }
    }

    private fun retry() {
        binding.rfidStatus.setText(R.string.strPlacePhoneOnDoc)
//        binding.rfidStatus.setTextColor(themeColor(R.attr.colorOnSecondary))
    }

    companion object {
        private const val TAG = "CustomRfidActivity"
    }
}
package com.regula.documentreader.custom.rfid

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.util.Log
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.nfc.IsoDepUniversalWrapper
import com.regula.documentreader.custom.rfid.CustomRfidActivity

class CustomRfidActivity : BaseCustomRfiActivity() {
    private var nfcAdapter: NfcAdapter? = null
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
        if (nfcAdapter!!.isEnabled && DocumentReader.Instance().documentReaderIsReady) {
            setupForegroundDispatch(this@CustomRfidActivity, nfcAdapter!!)
        } else {
            Log.e(TAG, "NFC is not enabled")
        }
    }

    /**
     * @param activity The corresponding [Activity] requesting the foreground dispatch.
     * @param adapter  The [NfcAdapter] used for the foreground dispatch.
     */
    @SuppressLint("MissingPermission")
    private fun setupForegroundDispatch(activity: Activity, adapter: NfcAdapter) {
        val intent = Intent(activity.applicationContext, activity.javaClass)
        val pendingIntent = PendingIntent.getActivity(activity.applicationContext, 0, intent, 0)
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
    @SuppressLint("MissingPermission")
    private fun stopForegroundDispatch(activity: Activity, adapter: NfcAdapter?) {
        if (adapter != null) {
            adapter.disableForegroundDispatch(activity)
            Log.d(TAG, "NFC adapter dispatch disabled")
        }
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
        Log.d(TAG, "IsoDep.get(nfcTag) successful")
        if (isoDepTag == null) {
            return
        }
        startRfidReading(IsoDepUniversalWrapper(isoDepTag))
    }

    companion object {
        private const val TAG = "CustomRfidActivity"
    }
}
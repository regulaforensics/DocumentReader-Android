package com.regula.documentreader.custom.rfid

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.acs.smartcardio.BluetoothSmartCard
import com.acs.smartcardio.BluetoothTerminalManager
import com.acs.smartcardio.TerminalTimeouts
import com.regula.documentreader.R
import com.regula.documentreader.acs.CardStateMonitor
import com.regula.documentreader.acs.NfcDeviceTag
import javax.smartcardio.Card
import javax.smartcardio.CardChannel
import javax.smartcardio.CardException
import javax.smartcardio.CardTerminal


class AcsDeviceCustomRfidActivity : BaseCustomRfiActivity() {
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var manager: BluetoothTerminalManager? = null
    private var mCardStateMonitor: CardStateMonitor? = null
    private var mTerminal: CardTerminal? = null
    private var nfcTag: NfcDeviceTag? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
         * Use this check to determine whether BLE is supported on the device.  Then you can
         * selectively disable BLE-related features.
         */if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(
                this, R.string.error_bluetooth_le_not_supported,
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        /*
         * Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
         * BluetoothAdapter through BluetoothManager.
         */
        val bluetoothManager: BluetoothManager =
            getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter

        /* Checks if Bluetooth is supported on the device. */if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        initBle()
    }

    override fun onResume() {
        super.onResume()

        /*
         * Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
         * fire an intent to display a dialog asking the user to grant permission to enable it.
         */when {
             mBluetoothAdapter?.isEnabled != true -> {
                 val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                 startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
             }
         }
        mCardStateMonitor?.resume()
    }

    override fun onPause() {
        super.onPause()
        mCardStateMonitor?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mCardStateMonitor?.removeOnStateChangeListener()
    }

    private fun initBle() {
        /* Request access fine location permission. */
        //                    mLogger.logMsg(terminal.getName() + ": removed");
        when {
            ContextCompat.checkSelfPermission(
                this@AcsDeviceCustomRfidActivity,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            -> {
                ActivityCompat.requestPermissions(this@AcsDeviceCustomRfidActivity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_ACCESS_FINE_LOCATION)
                return
            }

            /* Start the scan. */

            /* Initialize the card state monitor. */
            else -> {
                manager = BluetoothSmartCard.getInstance(this).manager

                /* Start the scan. */
                manager?.startScan(1) { terminal ->
                    if (mCardStateMonitor?.isContainsTerminal(terminal) == true) return@startScan
                    val timeouts: TerminalTimeouts? = manager?.getTimeouts(terminal)
                    timeouts?.connectionTimeout = TerminalTimeouts.DEFAULT_TIMEOUT
                    timeouts?.powerTimeout = TerminalTimeouts.DEFAULT_TIMEOUT
                    timeouts?.protocolTimeout = TerminalTimeouts.DEFAULT_TIMEOUT
                    timeouts?.apduTimeout = TerminalTimeouts.DEFAULT_TIMEOUT
                    timeouts?.controlTimeout = TerminalTimeouts.DEFAULT_TIMEOUT
                    mCardStateMonitor?.addTerminal(terminal)
                    mTerminal = terminal
                }

                /* Initialize the card state monitor. */mCardStateMonitor =
                    CardStateMonitor.getInstance()
                mCardStateMonitor?.setOnStateChangeListener { _: CardStateMonitor?, terminal: CardTerminal, prevState: Int, currState: Int ->
                    if (CardStateMonitor.CARD_STATE_ABSENT in currState until prevState
                    ) {
                        Log.d(TAG, terminal.getName().toString() + ": removed")
                        if (nfcTag != null) {
                            nfcTag?.cardAbsent()
                            nfcTag = null
                        }
                        //                    mLogger.logMsg(terminal.getName() + ": removed");
                    } else if (CardStateMonitor.CARD_STATE_ABSENT in prevState until currState
                    ) {
                        Log.d(TAG, terminal.getName().toString() + ": inserted")
                        Thread {
                            var channel: CardChannel? = null
                            try {
                                val card: Card = terminal.connect("*")
                                channel = card.getBasicChannel()
                            } catch (e: CardException) {
                                e.printStackTrace()
                            }
                            nfcTag = NfcDeviceTag(channel)
                            runOnUiThread { handleNFCIntent() }
                        }.start()
                    }
                }
            }
        }

    }

    private fun disconnectDevice() {
        manager?.stopScan()
        Thread {
            /* Remove the terminal from card state monitor. */
            mCardStateMonitor?.removeTerminal(mTerminal)

            /* Disconnect from the terminal. */
            if (mTerminal != null)
                manager?.disconnect(mTerminal)
        }.start()
    }

    private fun handleNFCIntent() {
        if (nfcTag == null) return
        HANDLER.removeCallbacks(retryRunnable)
        Log.d(TAG, "Chip reading started!")
        Log.d(TAG, "Start read RFID")
        rfidStatus?.setText(R.string.strReadingRFID)
        rfidStatus?.setTextColor(resources.getColor(android.R.color.holo_orange_dark))
        startRfidReading(nfcTag)
    }

    override fun completeActivity() {
        disconnectDevice()
        super.completeActivity()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        /* User chose not to enable Bluetooth. */
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                finish()
                return
            }
            initBle()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_ACCESS_FINE_LOCATION) {
            if (grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                initBle()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    companion object {
        private const val REQUEST_ENABLE_BT = 1
        private const val REQUEST_ACCESS_FINE_LOCATION = 2
        private const val TAG = "CustomRfidActivity"
    }
}
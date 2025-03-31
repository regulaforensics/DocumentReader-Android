package com.regula.documentreader

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.regula.common.ble.BLEWrapper
import com.regula.common.ble.BleWrapperCallback
import com.regula.common.ble.RegulaBleService
import com.regula.common.ble.callback.BleManagerCallback
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.completions.IDocumentReaderInitCompletion
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.params.BleDeviceConfig
import com.regula.documentreader.util.BluetoothUtil

class DeviceActivity : AppCompatActivity() {
    private var loadingDialog: AlertDialog? = null
    private var bleManager: BLEWrapper? = null
    private var isBleServiceConnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device)
        val etDeviceName: EditText = findViewById(R.id.ed_device)
        val btnConnect: Button = findViewById(R.id.btn_connect)

        etDeviceName.setText(DocumentReader.Instance().functionality().btDeviceName)
        btnConnect.setOnClickListener { view: View? ->
            if (etDeviceName.text != null) {
                showDialog("Searching devices")
                handler.sendEmptyMessageDelayed(0, 7000)
                DocumentReader.Instance().functionality().edit()
                    .setBtDeviceName(etDeviceName.text.toString()).apply()
                startBluetoothService()
            }
        }
    }

    private fun startBluetoothService() {
        if (isBleServiceConnected && bleManager != null && bleManager!!.isConnected && !DocumentReader.Instance().isReady) {
            initializeReader()
            return
        }
        if (!BluetoothUtil.isPermissionsGranted(this) || isBleServiceConnected) {
            return
        }
        val bleIntent = Intent(this, RegulaBleService::class.java)
        startService(bleIntent)
        bindService(bleIntent, mBleConnection, 0)
    }

    private val mBleConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            showDialog("Searching devices")
            isBleServiceConnected = true
            val bleService = (service as RegulaBleService.LocalBinder).service
            bleManager = bleService.bleManager
            bleManager.let {
                if (it?.isConnected == true) {
                    initializeReader()
                    return
                }
            }

            handler.sendEmptyMessageDelayed(0, 7000)
            bleManager.let {
                it!!.addCallback(bleManagerCallbacks)
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBleServiceConnected = false
        }
    }
    private val handler = Handler { msg: Message? ->
        Toast.makeText(this, "Failed to connect to the torch device", Toast.LENGTH_SHORT).show()
        dismissDialog()
        false
    }
    private val bleManagerCallbacks: BleManagerCallback = object : BleWrapperCallback() {
        override fun onDeviceReady() {
            handler.removeMessages(0)
            bleManager!!.removeCallback(this)
            initializeReader()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBleServiceConnected) {
            unbindService(mBleConnection)
            isBleServiceConnected = false
        }
    }

    private fun dismissDialog() {
        if (loadingDialog != null) {
            loadingDialog!!.dismiss()
        }
    }

    private fun showDialog(msg: String?) {
        dismissDialog()
        val builderDialog = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.simple_dialog, null)
        builderDialog.setTitle(msg)
        builderDialog.setView(dialogView)
        builderDialog.setCancelable(false)
        loadingDialog = builderDialog.show()
    }

    private fun initializeReader() {
        showDialog("Initializing")
        if (bleManager != null) {
            DocumentReader.Instance()
                .initializeReader(this@DeviceActivity, BleDeviceConfig(bleManager!!), initCompletion)
        } else {
            dismissDialog()
            Toast.makeText(this, "Error reading license from device", Toast.LENGTH_LONG).show()
        }
    }

    private val initCompletion =
        IDocumentReaderInitCompletion { result: Boolean, error: DocumentReaderException? ->
            dismissDialog()
            if (!result) { //Initialization was not successful
                Toast.makeText(this, "Init failed: ${error?.message}", Toast.LENGTH_LONG).show()
                return@IDocumentReaderInitCompletion
            } else {
                MainActivity.isInitializedByBleDevice = true
                successfulInit()
            }
        }

    private fun successfulInit() {
        startActivity(Intent(this, MainActivity::class.java))
    }
}
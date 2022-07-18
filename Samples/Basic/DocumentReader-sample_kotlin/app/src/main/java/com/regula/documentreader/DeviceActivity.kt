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
import android.widget.RelativeLayout
import android.widget.Toast
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.ble.BLEWrapper
import com.regula.documentreader.api.ble.BleWrapperCallback
import com.regula.documentreader.api.ble.RegulaBleService
import com.regula.documentreader.api.ble.RegulaBleService.LocalBinder
import com.regula.documentreader.api.ble.callback.BleManagerCallback
import com.regula.documentreader.api.params.Device7310Config
import com.regula.documentreader.util.BluetoothUtil.isPermissionsGranted

class DeviceActivity : BaseActivity() {
    private var bleManager: BLEWrapper? = null
    private var isBleServiceConnected = false
    private var deviceLayout: RelativeLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val etDeviceName: EditText = findViewById(R.id.ed_device)
        val btnConnect: Button = findViewById(R.id.btn_connect)
        deviceLayout = findViewById(R.id.device_layout)
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
        setMenuVisibility(false)
    }

    override fun initializeReader() {
        showDialog("Initializing")
        setMenuVisibility(true)
        if (bleManager != null) {
            DocumentReader.Instance()
                .initializeReader(this@DeviceActivity, Device7310Config(bleManager), initCompletion)
        } else {
            dismissDialog()
            Toast.makeText(this, "Error reading license from device", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPrepareDbCompleted() {
        dismissDialog()
        if (isBleServiceConnected && bleManager != null && bleManager!!.isConnected && !DocumentReader.Instance().isReady) {
            initializeReader()
            return
        }
        setMenuVisibility(false)
        deviceLayout!!.visibility = View.VISIBLE
    }

    private fun setMenuVisibility(visibility: Boolean) {
        deviceLayout!!.visibility = if (visibility) View.GONE else View.VISIBLE
        fragmentContainer?.visibility = if (visibility) View.VISIBLE else View.GONE
        setActionBarVisibility(visibility)
    }

    override fun onResume() {
        super.onResume()
        if (DocumentReader.Instance().isReady) {
            setMenuVisibility(true)
        }
    }

    private fun setActionBarVisibility(isVisible: Boolean) {
        if (supportActionBar != null) {
            if (isVisible) supportActionBar!!.show() else supportActionBar!!.hide()
        }
    }

    private fun startBluetoothService() {
        if (isBleServiceConnected && bleManager != null && bleManager!!.isConnected && !DocumentReader.Instance().isReady) {
            initializeReader()
            return
        }
        if (!isPermissionsGranted(this) || isBleServiceConnected) {
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
            val bleService = (service as LocalBinder).service
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
}
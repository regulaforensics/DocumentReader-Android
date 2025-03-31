package com.regula.demo

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.regula.common.ble.BLEWrapper
import com.regula.common.ble.BleWrapperCallback
import com.regula.common.ble.RegulaBleService
import com.regula.common.ble.callback.BleManagerCallback
import com.regula.demo.databinding.ActivityConnectBinding
import com.regula.demo.util.BluetoothUtil
import com.regula.demo.util.PermissionUtil
import com.regula.demo.util.PermissionUtil.Companion.respondToPermissionRequest
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.completions.IDocumentReaderInitCompletion
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.params.BleDeviceConfig
import com.regula.facesdk.FaceSDK
import com.regula.facesdk.configuration.InitializationBleDeviceConfiguration
import com.regula.facesdk.exception.InitException

class ConnectDeviceActivity : AppCompatActivity() {
    private var bleManager: BLEWrapper? = null
    private var isBleServiceConnected = false
    private var loadingDialog: AlertDialog? = null

    private val bluetoothUtil = BluetoothUtil()

    private lateinit var binding: ActivityConnectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConnectBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val prefs = getSharedPreferences("MySharedPrefs", MODE_PRIVATE);
        binding.edDevice.setText(prefs.getString("device_name", DocumentReader.Instance().functionality().btDeviceName))
        binding.btnConnect.setOnClickListener {
            val deviceName = binding.edDevice.text.toString()
            prefs.edit().putString("device_name", deviceName).apply()
            showDialog("Searching devices")
            handler.sendEmptyMessageDelayed(0, 7000)
            DocumentReader.Instance().functionality().edit().setBtDeviceName(deviceName).apply()
            startBluetoothService()
        }
    }

    @SuppressLint("MissingPermission")
    fun initializeReader() {
        showDialog("Initializing")
        if (bleManager != null) {
            DocumentReader.Instance().initializeReader(this@ConnectDeviceActivity, BleDeviceConfig(
                bleManager!!
            ), initCompletion)
        } else {
            dismissDialog()
            Toast.makeText(this, "Error reading license from device", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("MissingPermission")
    private val initCompletion =
        IDocumentReaderInitCompletion { result: Boolean, error: DocumentReaderException? ->

            bleManager?.let {
                val config = InitializationBleDeviceConfiguration(bleManager!!)
                FaceSDK.Instance().LOG.isEnableLog = true
                FaceSDK.Instance().initialize(
                    this@ConnectDeviceActivity,
                    config
                ) { b: Boolean, initException: InitException? ->
                    if (!b) {
                        Log.d("Init", "Face Init failed:${initException?.message}")
                    }
                    startActivity()
                }
            } ?: run {
                startActivity()
            }

            if (result) {
                setupFunctionality()
            } else {
                Log.d("Init", "Init failed:$error")
            }
        }

    private fun startActivity() {
        dismissDialog()
        startActivity(Intent(this@ConnectDeviceActivity, MainActivity::class.java))
    }

    @SuppressLint("MissingPermission")
    private fun setupFunctionality() {
        DocumentReader.Instance().functionality().edit().setUseAuthenticator(true).apply()
    }

    private fun startBluetoothService() {
        if (isBleServiceConnected && bleManager?.isConnected == true && !DocumentReader.Instance().isReady) {
            initializeReader()
            return
        }
        if (!bluetoothUtil.isBluetoothSettingsReady(this) || isBleServiceConnected) {
            return
        }
        val bleIntent = Intent(this, RegulaBleService::class.java)
        startService(bleIntent)
        bindService(bleIntent, mBleConnection, 0)
    }

    private val mBleConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            isBleServiceConnected = true
            val bleService = (service as RegulaBleService.LocalBinder).service
            bleManager = bleService.bleManager
            bleManager.let {
                handler.removeMessages(0)
                if (DocumentReader.Instance().isReady) {
                    dismissDialog()
                    startActivity(Intent(this@ConnectDeviceActivity, MainActivity::class.java))
                    return
                }
                if (it?.isConnected == true) {
                    dismissDialog()
                    initializeReader()
                    return
                }
                it?.addCallback(bleManagerCallbacks)
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
            bleManager?.removeCallback(this)
            initializeReader()
        }
    }

    override fun onPause() {
        super.onPause()
        if (isBleServiceConnected) {
            unbindService(mBleConnection)
            isBleServiceConnected = false
        }
    }
    
    private fun dismissDialog() {
        loadingDialog?.dismiss()
    }

    fun showDialog(msg: String?) {
        dismissDialog()
        val builderDialog = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.simple_dialog, null)
        builderDialog.setTitle(msg)
        builderDialog.setView(dialogView)
        builderDialog.setCancelable(false)
        loadingDialog = builderDialog.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionUtil.PERMISSIONS_BLE_ACCESS) {

            if(permissions.isEmpty())
                return

            respondToPermissionRequest(this,
                permissions[0],
                grantResults,
                permissionGrantedFunc = {
                    if (bluetoothUtil.isBluetoothSettingsReady(this))
                        startBluetoothService()
                },
                permissionRejectedFunc = {

                })
        }
    }

    override fun onActivityResult(requestCode: Int, rc: Int, data: Intent?) {
        super.onActivityResult(requestCode, rc, data)
        var resultCode = rc
        if (requestCode == BluetoothUtil.INTENT_REQUEST_ENABLE_LOCATION)
            resultCode = if (bluetoothUtil.isLocationServiceEnabled(this)) RESULT_OK
            else requestCode
        if (requestCode == BluetoothUtil.INTENT_REQUEST_ENABLE_BLUETOOTH or BluetoothUtil.INTENT_REQUEST_ENABLE_LOCATION)
            if (resultCode == RESULT_OK) {
                if (bluetoothUtil.isBluetoothSettingsReady(this))
                    initializeReader()
            }
    }
}

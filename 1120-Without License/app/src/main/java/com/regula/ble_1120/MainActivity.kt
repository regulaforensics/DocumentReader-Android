package com.regula.ble_1120

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.regula.ble_1120.util.BluetoothUtil
import com.regula.ble_1120.util.PermissionUtil
import com.regula.ble_1120.util.PermissionUtil.Companion.respondToPermissionRequest
import com.regula.ble_1120.util.Utils
import com.regula.common.ble.BLEWrapper
import com.regula.common.ble.BleWrapperCallback
import com.regula.common.ble.RegulaBleService
import com.regula.common.ble.callback.BleManagerCallback
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.completions.IDocumentReaderInitCompletion
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.params.DocReaderConfig

class MainActivity : AppCompatActivity() {
    private var bleManager: BLEWrapper? = null
    private var isBleServiceConnected = false
    private var loadingDialog: AlertDialog? = null

    var etDeviceName: EditText? = null
    var btnConnect: Button? = null

    private val bluetoothUtil = BluetoothUtil()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
        initializeReader()
        etDeviceName?.setText(DocumentReader.Instance().functionality().btDeviceName)
        btnConnect?.setOnClickListener { view: View? ->
            if (etDeviceName?.text != null) {
                showDialog("Searching devices")
                handler.sendEmptyMessageDelayed(0, 7000)
                DocumentReader.Instance().functionality().edit()
                    .setUseAuthenticator(true)
                    .setBtDeviceName(etDeviceName?.text.toString()).apply()
                startBluetoothService()
            }
        }
    }

    private fun initViews() {
        etDeviceName = findViewById(R.id.ed_device)
        btnConnect = findViewById(R.id.btn_connect)
    }

    private fun initializeReader() {
        val license = Utils.getLicense(this) ?: return
        showDialog("Initializing")

        DocumentReader.Instance().initializeReader(this@MainActivity, DocReaderConfig(license), initCompletion)
    }

    private val initCompletion =
        IDocumentReaderInitCompletion { result: Boolean, error: DocumentReaderException? ->
            dismissDialog()
            if (result) {
                btnConnect?.isEnabled = true
            } else {
                Toast.makeText(this@MainActivity, "Init failed:${error?.message}", Toast.LENGTH_LONG).show()
                return@IDocumentReaderInitCompletion
            }
        }

    private fun startBluetoothService() {
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

            if (bleManager?.isConnected == true) {
                startActivity(Intent(this@MainActivity, SuccessfulInitActivity::class.java))
                return
            }

            showDialog("Searching devices")
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
            startActivity(Intent(this@MainActivity, SuccessfulInitActivity::class.java))
            dismissDialog()
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
        if (loadingDialog != null) {
            loadingDialog!!.dismiss()
        }
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
                        btnConnect?.callOnClick()
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

    override fun setContentView(@LayoutRes layoutResID: Int) {
        super.setContentView(layoutResID)

        applyEdgeToEdgeInsets()
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

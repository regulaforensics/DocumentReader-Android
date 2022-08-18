package com.regula.ble_7310_kotlin

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.regula.ble_7310_kotlin.util.BluetoothUtil.isPermissionsGranted
import com.regula.common.utils.CameraUtil
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.ble.BLEWrapper
import com.regula.documentreader.api.ble.BleWrapperCallback
import com.regula.documentreader.api.ble.RegulaBleService
import com.regula.documentreader.api.ble.callback.BleManagerCallback
import com.regula.documentreader.api.completions.IDocumentReaderInitCompletion
import com.regula.documentreader.api.completions.IDocumentReaderPrepareCompletion
import com.regula.documentreader.api.enums.Scenario
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.params.Device7310Config

class MainActivity : AppCompatActivity() {
    private var bleManager: BLEWrapper? = null
    private var isBleServiceConnected = false
    private var loadingDialog: AlertDialog? = null

    var etDeviceName: EditText? = null
    var btnConnect: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
        prepareDatabase()
        etDeviceName!!.setText(DocumentReader.Instance().functionality().btDeviceName)
        btnConnect!!.setOnClickListener { view: View? ->
            if (!CameraUtil.isWP7Device()) {
                Toast.makeText(
                    this,
                    "you need WP7 device model for the next stages",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (etDeviceName!!.text != null) {
                showDialog("Searching devices")
                handler.sendEmptyMessageDelayed(0, 7000)
                DocumentReader.Instance().functionality().edit()
                    .setBtDeviceName(etDeviceName!!.text.toString()).apply()
                startBluetoothService()
            }
        }
    }

    private fun prepareDatabase() {
        showDialog("preparing database")
        DocumentReader.Instance()
            .prepareDatabase(//call prepareDatabase not necessary if you have local database at assets/Regula/db.dat
                this@MainActivity,
                "FullAuth",
                object : IDocumentReaderPrepareCompletion {
                    override fun onPrepareProgressChanged(progress: Int) {
                        if (loadingDialog != null)
                            loadingDialog!!.setTitle("Downloading database: $progress%")
                    }

                    override fun onPrepareCompleted(
                        status: Boolean,
                        error: DocumentReaderException?
                    ) {
                        if (!status) {
                            dismissDialog()
                            Toast.makeText(
                                this@MainActivity,
                                "Prepare DB failed:$error",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        dismissDialog()
                    }
                })
    }

    private fun initViews() {
        etDeviceName = findViewById(R.id.ed_device)
        btnConnect = findViewById(R.id.btn_connect)
    }

    fun initializeReader() {
        showDialog("Initializing")
        if (bleManager != null) {
            DocumentReader.Instance()
                .initializeReader(this@MainActivity, Device7310Config(bleManager), initCompletion)
        } else {
            dismissDialog()
            Toast.makeText(this, "Error reading license from device", Toast.LENGTH_LONG).show()
        }
    }

    private val initCompletion =
        IDocumentReaderInitCompletion { result: Boolean, error: DocumentReaderException? ->
            dismissDialog()
            if (result) {
                DocumentReader.Instance().functionality().edit().setUseAuthenticator(true).apply()
                DocumentReader.Instance().processParams().setScenario(Scenario.SCENARIO_FULL_AUTH)
                startActivity(Intent(this@MainActivity, SuccessfulInitActivity::class.java))
            } else {
                Toast.makeText(this@MainActivity, "Init failed:$error", Toast.LENGTH_LONG).show()
                return@IDocumentReaderInitCompletion
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
        Toast.makeText(this, "Failed to connect to the device 7310", Toast.LENGTH_SHORT).show()
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

    fun showDialog(msg: String?) {
        dismissDialog()
        val builderDialog = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.simple_dialog, null)
        builderDialog.setTitle(msg)
        builderDialog.setView(dialogView)
        builderDialog.setCancelable(false)
        loadingDialog = builderDialog.show()
    }
}
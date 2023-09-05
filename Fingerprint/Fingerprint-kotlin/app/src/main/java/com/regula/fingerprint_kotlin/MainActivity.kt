package com.regula.fingerprint_kotlin

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.regula.fingerprint_kotlin.BluetoothReaderHelper.calcCheckSum
import com.regula.fingerprint_kotlin.BluetoothReaderHelper.getFingerprintImage
import com.regula.fingerprint_kotlin.BluetoothReaderHelper.memcpy
import com.regula.fingerprint_kotlin.util.BluetoothPermissionHelper.isPermissionsGranted
import com.regula.fingerprint_kotlin.util.BluetoothPermissionHelper.requestBlePermissions
import com.regula.fingerprint_kotlin.util.Constants
import com.regula.fingerprint_kotlin.util.Constants.CMD_GETIMAGE
import com.regula.fingerprint_kotlin.util.Constants.IMG288
import com.regula.fingerprint_kotlin.util.Constants.MESSAGE_STATE_CHANGE
import com.regula.fingerprint_kotlin.util.Constants.TOAST
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {
    private var mDeviceCmd: Byte = 0x00
    private var mIsWork = false
    private var mTimerTimeout: Timer? = null
    private var mTaskTimeout: TimerTask? = null
    private var mHandlerTimeout: Handler? = null

    // Local Bluetooth adapter
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var imgSize = 0
    var mUpImageSize = 0

    // Member object for the chat services
    private var mChatService: BluetoothReaderService? = null
    var mUpImage = ByteArray(73728) // image data
    var mUpImage2 = ByteArray(73728) // image data
    private var fingerprintImage: ImageView? = null

    override fun onStart() {
        super.onStart()
        if (!mBluetoothAdapter!!.isEnabled) {
            if (isPermissionsGranted(this)) {
                val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableIntent, 2)
            }
        } else {
            if (mChatService == null)
                mChatService = BluetoothReaderService(
                    this,
                    mHandler
                ) // Initialize the BluetoothChatService to perform bluetooth connections
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show()
            finish()
        }
        setup()
    }

    @Synchronized
    public override fun onResume() {
        super.onResume()
        if (isPermissionsGranted(this)) {
            if (mChatService != null) {
                if (mChatService!!.state == Constants.STATE_NONE) {
                    mChatService!!.start()
                }
            }
        }
    }

    fun setup() {
        val captureFinger = findViewById<Button>(R.id.captureFingerBtn)
        fingerprintImage = findViewById(R.id.capturedFingerIv)
        captureFinger.setOnClickListener { v: View? ->
            if(mChatService != null) {
                imgSize = IMG288
                mUpImageSize = 0
                sendCommand(CMD_GETIMAGE, null, 0)
            }
        }
        val connectToScanner = findViewById<Button>(R.id.connectBtn)
        connectToScanner.setOnClickListener { v: View? ->
            if (checkPermissions())
                connectBluetooth()
        }
    }

    private val mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MESSAGE_STATE_CHANGE -> when (msg.arg1) {
                    Constants.STATE_CONNECTED -> Toast.makeText(
                        this@MainActivity,
                        "connected",
                        Toast.LENGTH_SHORT
                    ).show()
                    Constants.STATE_CONNECTING -> Toast.makeText(
                        this@MainActivity,
                        "connecting",
                        Toast.LENGTH_SHORT
                    ).show()
                    Constants.STATE_LISTEN, Constants.STATE_NONE -> Toast.makeText(
                        this@MainActivity,
                        "not connected",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                Constants.MESSAGE_WRITE -> {}
                Constants.MESSAGE_READ -> {
                    val readBuf = msg.obj as ByteArray
                    if (readBuf.isNotEmpty()) {
                        if (readBuf[0] == 0x1b.toByte()) {
                            BluetoothReaderHelper.addStatusListHex(readBuf, msg.arg1)
                        } else {
                            receiveCommand(readBuf, msg.arg1)
                        }
                    }
                }
                Constants.MESSAGE_TOAST -> Toast.makeText(
                    applicationContext,
                    msg.data.getString(TOAST),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * stop the timer
     */
    fun timeOutStop() {
        if (mTimerTimeout != null) {
            mTimerTimeout!!.cancel()
            mTimerTimeout = null
            mTaskTimeout!!.cancel()
            mTaskTimeout = null
        }
    }

    /**
     * stat the timer for counting
     */
    private fun timeOutStart() {
        if (mTimerTimeout != null) {
            return
        }
        mTimerTimeout = Timer()
        mHandlerTimeout = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                timeOutStop()
                if (mIsWork) {
                    mIsWork = false
                    //AddStatusList("Time Out");
                }
                super.handleMessage(msg)
            }
        }
        mTaskTimeout = object : TimerTask() {
            override fun run() {
                val message = Message()
                message.what = 1
                (mHandlerTimeout as Handler).sendMessage(message)
            }
        }
        mTimerTimeout!!.schedule(mTaskTimeout, 10000, 10000)
    }

    /**
     * Generate the command package sending via bluetooth
     *
     * @param cmdid command code for different function achieve.
     * @param data  the required data need to send to the device
     * @param size  the size of the byte[] data
     */
    private fun sendCommand(cmdid: Byte, data: ByteArray?, size: Int) {
        if (mIsWork) return
        val sendsize = 9 + size
        val sendbuf = ByteArray(sendsize)
        sendbuf[0] = 'F'.code.toByte()
        sendbuf[1] = 'T'.code.toByte()
        sendbuf[2] = 0
        sendbuf[3] = 0
        sendbuf[4] = cmdid
        sendbuf[5] = size.toByte()
        sendbuf[6] = (size shr 8).toByte()
        if (size > 0) {
            System.arraycopy(data, 0, sendbuf, 7, size)
        }
        val sum = calcCheckSum(sendbuf, 7 + size)
        sendbuf[7 + size] = sum.toByte()
        sendbuf[8 + size] = (sum shr 8).toByte()
        mIsWork = true
        timeOutStart()
        mDeviceCmd = cmdid
        mChatService!!.write(sendbuf)
    }

    /**
     * Received the response from the device
     *
     * @param databuf  the data package response from the device
     * @param datasize the size of the data package
     */
    private fun receiveCommand(databuf: ByteArray, datasize: Int) {
        if (mDeviceCmd == CMD_GETIMAGE) { //receiving the image data from the device
            if (imgSize == IMG288) {   //image size with 256*288
                memcpy(mUpImage, mUpImageSize, databuf, 0, datasize)
                mUpImageSize += datasize
                if (mUpImageSize >= 36864) {
                    val file = File("/sdcard/test.raw")
                    try {
                        file.createNewFile()
                        val out = FileOutputStream(file)
                        out.write(mUpImage)
                        out.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    val imageData =
                        BluetoothReaderHelper.checkHaveTis(mUpImage, mUpImage2, mUpImageSize)
                    val bmpdata: ByteArray = getFingerprintImage(imageData, 256, 288, 0 /*18*/)!!
                    val image = BitmapFactory.decodeByteArray(bmpdata, 0, bmpdata.size)
                    fingerprintImage!!.setImageBitmap(image)
                    mUpImageSize = 0
                    mIsWork = false
                }
            }
        }
    }

    private fun connectBluetooth() {
        if (mChatService == null)
            mChatService = BluetoothReaderService(
                this,
                mHandler
            ) // Initialize the BluetoothChatService to perform bluetooth connections
        val address = "8C:DE:52:CD:F9:8B"
        BluetoothReaderHelper.connectBluetooth(address, mBluetoothAdapter!!, mChatService!!)
    }

    private fun checkPermissions(): Boolean {
        if (!this.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Toast.makeText(this@MainActivity, "ble not supported", Toast.LENGTH_SHORT)
                .show()
            return false
        }
        if (!this.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this@MainActivity, "ble not supported", Toast.LENGTH_SHORT).show()
            return false
        }
        requestBlePermissions(
            this@MainActivity,
            PackageManager.PERMISSION_GRANTED
        )
        return if (isPermissionsGranted(this)) {
            if (!mBluetoothAdapter!!.isEnabled) {
                val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableIntent, 2)
                return false;
            }
            true
        } else {
            Toast.makeText(
                this@MainActivity,
                "ble permission is not granted",
                Toast.LENGTH_SHORT
            ).show()
            false
        }
    }
}
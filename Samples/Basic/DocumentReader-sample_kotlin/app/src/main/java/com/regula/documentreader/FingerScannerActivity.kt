package com.regula.documentreader

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.regula.documentreader.fingerprintsample.BluetoothReaderHelper
import com.regula.documentreader.fingerprintsample.BluetoothReaderHelper.getFingerprintImage
import com.regula.documentreader.fingerprintsample.BluetoothReaderService
import com.regula.documentreader.util.Constants
import com.regula.documentreader.util.Constants.MESSAGE_STATE_CHANGE
import com.regula.documentreader.util.Constants.TOAST
import java.io.*
import java.util.*
import kotlin.experimental.and

class FingerScannerActivity : AppCompatActivity() {
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
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, 2)
        } else {
            if (mChatService == null) setup()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fingerprint)
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, PERMISSIONS_STORAGE,
                    REQUEST_PERMISSION_CODE
                )
            }
        }
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    @Synchronized
    public override fun onResume() {
        super.onResume()
        if (mChatService != null) {
            if (mChatService!!.state == Constants.STATE_NONE) {
                mChatService!!.start()
            }
        }
    }

    fun setup() {
        val captureFinger = findViewById<Button>(R.id.captureFingerBtn)
        fingerprintImage = findViewById(R.id.capturedFingerIv)
        captureFinger.setOnClickListener { v: View? ->
            imgSize = IMG288
            mUpImageSize = 0
            SendCommand(CMD_GETIMAGE, null, 0)
        }
        val connectToScanner = findViewById<Button>(R.id.connectBtn)
        connectToScanner.setOnClickListener { v: View? -> connectBluetooth() }
        mChatService = BluetoothReaderService(
            this,
            mHandler
        ) // Initialize the BluetoothChatService to perform bluetooth connections
    }

    @SuppressLint("HandlerLeak")
    private val mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MESSAGE_STATE_CHANGE -> when (msg.arg1) {
                    Constants.STATE_CONNECTED -> Toast.makeText(
                        this@FingerScannerActivity,
                        "connected",
                        Toast.LENGTH_SHORT
                    ).show()
                    Constants.STATE_CONNECTING -> Toast.makeText(
                        this@FingerScannerActivity,
                        "connecting",
                        Toast.LENGTH_SHORT
                    ).show()
                    Constants.STATE_LISTEN, Constants.STATE_NONE -> Toast.makeText(
                        this@FingerScannerActivity,
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
                            ReceiveCommand(readBuf, msg.arg1)
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
     * calculate the check sum of the byte[]
     *
     * @param buffer byte[] required for calculating
     * @param size   the size of the byte[]
     * @return the calculated check sum
     */
    private fun calcCheckSum(buffer: ByteArray, size: Int): Int {
        var sum = 0
        for (i in 0 until size) {
            sum = sum + buffer[i]
        }
        return sum and 0x00ff
    }

    /**
     * stop the timer
     */
    fun TimeOutStop() {
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
    fun TimeOutStart() {
        if (mTimerTimeout != null) {
            return
        }
        mTimerTimeout = Timer()
        mHandlerTimeout = object : Handler() {
            override fun handleMessage(msg: Message) {
                TimeOutStop()
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
    private fun SendCommand(cmdid: Byte, data: ByteArray?, size: Int) {
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
        TimeOutStart()
        mDeviceCmd = cmdid
        mChatService!!.write(sendbuf)
    }

    /**
     * Received the response from the device
     *
     * @param databuf  the data package response from the device
     * @param datasize the size of the data package
     */
    private fun ReceiveCommand(databuf: ByteArray, datasize: Int) {
        if (mDeviceCmd == CMD_GETIMAGE) { //receiving the image data from the device
            if (imgSize == IMG288) {   //image size with 256*288
                memcpy(mUpImage, mUpImageSize, databuf, 0, datasize)
                mUpImageSize = mUpImageSize + datasize
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
                    val imageData = BluetoothReaderHelper.checkHaveTis(mUpImage,mUpImage2,mUpImageSize)
                    val bmpdata: ByteArray = getFingerprintImage(imageData, 256, 288, 0 /*18*/)
                    val image = BitmapFactory.decodeByteArray(bmpdata, 0, bmpdata.size)
                    fingerprintImage!!.setImageBitmap(image)
                    mUpImageSize = 0
                    mIsWork = false
                }
            }
        }
    }

    private fun changeByte(data: Int): ByteArray {
        val b4 = (data shr 24).toByte()
        val b3 = (data shl 8 shr 24).toByte()
        val b2 = (data shl 16 shr 24).toByte()
        val b1 = (data shl 24 shr 24).toByte()
        return byteArrayOf(b1, b2, b3, b4)
    }

    /**
     * generate the image data into Bitmap format
     *
     * @param width  width of the image
     * @param height height of the image
     * @param data   image data
     * @return bitmap image data
     */
    private fun toBmpByte(width: Int, height: Int, data: ByteArray): ByteArray? {
        var buffer: ByteArray? = null
        try {
            val baos = ByteArrayOutputStream()
            val dos = DataOutputStream(baos)
            val bfType = 0x424d
            val bfSize = 54 + 1024 + width * height
            val bfReserved1 = 0
            val bfReserved2 = 0
            val bfOffBits = 54 + 1024
            dos.writeShort(bfType)
            dos.write(changeByte(bfSize), 0, 4)
            dos.write(changeByte(bfReserved1), 0, 2)
            dos.write(changeByte(bfReserved2), 0, 2)
            dos.write(changeByte(bfOffBits), 0, 4)
            val biSize = 40
            val biPlanes = 1
            val biBitcount = 8
            val biCompression = 0
            val biSizeImage = width * height
            val biXPelsPerMeter = 0
            val biYPelsPerMeter = 0
            val biClrUsed = 256
            val biClrImportant = 0
            dos.write(changeByte(biSize), 0, 4)
            dos.write(changeByte(width), 0, 4)
            dos.write(changeByte(height), 0, 4)
            dos.write(changeByte(biPlanes), 0, 2)
            dos.write(changeByte(biBitcount), 0, 2)
            dos.write(changeByte(biCompression), 0, 4)
            dos.write(changeByte(biSizeImage), 0, 4)
            dos.write(changeByte(biXPelsPerMeter), 0, 4)
            dos.write(changeByte(biYPelsPerMeter), 0, 4)
            dos.write(changeByte(biClrUsed), 0, 4)
            dos.write(changeByte(biClrImportant), 0, 4)
            val palatte = ByteArray(1024)
            for (i in 0..255) {
                palatte[i * 4] = i.toByte()
                palatte[i * 4 + 1] = i.toByte()
                palatte[i * 4 + 2] = i.toByte()
                palatte[i * 4 + 3] = 0
            }
            dos.write(palatte)
            dos.write(data)
            dos.flush()
            buffer = baos.toByteArray()
            dos.close()
            baos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return buffer
    }

    /**
     * method of copying the byte[] data with specific length
     *
     * @param dstbuf    byte[] for storing the copied data with specific length
     * @param dstoffset the starting point for storing
     * @param srcbuf    the source byte[] used for copying.
     * @param srcoffset the starting point for copying
     * @param size      the length required to copy
     */
    private fun memcpy(
        dstbuf: ByteArray,
        dstoffset: Int,
        srcbuf: ByteArray,
        srcoffset: Int,
        size: Int
    ) {
        if (size >= 0) System.arraycopy(srcbuf, srcoffset, dstbuf, dstoffset, size)
    }

    private fun connectBluetooth() {
        val address = "8C:DE:52:CD:F9:8B"
        BluetoothReaderHelper.connectBluetooth(address, mBluetoothAdapter, mChatService)
    }

    companion object {
        private const val REQUEST_PERMISSION_CODE = 1
        private const val CMD_GETIMAGE: Byte = 0x30

        //other image size
        const val IMG288 = 288
        private val PERMISSIONS_STORAGE = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}
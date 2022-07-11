package com.regula.documentreader;


import static com.regula.documentreader.fingerprintsample.BluetoothReaderHelper.getFingerprintImage;
import static com.regula.documentreader.util.Constants.MESSAGE_STATE_CHANGE;
import static com.regula.documentreader.util.Constants.TOAST;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.regula.documentreader.fingerprintsample.BluetoothReaderHelper;
import com.regula.documentreader.fingerprintsample.BluetoothReaderService;
import com.regula.documentreader.util.Constants;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class FingerScannerActivity extends AppCompatActivity {
    private static final int REQUEST_PERMISSION_CODE = 1;
    private final static byte CMD_GETIMAGE = 0x30;
    //other image size
    public static final int IMG288 = 288;

    private byte mDeviceCmd = 0x00;
    private boolean mIsWork = false;

    private Timer mTimerTimeout = null;
    private TimerTask mTaskTimeout = null;
    private Handler mHandlerTimeout;

    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;

    private int imgSize;
    public int mUpImageSize = 0;
    private static final String[] PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    // Member object for the chat services
    private BluetoothReaderService mChatService = null;
    public byte[] mUpImage = new byte[73728]; // image data
    public byte[] mUpImage2 = new byte[73728]; // image data

    private ImageView fingerprintImage;

    @Override
    protected void onStart() {
        super.onStart();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, 2);
        } else {
            if (mChatService == null) setup();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fingerprint);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission
                    .WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE,
                        REQUEST_PERMISSION_CODE);
            }
        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (mChatService != null) {
            if (mChatService.getState() == Constants.STATE_NONE) {
                mChatService.start();
            }
        }
    }

    public void setup() {
        final Button captureFinger = findViewById(R.id.captureFingerBtn);
        fingerprintImage = findViewById(R.id.capturedFingerIv);
        captureFinger.setOnClickListener(v -> {
            imgSize = IMG288;
            mUpImageSize = 0;
            SendCommand(CMD_GETIMAGE, null, 0);
        });
        final Button connectToScanner = findViewById(R.id.connectBtn);
        connectToScanner.setOnClickListener(v -> connectBluetooth());

        mChatService = new BluetoothReaderService(this, mHandler);    // Initialize the BluetoothChatService to perform bluetooth connections
    }

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case Constants.STATE_CONNECTED:
                            Toast.makeText(FingerScannerActivity.this, "connected", Toast.LENGTH_SHORT).show();
                            break;
                        case Constants.STATE_CONNECTING:
                            Toast.makeText(FingerScannerActivity.this, "connecting", Toast.LENGTH_SHORT).show();
                            break;
                        case Constants.STATE_LISTEN:
                        case Constants.STATE_NONE:
                            Toast.makeText(FingerScannerActivity.this, "not connected", Toast.LENGTH_SHORT).show();
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    if (readBuf.length > 0) {
                        if (readBuf[0] == (byte) 0x1b) {
                            addStatusListHex(readBuf, msg.arg1);
                        } else {
                            ReceiveCommand(readBuf, msg.arg1);
                        }
                    }
                    break;

                case Constants.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private void addStatusListHex(byte[] data, int size) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < size; i++) {
            text.append(" ").append(Integer.toHexString(data[i] & 0xFF).toUpperCase()).append("  ");
        }
    }

    /**
     * calculate the check sum of the byte[]
     *
     * @param buffer byte[] required for calculating
     * @param size   the size of the byte[]
     * @return the calculated check sum
     */
    private int calcCheckSum(byte[] buffer, int size) {
        int sum = 0;
        for (int i = 0; i < size; i++) {
            sum = sum + buffer[i];
        }
        return (sum & 0x00ff);
    }

    /**
     * stop the timer
     */
    public void TimeOutStop() {
        if (mTimerTimeout != null) {
            mTimerTimeout.cancel();
            mTimerTimeout = null;
            mTaskTimeout.cancel();
            mTaskTimeout = null;
        }
    }


    /**
     * stat the timer for counting
     */
    public void TimeOutStart() {
        if (mTimerTimeout != null) {
            return;
        }
        mTimerTimeout = new Timer();
        mHandlerTimeout = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                TimeOutStop();
                if (mIsWork) {
                    mIsWork = false;
                    //AddStatusList("Time Out");
                }
                super.handleMessage(msg);
            }
        };
        mTaskTimeout = new TimerTask() {
            @Override
            public void run() {
                Message message = new Message();
                message.what = 1;
                mHandlerTimeout.sendMessage(message);
            }
        };
        mTimerTimeout.schedule(mTaskTimeout, 10000, 10000);
    }


    /**
     * Generate the command package sending via bluetooth
     *
     * @param cmdid command code for different function achieve.
     * @param data  the required data need to send to the device
     * @param size  the size of the byte[] data
     */
    private void SendCommand(byte cmdid, byte[] data, int size) {
        if (mIsWork) return;

        int sendsize = 9 + size;
        byte[] sendbuf = new byte[sendsize];
        sendbuf[0] = 'F';
        sendbuf[1] = 'T';
        sendbuf[2] = 0;
        sendbuf[3] = 0;
        sendbuf[4] = cmdid;
        sendbuf[5] = (byte) (size);
        sendbuf[6] = (byte) (size >> 8);
        if (size > 0) {
            System.arraycopy(data, 0, sendbuf, 7, size);
        }
        int sum = calcCheckSum(sendbuf, (7 + size));
        sendbuf[7 + size] = (byte) (sum);
        sendbuf[8 + size] = (byte) (sum >> 8);

        mIsWork = true;
        TimeOutStart();
        mDeviceCmd = cmdid;
        mChatService.write(sendbuf);
    }

    /**
     * Received the response from the device
     *
     * @param databuf  the data package response from the device
     * @param datasize the size of the data package
     */
    private void ReceiveCommand(byte[] databuf, int datasize) {
        if (mDeviceCmd == CMD_GETIMAGE) { //receiving the image data from the device
            if (imgSize == IMG288) {   //image size with 256*288
                memcpy(mUpImage, mUpImageSize, databuf, 0, datasize);
                mUpImageSize = mUpImageSize + datasize;
                if (mUpImageSize >= 36864) {
                    File file = new File("/sdcard/test.raw");
                    try {
                        file.createNewFile();
                        FileOutputStream out = new FileOutputStream(file);
                        out.write(mUpImage);
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    byte[] imageData = checkHaveTis(mUpImage);
                    byte[] bmpdata = getFingerprintImage(imageData, 256, 288, 0/*18*/);
                    Bitmap image = BitmapFactory.decodeByteArray(bmpdata, 0, bmpdata.length);
                    fingerprintImage.setImageBitmap(image);
                    mUpImageSize = 0;
                    mIsWork = false;
                }
            }
        }
    }

    private byte[] changeByte(int data) {
        byte b4 = (byte) ((data) >> 24);
        byte b3 = (byte) (((data) << 8) >> 24);
        byte b2 = (byte) (((data) << 16) >> 24);
        byte b1 = (byte) (((data) << 24) >> 24);
        return new byte[]{b1, b2, b3, b4};
    }

    /**
     * generate the image data into Bitmap format
     *
     * @param width  width of the image
     * @param height height of the image
     * @param data   image data
     * @return bitmap image data
     */
    private byte[] toBmpByte(int width, int height, byte[] data) {
        byte[] buffer = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            int bfType = 0x424d;
            int bfSize = 54 + 1024 + width * height;
            int bfReserved1 = 0;
            int bfReserved2 = 0;
            int bfOffBits = 54 + 1024;

            dos.writeShort(bfType);
            dos.write(changeByte(bfSize), 0, 4);
            dos.write(changeByte(bfReserved1), 0, 2);
            dos.write(changeByte(bfReserved2), 0, 2);
            dos.write(changeByte(bfOffBits), 0, 4);

            int biSize = 40;
            int biPlanes = 1;
            int biBitcount = 8;
            int biCompression = 0;
            int biSizeImage = width * height;
            int biXPelsPerMeter = 0;
            int biYPelsPerMeter = 0;
            int biClrUsed = 256;
            int biClrImportant = 0;

            dos.write(changeByte(biSize), 0, 4);
            dos.write(changeByte(width), 0, 4);
            dos.write(changeByte(height), 0, 4);
            dos.write(changeByte(biPlanes), 0, 2);
            dos.write(changeByte(biBitcount), 0, 2);
            dos.write(changeByte(biCompression), 0, 4);
            dos.write(changeByte(biSizeImage), 0, 4);
            dos.write(changeByte(biXPelsPerMeter), 0, 4);
            dos.write(changeByte(biYPelsPerMeter), 0, 4);
            dos.write(changeByte(biClrUsed), 0, 4);
            dos.write(changeByte(biClrImportant), 0, 4);

            byte[] palatte = new byte[1024];
            for (int i = 0; i < 256; i++) {
                palatte[i * 4] = (byte) i;
                palatte[i * 4 + 1] = (byte) i;
                palatte[i * 4 + 2] = (byte) i;
                palatte[i * 4 + 3] = 0;
            }
            dos.write(palatte);

            dos.write(data);
            dos.flush();
            buffer = baos.toByteArray();
            dos.close();
            baos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buffer;
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
    private void memcpy(byte[] dstbuf, int dstoffset, byte[] srcbuf, int srcoffset, int size) {
        if (size >= 0) System.arraycopy(srcbuf, srcoffset, dstbuf, dstoffset, size);
    }

    private byte[] checkHaveTis(byte[] mUpImage) {
        if ((mUpImage[0] == 'F') && (mUpImage[1] == 'T')) {
            memcpy(mUpImage2, 0, mUpImage, 20, mUpImageSize - 20);
        } else {
            memcpy(mUpImage2, 0, mUpImage, 0, mUpImageSize);
        }
        return mUpImage2;
    }

    private void connectBluetooth() {
        String address = "8C:DE:52:CD:F9:8B";
        BluetoothReaderHelper.connectBluetooth(address,mBluetoothAdapter,mChatService);
    }
}

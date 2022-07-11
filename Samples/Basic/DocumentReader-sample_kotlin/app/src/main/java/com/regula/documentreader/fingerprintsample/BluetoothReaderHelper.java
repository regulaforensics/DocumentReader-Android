package com.regula.documentreader.fingerprintsample;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

public class BluetoothReaderHelper {

    public static void connectBluetooth(String mac, BluetoothAdapter mBluetoothAdapter, BluetoothReaderService mChatService){
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mac);
        mChatService.connect(device);
        Log.v("Connected device mac", device.getAddress());
    }

    /**
     * calculate the check sum of the byte[]
     *
     * @param buffer byte[] required for calculating
     * @param size   the size of the byte[]
     * @return the calculated check sum
     */
    public static int calcCheckSum(byte[] buffer, int size) {
        int sum = 0;
        for (int i = 0; i < size; i++) {
            sum = sum + buffer[i];
        }
        return (sum & 0x00ff);
    }

    /**
     * generate the fingerprint image
     *
     * @param data   image data
     * @param width  width of the image
     * @param height height of the image
     * @param offset default setting as 0
     * @return bitmap image data
     */
    public static byte[] getFingerprintImage(byte[] data, int width, int height, int offset) {
        if (data == null) {
            return null;
        }
        byte[] imageData = new byte[width * height];
        for (int i = 0; i < (width * height / 2); i++) {
            imageData[i * 2] = (byte) (data[i + offset] & 0xf0);
            imageData[i * 2 + 1] = (byte) (data[i + offset] << 4 & 0xf0);
        }
        byte[] bmpData = toBmpByte(width, height, imageData);
        return bmpData;
    }

    public static byte[] changeByte(int data) {
        byte b4 = (byte) ((data) >> 24);
        byte b3 = (byte) (((data) << 8) >> 24);
        byte b2 = (byte) (((data) << 16) >> 24);
        byte b1 = (byte) (((data) << 24) >> 24);
        byte[] bytes = {b1, b2, b3, b4};
        return bytes;
    }

    /**
     * generate the image data into Bitmap format
     *
     * @param width  width of the image
     * @param height height of the image
     * @param data   image data
     * @return bitmap image data
     */
    public static byte[] toBmpByte(int width, int height, byte[] data) {
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
            int biWidth = width;
            int biHeight = height;
            int biPlanes = 1;
            int biBitcount = 8;
            int biCompression = 0;
            int biSizeImage = width * height;
            int biXPelsPerMeter = 0;
            int biYPelsPerMeter = 0;
            int biClrUsed = 256;
            int biClrImportant = 0;

            dos.write(changeByte(biSize), 0, 4);
            dos.write(changeByte(biWidth), 0, 4);
            dos.write(changeByte(biHeight), 0, 4);
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
    public static void memcpy(byte[] dstbuf, int dstoffset, byte[] srcbuf, int srcoffset, int size) {
        if (size >= 0) System.arraycopy(srcbuf, srcoffset, dstbuf, dstoffset, size);
    }

    public static byte[] checkHaveTis(byte[] mUpImage, byte[] mUpImage2, int mUpImageSize) {
        if ((mUpImage[0] == 'F') && (mUpImage[1] == 'T')) {
            memcpy(mUpImage2, 0, mUpImage, 20, mUpImageSize - 20);
        } else {
            memcpy(mUpImage2, 0, mUpImage, 0, mUpImageSize);
        }
        return mUpImage2;
    }

    public static StringBuilder addStatusListHex(byte[] data, int size) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < size; i++) {
            text.append(" ").append(Integer.toHexString(data[i] & 0xFF).toUpperCase()).append("  ");
        }
        return text;
    }
}

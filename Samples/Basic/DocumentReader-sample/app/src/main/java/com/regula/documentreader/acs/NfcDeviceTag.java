package com.regula.documentreader.acs;

import android.util.Log;

import com.regula.documentreader.api.nfc.BleNfcTag;
import com.regula.documentreader.api.nfc.IUniversalNfcTag;

import java.lang.ref.WeakReference;

import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

/**
 * Created by Sergey Yakimchik on 9.09.21.
 * Copyright (c) 2021 Regula. All rights reserved.
 */

public class NfcDeviceTag implements IUniversalNfcTag {

    private final static String TAG = BleNfcTag.class.getSimpleName();

    private boolean cardAbsent = false;
    private final WeakReference<CardChannel> mChannel;

    public NfcDeviceTag(CardChannel channel) {
        mChannel = new WeakReference<>(channel);
    }

    @Override
    public byte[] sendApduCommand(byte[] apduCommand) {
        Log.d(TAG, "Command to BLE: " + bytesToHex(apduCommand));
        if (cardAbsent)
            return null;
        CommandAPDU commandAPDU = new CommandAPDU(apduCommand);
        byte[] response;
        try {
            ResponseAPDU responseAPDU = null;
            CardChannel channel = mChannel.get();
            if (channel != null)
                responseAPDU = channel.transmit(commandAPDU);
            response = responseAPDU != null ? responseAPDU.getBytes() : null;
        } catch (CardException e) {
            Log.e(TAG, "Exception: " + e.getMessage());
            return null;
        }

        Log.d(TAG, "Response from BLE: " + (response != null ? bytesToHex(response) : null));
        return cardAbsent ? null : response;
    }

    @Override
    public int getTranscieveTimeout() {
        return 0;
    }

    @Override
    public void setTranscieveTimeout(int timeout) {

    }

    @Override
    public void connect() {
        cardAbsent = false;
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public void cardAbsent() {
        cardAbsent = true;
    }
}

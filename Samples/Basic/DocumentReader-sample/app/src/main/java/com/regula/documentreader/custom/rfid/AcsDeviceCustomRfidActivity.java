package com.regula.documentreader.custom.rfid;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.acs.smartcardio.BluetoothSmartCard;
import com.acs.smartcardio.BluetoothTerminalManager;
import com.acs.smartcardio.TerminalTimeouts;
import com.regula.documentreader.R;
import com.regula.documentreader.acs.CardStateMonitor;
import com.regula.documentreader.acs.NfcDeviceTag;

import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;


public class AcsDeviceCustomRfidActivity extends BaseCustomRfiActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_ACCESS_FINE_LOCATION = 2;

    private static final String TAG = "CustomRfidActivity";

    private BluetoothAdapter mBluetoothAdapter;
    BluetoothTerminalManager manager;
    CardStateMonitor mCardStateMonitor;
    CardTerminal mTerminal;

    private NfcDeviceTag nfcTag;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * Use this check to determine whether BLE is supported on the device.  Then you can
         * selectively disable BLE-related features.
         */
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {

            Toast.makeText(this, R.string.error_bluetooth_le_not_supported,
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        /*
         * Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
         * BluetoothAdapter through BluetoothManager.
         */
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }

        /* Checks if Bluetooth is supported on the device. */
        if (mBluetoothAdapter == null) {

            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initBle();
    }

    @Override
    protected void onResume() {
        super.onResume();

        /*
         * Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
         * fire an intent to display a dialog asking the user to grant permission to enable it.
         */
        if (!mBluetoothAdapter.isEnabled()) {

            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        if (mCardStateMonitor != null)
            mCardStateMonitor.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mCardStateMonitor != null)
            mCardStateMonitor.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCardStateMonitor != null)
            mCardStateMonitor.removeOnStateChangeListener();
    }

    private void initBle() {
        /* Request access fine location permission. */
        if (ContextCompat.checkSelfPermission(AcsDeviceCustomRfidActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(AcsDeviceCustomRfidActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_ACCESS_FINE_LOCATION);

            return;
        }

        manager = BluetoothSmartCard.getInstance(this).getManager();

        /* Start the scan. */
        manager.startScan(1, terminal -> {
            if (mCardStateMonitor.isContainsTerminal(terminal))
                return;

            TerminalTimeouts timeouts = manager.getTimeouts(terminal);
            timeouts.setConnectionTimeout(TerminalTimeouts.DEFAULT_TIMEOUT);
            timeouts.setPowerTimeout(TerminalTimeouts.DEFAULT_TIMEOUT);
            timeouts.setProtocolTimeout(TerminalTimeouts.DEFAULT_TIMEOUT);
            timeouts.setApduTimeout(TerminalTimeouts.DEFAULT_TIMEOUT);
            timeouts.setControlTimeout(TerminalTimeouts.DEFAULT_TIMEOUT);

            mCardStateMonitor.addTerminal(terminal);

            AcsDeviceCustomRfidActivity.this.mTerminal = terminal;
        });

        /* Initialize the card state monitor. */
        mCardStateMonitor = CardStateMonitor.getInstance();
        mCardStateMonitor.setOnStateChangeListener((monitor, terminal, prevState, currState) -> {
            if ((prevState > CardStateMonitor.CARD_STATE_ABSENT)
                    && (currState <= CardStateMonitor.CARD_STATE_ABSENT)) {
                Log.d(TAG, terminal.getName() + ": removed");

                if (nfcTag != null) {
                    nfcTag.cardAbsent();
                    nfcTag = null;
                }
//                    mLogger.logMsg(terminal.getName() + ": removed");
            } else if ((prevState <= CardStateMonitor.CARD_STATE_ABSENT)
                    && (currState > CardStateMonitor.CARD_STATE_ABSENT)) {
                Log.d(TAG, terminal.getName() + ": inserted");
                new Thread(() -> {
                    CardChannel channel = null;
                    try {
                        Card card = terminal.connect("*");
                        channel = card.getBasicChannel();
                    } catch (CardException e) {
                        e.printStackTrace();
                    }

                    nfcTag = new NfcDeviceTag(channel);

                    runOnUiThread(this::handleNFCIntent);
                }).start();
            }
        });
    }

    private void disconnectDevice() {
        manager.stopScan();
        new Thread(() -> {

            /* Remove the terminal from card state monitor. */
            mCardStateMonitor.removeTerminal(mTerminal);

            /* Disconnect from the terminal. */
            if (mTerminal != null)
                manager.disconnect(mTerminal);
        }).start();
    }

    private void handleNFCIntent() {
        if (nfcTag == null)
            return;

        HANDLER.removeCallbacks(retryRunnable);

        Log.d(TAG, "Chip reading started!");

        Log.d(TAG, "Start read RFID");
        rfidStatus.setText(R.string.strReadingRFID);
        rfidStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));

        startRfidReading(nfcTag);
    }

    @Override
    protected void completeActivity() {
        disconnectDevice();
        super.completeActivity();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        /* User chose not to enable Bluetooth. */
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED){
                finish();
                return;
            }
            initBle();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == REQUEST_ACCESS_FINE_LOCATION) {

            if ((grantResults.length > 0)
                    && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {

                initBle();
            }

        } else {

            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}

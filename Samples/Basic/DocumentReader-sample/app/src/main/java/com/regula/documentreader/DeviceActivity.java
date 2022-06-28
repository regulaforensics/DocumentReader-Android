package com.regula.documentreader;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.regula.documentreader.api.DocumentReader;
import com.regula.documentreader.api.ble.BLEWrapper;
import com.regula.documentreader.api.ble.BleWrapperCallback;
import com.regula.documentreader.api.ble.RegulaBleService;
import com.regula.documentreader.api.ble.callback.BleManagerCallback;
import com.regula.documentreader.api.params.Device7310Config;
import com.regula.documentreader.util.BluetoothUtil;

public class DeviceActivity extends BaseActivity {
    private BLEWrapper bleManager;
    private boolean isBleServiceConnected;
    private RelativeLayout deviceLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EditText etDeviceName = findViewById(R.id.ed_device);
        Button btnConnect = findViewById(R.id.btn_connect);
        deviceLayout = findViewById(R.id.device_layout);
        etDeviceName.setText(DocumentReader.Instance().functionality().getBtDeviceName());
        btnConnect.setOnClickListener(view -> {
            if (etDeviceName.getText() != null) {
                showDialog("Searching devices");
                handler.sendEmptyMessageDelayed(0, 7000);
                DocumentReader.Instance().functionality().edit().setBtDeviceName(etDeviceName.getText().toString()).apply();
                startBluetoothService();
            }
        });
        setMenuVisibility(false);
    }

    @Override
    protected void initializeReader() {
        showDialog("Initializing");
        setMenuVisibility(true);
        if (bleManager != null) {
            DocumentReader.Instance().initializeReader(DeviceActivity.this, new Device7310Config(bleManager), initCompletion);
        } else {
            dismissDialog();
            Toast.makeText(this, "Error reading license from device", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPrepareDbCompleted() {
        dismissDialog();

        if (isBleServiceConnected && bleManager != null && bleManager.isConnected() && !DocumentReader.Instance().isReady()) {
            initializeReader();
            return;
        }
        setMenuVisibility(false);
        deviceLayout.setVisibility(View.VISIBLE);
    }

    private void setMenuVisibility(boolean visibility) {
        deviceLayout.setVisibility(visibility ? View.GONE : View.VISIBLE);
        fragmentContainer.setVisibility(visibility ? View.VISIBLE : View.GONE);
        setActionBarVisibility(visibility);
    }

    private void setActionBarVisibility(boolean isVisible) {
        if (getSupportActionBar() != null) {
            if (isVisible)
                getSupportActionBar().show();
            else
                getSupportActionBar().hide();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DocumentReader.Instance().isReady()) {
            setMenuVisibility(true);
        }
    }

    private void startBluetoothService() {

        if (isBleServiceConnected && bleManager != null && bleManager.isConnected() && !DocumentReader.Instance().isReady()) {
            initializeReader();
            return;
        }

        if (!BluetoothUtil.isPermissionsGranted(this) || isBleServiceConnected) {
            return;
        }

        Intent bleIntent = new Intent(this, RegulaBleService.class);
        startService(bleIntent);
        bindService(bleIntent, mBleConnection, 0);
    }

    private final ServiceConnection mBleConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            showDialog("Searching devices");
            isBleServiceConnected = true;
            RegulaBleService bleService = ((RegulaBleService.LocalBinder) service).getService();
            bleManager = bleService.getBleManager();
            if (bleManager.isConnected()) {
                initializeReader();
                return;
            }
            handler.sendEmptyMessageDelayed(0, 7000);
            bleManager.addCallback(bleManagerCallbacks);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBleServiceConnected = false;
        }
    };

    private final Handler handler = new Handler(msg -> {
        Toast.makeText(this, "Failed to connect to the torch device", Toast.LENGTH_SHORT).show();
        dismissDialog();
        return false;
    });

    private final BleManagerCallback bleManagerCallbacks = new BleWrapperCallback() {
        @Override
        public void onDeviceReady() {
            handler.removeMessages(0);
            bleManager.removeCallback(bleManagerCallbacks);
            initializeReader();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBleServiceConnected) {
            unbindService(mBleConnection);
            isBleServiceConnected = false;
        }
    }
}
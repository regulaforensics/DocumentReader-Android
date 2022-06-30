package com.regula.documentreader.custom;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.regula.documentreader.BaseActivity;

import com.regula.documentreader.R;
import com.regula.documentreader.api.DocumentReader;
import com.regula.documentreader.api.completions.IDocumentReaderCompletion;
import com.regula.documentreader.api.enums.DocReaderAction;
import com.regula.documentreader.api.enums.eRFID_DataFile_Type;
import com.regula.documentreader.api.enums.eRFID_ErrorCodes;
import com.regula.documentreader.api.enums.eRFID_NotificationCodes;
import com.regula.documentreader.api.errors.DocumentReaderException;
import com.regula.documentreader.api.results.DocumentReaderResults;


public class CustomRfidActivity extends AppCompatActivity {

    private static String TAG = "CustomRfidActivity";

    protected Handler HANDLER = new Handler(Looper.getMainLooper());

    private NfcAdapter nfcAdapter;

    TextView rfidStatus;
    TextView currentRfidDgTv;
    LinearLayout currentDataGroupLt;

    Runnable retryRunnable = new Runnable() {
        @Override
        public void run() {
            retry();
        }
    };

    private final BroadcastReceiver nfcActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action != null && action.equals(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)) {
                checkAdapterEnabled();
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_custom_rfid);

        currentDataGroupLt = findViewById(R.id.currentDataGroupLt);
        currentRfidDgTv = findViewById(R.id.currentRfidDgTv);
        rfidStatus = findViewById(R.id.rfidStatus);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (nfcAdapter == null) {
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAdapterEnabled();

        if (nfcAdapter == null)
            return;

        try {
            IntentFilter filter = new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
            this.registerReceiver(nfcActionReceiver, filter);
        } catch (Exception ex) {
            //android broadcast adding error
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (nfcAdapter == null)
            return;

        stopForegroundDispatch(CustomRfidActivity.this, nfcAdapter);

        try {
            this.unregisterReceiver(nfcActionReceiver);
        } catch (Exception ex) {
            //android not providing any api to check if is registered
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent");
        String action = intent.getAction();
        if (action != null && action.equals(NfcAdapter.ACTION_TECH_DISCOVERED)) {
            handleNFCIntent(intent);
        }
    }

    private void checkAdapterEnabled() {
        if (nfcAdapter == null)
            return;
        if (nfcAdapter.isEnabled() && DocumentReader.Instance().isReady()) {
            setupForegroundDispatch(CustomRfidActivity.this, nfcAdapter);
        } else {
            Log.e(TAG, "NFC is not enabled");
        }
    }

    /**
     * @param activity The corresponding {@link Activity} requesting the foreground dispatch.
     * @param adapter  The {@link NfcAdapter} used for the foreground dispatch.
     */
    @SuppressLint("MissingPermission")
    private void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());


        int flag = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flag = PendingIntent.FLAG_MUTABLE;
        }
        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, flag);

        IntentFilter[] filters = new IntentFilter[1];

        // Notice that this is the same filter as in our manifest.
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_TECH_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);

        String[][] techList = new String[][]{
                new String[]{"android.nfc.tech.IsoDep"}
        };

        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
        Log.d(TAG, "NFC adapter dispatch enabled for android.nfc.tech.IsoDep");
    }

    /**
     * @param activity The corresponding {@link Activity} requesting to stop the foreground dispatch.
     * @param adapter  The {@link NfcAdapter} used for the foreground dispatch.
     */
    @SuppressLint("MissingPermission")
    private void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        if (adapter != null) {
            adapter.disableForegroundDispatch(activity);
            Log.d(TAG, "NFC adapter dispatch disabled");
        }
    }

    private void handleNFCIntent(final Intent intent) {
        Log.d(TAG, "Intent received: NfcAdapter.ACTION_TECH_DISCOVERED");

        HANDLER.removeCallbacks(retryRunnable);

        String action = intent.getAction();
        if (action == null || !action.equals(NfcAdapter.ACTION_TECH_DISCOVERED)) {
            return;
        }

        Log.d(TAG, "Chip reading started!");

        Tag nfcTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        Log.d(TAG, "nfcTag extracted from NfcAdapter.EXTRA_TAG");
        if (nfcTag == null) {
            Log.e(TAG, "NFC tag is null");
            return;
        }

        IsoDep isoDepTag = IsoDep.get(nfcTag);
        Log.d(TAG, "IsoDep.get(nfcTag) successful");
        if (isoDepTag == null) {
            return;
        }

        Log.d(TAG, "Start read RFID");
        rfidStatus.setText(R.string.strReadingRFID);
        rfidStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        DocumentReader.Instance().readRFID(isoDepTag, new IDocumentReaderCompletion() {
            @Override
            public void onCompleted(int rfidAction, DocumentReaderResults documentReaderResults, DocumentReaderException error) {
                if (rfidAction == DocReaderAction.COMPLETE) {
                    // Completed rfid reading
                    BaseActivity.documentReaderResults = documentReaderResults;
                    if (documentReaderResults.rfidResult == 0x00000001) {
                        setResult(RESULT_OK);
                        rfidStatus.setText(CustomRfidActivity.this.getString(R.string.RSDT_RFID_READING_FINISHED));
                        rfidStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                        HANDLER.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                finish();
                            }
                        }, 2500);
                    } else {
                        String builder = getString(R.string.RFID_Error_Failed) +
                                "\n" +
                                eRFID_ErrorCodes.getTranslation(CustomRfidActivity.this, documentReaderResults.rfidResult);
                        rfidStatus.setText(builder);
                        rfidStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        HANDLER.postDelayed(retryRunnable, getResources().getInteger(R.integer.reg_rfid_activity_error_timeout));
                    }
                    currentDataGroupLt.setVisibility(View.GONE);
                } else if (rfidAction == DocReaderAction.NOTIFICATION) {
                    HANDLER.post(new Runnable() {
                        @Override
                        public void run() {
                            if (currentDataGroupLt.getVisibility() == View.GONE) {
                                currentDataGroupLt.setVisibility(View.VISIBLE);
                                rfidStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
                            }
                        }
                    });
                    rfidProgress(documentReaderResults.documentReaderNotification.code, documentReaderResults.documentReaderNotification.value);
                } else if (rfidAction == DocReaderAction.ERROR) {
                    String builder = getString(R.string.RFID_Error_Failed) +
                            "\n" +
                            eRFID_ErrorCodes.getTranslation(CustomRfidActivity.this, documentReaderResults.rfidResult);
                    rfidStatus.setText(builder);
                    rfidStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    currentDataGroupLt.setVisibility(View.GONE);
                    Log.e(TAG, "Error: " + error);
                }
            }
        });
    }

    public void rfidProgress(int code, int value) {
        int hiword = code & 0xFFFF0000;
        final int loword = code & 0x0000FFFF;

        switch (hiword) {
            case eRFID_NotificationCodes.RFID_NOTIFICATION_PCSC_READING_DATAGROUP:
                if (value == 0) {
                    HANDLER.post(new Runnable() {
                        @Override
                        public void run() {
                            currentRfidDgTv.setText(String.format(getString(R.string.strReadingRFIDDG), eRFID_DataFile_Type.getTranslation(getApplicationContext(), loword)));
                        }
                    });
                }
                break;
        }
    }

    void retry() {
        rfidStatus.setText(R.string.strPlacePhoneOnDoc);
        rfidStatus.setTextColor(getResources().getColor(android.R.color.black));
    }

    public void skipReadRfid(View view) {
        DocumentReader.Instance().stopRFIDReader(getApplicationContext());
        setResult(RESULT_CANCELED);
        finish();
    }
}

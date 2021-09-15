package com.regula.documentreader.custom.rfid;

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
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Nullable;

import com.regula.documentreader.api.DocumentReader;
import com.regula.documentreader.api.nfc.IsoDepUniversalWrapper;


public class CustomRfidActivity extends BaseCustomRfiActivity {

    private static final String TAG = "CustomRfidActivity";

    private NfcAdapter nfcAdapter;

    private final BroadcastReceiver nfcActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action!=null && action.equals(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)) {
                checkAdapterEnabled();
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if(nfcAdapter == null) {
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
        } catch (Exception ex){
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
        } catch(Exception ex) {
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

        if (nfcAdapter.isEnabled() && DocumentReader.Instance().getDocumentReaderIsReady()) {
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

        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);

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

        startRfidReading(new IsoDepUniversalWrapper(isoDepTag));
    }
}

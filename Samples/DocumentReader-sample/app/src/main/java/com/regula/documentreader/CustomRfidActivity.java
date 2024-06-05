package com.regula.documentreader;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.regula.documentreader.api.DocumentReader;
import com.regula.documentreader.api.completions.rfid.IRfidCompletion;
import com.regula.documentreader.api.enums.DocReaderAction;
import com.regula.documentreader.api.enums.eRFID_DataFile_Type;
import com.regula.documentreader.api.enums.eRFID_ErrorCodes;
import com.regula.documentreader.api.enums.eRFID_NotificationCodes;
import com.regula.documentreader.api.errors.DocumentReaderException;
import com.regula.documentreader.api.nfc.PCSCTag;
import com.regula.documentreader.api.pcsc.PCSCWrapper;
import com.regula.documentreader.api.pcsc.callback.PCSCCallback;
import com.regula.documentreader.api.results.DocumentReaderNotification;
import com.regula.documentreader.api.results.DocumentReaderResults;


public class CustomRfidActivity extends AppCompatActivity {

    private static String TAG = "CustomRfidActivity";

    protected Handler HANDLER = new Handler(Looper.getMainLooper());

    TextView rfidStatus;
    TextView currentRfidDgTv;
    LinearLayout currentDataGroupLt;

    PCSCWrapper pcscReader;
    PCSCTag pcscTag;

    Runnable retryRunnable = new Runnable() {
        @Override
        public void run() {
            retry();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_custom_rfid);

        currentDataGroupLt = findViewById(R.id.currentDataGroupLt);
        currentRfidDgTv = findViewById(R.id.currentRfidDgTv);
        rfidStatus = findViewById(R.id.rfidStatus);

        pcscReader = new PCSCWrapper(this, pcscReaderCallback);
        pcscTag = new PCSCTag(pcscReader);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pcscReader != null) {
            pcscReader.removeAllCallbacks();
            pcscReader.disconnect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!pcscReader.isConnected()) {
            pcscReader.addCallback(pcscReaderCallback);
            pcscReader.connect();
        }
    }

    private void startReadRfid() {
        Log.d(TAG, "Start read RFID");
        rfidStatus.setText(R.string.strReadingRFID);
        rfidStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        DocumentReader.Instance().readRFID(pcscTag, new IRfidCompletion() {
            @Override
            public void onCompleted(int rfidAction, @Nullable DocumentReaderResults documentReaderResults, @Nullable DocumentReaderException error) {
                if (rfidAction == DocReaderAction.COMPLETE) {
                    // Completed rfid reading
                    MainActivity.documentReaderResults = documentReaderResults;
                    if (documentReaderResults.rfidResult == 0x00000001) {
                        setResult(RESULT_OK);
                        rfidStatus.setText(CustomRfidActivity.this.getString(R.string.RSDT_RFID_READING_FINISHED));
                        rfidStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                        HANDLER.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                setResult(RESULT_OK);
                                finish();
                            }
                        }, 2500);
                    } else {
                        int errorCode = documentReaderResults.rfidResult;

                        String builder = getString(R.string.RFID_Error_Failed) +
                                "\n" +
                                eRFID_ErrorCodes.getTranslation(CustomRfidActivity.this, errorCode);
                        rfidStatus.setText(builder);
                        rfidStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        HANDLER.postDelayed(retryRunnable, getResources().getInteger(R.integer.reg_rfid_activity_error_timeout));
                    }
                    currentDataGroupLt.setVisibility(View.GONE);
                }
            }

            @Override
            public void onProgress(@NonNull DocumentReaderNotification notification) {
                super.onProgress(notification);
                HANDLER.post(new Runnable() {
                    @Override
                    public void run() {
                        if(currentDataGroupLt.getVisibility() == View.GONE) {
                            currentDataGroupLt.setVisibility(View.VISIBLE);
                            rfidStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
                        }
                    }
                });
                rfidProgress(notification.code, notification.value);
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
        DocumentReader.Instance().stopRFIDReader(this);
        setResult(RESULT_CANCELED);
        finish();
    }

    private PCSCCallback pcscReaderCallback = new PCSCCallback() {
        @Override
        public void onConnected() {
        }

        @Override
        public void onDisconnected() {
        }

        @Override
        public void onNotFoundDevice() {

        }

        @Override
        public void onCardStatusChanged(byte[] bytes, boolean isPresent) {
            Log.d(TAG, "Card status: present=" + isPresent);
            if (isPresent) {
                HANDLER.post(() -> startReadRfid());
            } else {
                pcscTag.cardAbsent();
            }
        }

        @Override
        public void onReceivedATRResponse(byte[] bytes) {

        }

        @Override
        public void onParametersResponse(byte[] bytes) {

        }

        @Override
        public void onReceivedAPDUResponse(byte[] bytes) {

        }
    };
}

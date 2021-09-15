package com.regula.documentreader.custom.rfid;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.regula.documentreader.R;
import com.regula.documentreader.SharedDocReaderResults;
import com.regula.documentreader.api.DocumentReader;
import com.regula.documentreader.api.enums.DocReaderAction;
import com.regula.documentreader.api.enums.eRFID_DataFile_Type;
import com.regula.documentreader.api.enums.eRFID_NotificationAndErrorCodes;
import com.regula.documentreader.api.nfc.IUniversalNfcTag;

/**
 * Created by Sergey Yakimchik on 15.09.21.
 * Copyright (c) 2021 Regula. All rights reserved.
 */

abstract class BaseCustomRfiActivity extends AppCompatActivity {

    private static String TAG = "BaseCustomRfiActivity";

    protected Handler HANDLER = new Handler(Looper.getMainLooper());

    TextView rfidStatus;
    TextView currentRfidDgTv;
    LinearLayout currentDataGroupLt;
    ImageButton skipRfidBtn;

    Runnable retryRunnable = this::retry;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_custom_rfid);

        currentDataGroupLt = findViewById(R.id.currentDataGroupLt);
        currentRfidDgTv = findViewById(R.id.currentRfidDgTv);
        rfidStatus = findViewById(R.id.rfidStatus);
        skipRfidBtn = findViewById(R.id.skipRfidBtn);

        skipRfidBtn.setOnClickListener(v -> {
            completeActivity();
        });
    }

    protected void rfidProgress(int code, int value) {
        int hiword = code & 0xFFFF0000;
        final int loword = code & 0x0000FFFF;

        switch (hiword) {
            case eRFID_NotificationAndErrorCodes.RFID_NOTIFICATION_PCSC_READING_DATAGROUP:
                if (value == 0) {
                    HANDLER.post(() -> currentRfidDgTv.setText(String.format(getString(R.string.strReadingRFIDDG), eRFID_DataFile_Type.getTranslation(getApplicationContext(), loword))));
                }
                break;
        }
    }

    protected void retry() {
        rfidStatus.setText(R.string.strPlacePhoneOnDoc);
        rfidStatus.setTextColor(getResources().getColor(android.R.color.black));
    }

    public void skipReadRfid(View view) {
        DocumentReader.Instance().stopRFIDReader(getApplicationContext());
        setResult(RESULT_CANCELED);
        completeActivity();
    }

    protected void completeActivity() {
        finish();
    }

    protected void startRfidReading(IUniversalNfcTag tag) {
        Log.d(TAG, "Start read RFID");
        rfidStatus.setText(R.string.strReadingRFID);
        rfidStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        DocumentReader.Instance().readRFID(tag, (rfidAction, documentReaderResults, error) -> {
            if(rfidAction == DocReaderAction.COMPLETE) {
                // Completed rfid reading
                SharedDocReaderResults.documentReaderResults = documentReaderResults;
                if (documentReaderResults.rfidResult == 0x00000001) {
                    setResult(RESULT_OK);
                    rfidStatus.setText(BaseCustomRfiActivity.this.getString(R.string.RSDT_RFID_READING_FINISHED));
                    rfidStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    HANDLER.postDelayed(this::completeActivity, 2500);
                } else {
                    String builder = getString(R.string.RFID_Error_Failed) +
                            "\n" +
                            eRFID_NotificationAndErrorCodes.getTranslation(BaseCustomRfiActivity.this, documentReaderResults.rfidResult);
                    rfidStatus.setText(builder);
                    rfidStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    HANDLER.postDelayed(retryRunnable, getResources().getInteger(R.integer.reg_rfid_activity_error_timeout));
                }
                currentDataGroupLt.setVisibility(View.GONE);
            } else if(rfidAction == DocReaderAction.NOTIFICATION) {
                HANDLER.post(() -> {
                    if(currentDataGroupLt.getVisibility() == View.GONE) {
                        currentDataGroupLt.setVisibility(View.VISIBLE);
                        rfidStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
                    }
                });
                if (documentReaderResults != null && documentReaderResults.documentReaderNotification != null)
                    rfidProgress(documentReaderResults.documentReaderNotification.code, documentReaderResults.documentReaderNotification.value);
            } else if(rfidAction == DocReaderAction.ERROR) {
                String builder = getString(R.string.RFID_Error_Failed) +
                        "\n" +
                        eRFID_NotificationAndErrorCodes.getTranslation(BaseCustomRfiActivity.this, documentReaderResults.rfidResult);
                rfidStatus.setText(builder);
                rfidStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                currentDataGroupLt.setVisibility(View.GONE);
                Log.e(TAG, "Error: " + error);
            }
        });
    }
}

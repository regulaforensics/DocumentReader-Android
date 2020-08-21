package com.regula.documentreader;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.regula.common.http.RequestResponseData;
import com.regula.documentreader.api.BaseActivity;
import com.regula.documentreader.api.DocumentReader;
import com.regula.documentreader.api.RegDeviceService;
import com.regula.documentreader.api.completions.IDocumentReaderCompletion;
import com.regula.documentreader.api.enums.DocReaderAction;
import com.regula.documentreader.api.results.DocumentReaderResults;

public class CustomDeviceActivity extends AppCompatActivity {

    private static final String TAG = "CustomDeviceActivity";

    ImageView view;

    private ProgressBar mLoadingBar;
    private TextView statusTxt;

    private final int SKIP_FRAME_COUNT = 5;
    private int currentFrameCount = 0;

    protected Handler HANDLER = new Handler(Looper.getMainLooper());

    boolean isStartScan = false;

    private RegDeviceService mRegulaService;

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mRegulaService = ((RegDeviceService.LocalBinder)service).getService();

            mRegulaService.setCallback(callback);
            mRegulaService.setContinuousMode(DocumentReader.Instance().functionality().isRegDeviceContinuesMode());
            deviceConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mRegulaService = null;
        }
    };

    private RegDeviceService.RegDeviceServiceCallback callback = new RegDeviceService.RegDeviceServiceCallback() {

        @Override
        public void onFrame(final byte[] frame) {
            final Bitmap bitmap = BitmapFactory.decodeByteArray(frame, 0, frame.length);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    view.setImageBitmap(bitmap);
                }
            });

            if (!DocumentReader.Instance().functionality().isRegDeviceContinuesMode() || currentFrameCount++ > SKIP_FRAME_COUNT) {
                final Bitmap tempBitmap = BitmapFactory.decodeByteArray(frame, 0, frame.length);
                recognizeImage(tempBitmap);
            }
        }

        @Override
        public void onDeviceConnected() {
            deviceConnected();
        }

        @Override
        public void onDeviceDisconnected() {
            HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    mLoadingBar.setVisibility(View.GONE);
                    statusTxt.setText("Device is not connected");
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_reg_device_activity);

        view = findViewById(com.regula.documentreader.api.R.id.imageView);

        statusTxt = findViewById(com.regula.documentreader.api.R.id.statusTxt);
        statusTxt.setText("Connecting to service...");

        mLoadingBar = findViewById(com.regula.documentreader.api.R.id.loadingPb);

        mLoadingBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(CustomDeviceActivity.this, RegDeviceService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mRegulaService != null) {
            mRegulaService.setCallback(null);
            unbindService(mConnection);
        }
    }

    public void onLedButtonClick(View view) {
        if (mRegulaService == null)
            return;

        if(mRegulaService.isLedOn()) {
            mRegulaService.turnOffLed();
        } else {
            mRegulaService.turnOnLed();
        }

        ((ImageButton)view).setImageResource(mRegulaService.isLedOn() ? com.regula.documentreader.api.R.drawable.reg_flash_on: com.regula.documentreader.api.R.drawable.reg_flash_off);
    }

    public void onCloseClick(View view) {
        finish();
    }

    private void recognizeImage(Bitmap bitmap) {
        if (isStartScan)
            return;

        isStartScan = true;

        HANDLER.post(new Runnable() {
            @Override
            public void run() {
                mLoadingBar.setVisibility(View.VISIBLE);
            }
        });
        DocumentReader.Instance().recognizeImage(bitmap, new IDocumentReaderCompletion() {
            @Override
            public void onCompleted(int action, DocumentReaderResults results, Throwable error) {
                if (action != DocReaderAction.COMPLETE) {
                    Log.d(TAG, "Completed recognize");
                    return;
                }

                mRegulaService.turnOffLed();

                MainActivity.documentReaderResults = results;
                HANDLER.post(new Runnable() {
                    @Override
                    public void run() {
                        mLoadingBar.setVisibility(View.GONE);
                        setResult(RESULT_OK);
                        finish();
                    }
                });
            }
        });
    }

    private void deviceConnected() {
        HANDLER.post(new Runnable() {
            @Override
            public void run() {
                mLoadingBar.setVisibility(View.GONE);
                if (mRegulaService.isRunning()) {
                    if (DocumentReader.Instance().functionality().isRegDeviceContinuesMode()) {
                        statusTxt.setText("Device is ready");
                        onLedButtonClick(findViewById(com.regula.documentreader.api.R.id.lightBtn));
                    } else {
                        statusTxt.setText("Waiting motion...");
                    }
                } else {
                    statusTxt.setText("Device is not running");
                }
            }
        });
    }
}

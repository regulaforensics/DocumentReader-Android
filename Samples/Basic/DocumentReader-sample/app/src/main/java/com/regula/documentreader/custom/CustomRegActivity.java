package com.regula.documentreader.custom;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import com.regula.common.CameraCallbacks;
import com.regula.common.CameraFragment;
import com.regula.common.RegCameraFragment;
import com.regula.common.enums.CommonKeys;
import com.regula.documentreader.R;
import com.regula.documentreader.api.CaptureActivity3;
import com.regula.documentreader.api.DocumentReader;
import com.regula.documentreader.api.enums.DocReaderAction;
import com.regula.documentreader.api.enums.eVisualFieldType;
import com.regula.documentreader.api.params.ImageInputParam;

/**
 * Created by Sergey Yakimchik on 10/13/20.
 * Copyright (c) 2020 Regula. All rights reserved.
 */

public class CustomRegActivity extends CaptureActivity3 implements CameraCallbacks {

    private static final String DEBUG = "DEBUG";
    private static final String FRAGMENT_TAG = "cameraFragmentTag";
    private static final int PERMISSIONS_CAMERA = 1100;

    private static final Object lock = new Object();

    private OrientationEventListener orientationEventListener;

    private RegCameraFragment cameraFragment;

    private int mCurrentDegrees;
    private boolean recognitionFinished = true;
    private boolean isPauseRecognize = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ContextCompat.checkSelfPermission(CustomRegActivity.this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(DEBUG, "OnResume: Asking permissions");
            ActivityCompat.requestPermissions(CustomRegActivity.this,
                    new String[]{Manifest.permission.CAMERA}, PERMISSIONS_CAMERA);
        } else {
            Log.d(DEBUG, "OnResume: Permissions granted");

            safeCameraOpenInView();
        }

        DocumentReader.Instance().startNewSession();

        setContentView(R.layout.activity_custom_reg);

        int currentOrientation = getResources().getConfiguration().orientation;
        mCurrentDegrees = currentOrientation == Configuration.ORIENTATION_PORTRAIT ? 0 : 90;

        orientationEventListener = new OrientationEventListener(CustomRegActivity.this) {
            @Override
            public void onOrientationChanged(int orientation) {
                orientationChanged(orientation);
            }
        };
    }

    @Override
    protected void onPause() {
        super.onPause();
        orientationEventListener.disable();
    }

    private void safeCameraOpenInView() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        cameraFragment = (RegCameraFragment) fragmentManager.findFragmentByTag(FRAGMENT_TAG);
        if (cameraFragment == null) {
            cameraFragment = new CameraFragment();
//            cameraFragment = new Camera2Fragment();

            Bundle args = new Bundle();
            args.putInt(CommonKeys.CAMERA_ID, 0);
            cameraFragment.setArguments(args);
            fragmentManager.beginTransaction().add(R.id.cameraUi, cameraFragment, FRAGMENT_TAG).commit();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_CAMERA) {
            if (grantResults.length == 0
                    || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                CustomRegActivity.this.finish();
            } else {
                safeCameraOpenInView();
            }
        }
    }

    public void onClickClosed(View view) {
        finish();
    }

    @Override
    public void onCameraOpened(boolean b) {
        orientationEventListener.enable();
    }

    @Override
    public void onFrame(byte[] frame) {
        //Filling the params with appropriate values
        if (!recognitionFinished || isPauseRecognize) {
            //if already completed - ignore, results won't change
            return;
        }
        recognitionFinished = false;

        ImageInputParam params = new ImageInputParam(cameraFragment.getPreviewWidth(), cameraFragment.getPreviewHeight(), cameraFragment.getFrameFormat());
        if (cameraFragment.getCameraFacing() == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            params.rotation = (mCurrentDegrees + cameraFragment.getCameraOrientation()) % 360;
        } else {
            params.rotation = mCurrentDegrees - cameraFragment.getCameraOrientation();
        }
        recognizeFrame(frame, params);
    }

    private void recognizeFrame(byte[] frame, ImageInputParam params) {
        DocumentReader.Instance().recognizeVideoFrame(frame, params, (action, results, error) -> {
            switch (action) {
                case DocReaderAction.COMPLETE: //all done, no more frames required, results won't change
                    synchronized (lock) {
                        if (results != null
                                && results.morePagesAvailable == 0)
                            isPauseRecognize = true;
                    }
                    if (results != null && results.morePagesAvailable != 0) { //more pages are available for this document
                        Toast.makeText(CustomRegActivity.this, "Page ready, flip", Toast.LENGTH_LONG).show();

                        //letting API know, that all frames will be from different page of the same document, merge same field types
                        DocumentReader.Instance().startNewPage();
//                            mPreview.startCameraPreview();
                    } else { //no more pages available
                        AlertDialog.Builder builder = new AlertDialog.Builder(CustomRegActivity.this);
                        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                DocumentReader.Instance().startNewSession();
                                dialogInterface.dismiss();
                                synchronized (lock) {
                                    isPauseRecognize = false;
                                }
                            }
                        });
                        builder.setTitle("Processing finished");
                        //getting text field value from results
                        builder.setMessage(results == null ? "Empty results" : results.getTextFieldValueByType(eVisualFieldType.FT_SURNAME_AND_GIVEN_NAMES));
                        builder.show();
                    }
                    break;
                case DocReaderAction.ERROR: //something went wrong
                    isPauseRecognize = true;
                    Toast.makeText(CustomRegActivity.this, "Error: " + (error != null ? error.getMessage() : ""), Toast.LENGTH_LONG).show();
                    break;
            }

            recognitionFinished = true;
        });
    }

    private void orientationChanged(int orientation) {
        synchronized (lock) {
            int degrees;

            if (orientation > 315 && orientation <= 360 || orientation >= 0 && orientation <= 45) {
                degrees = 0;
            } else if (orientation > 45 && orientation <= 135) {
                degrees = -90;
            } else if (orientation > 135 && orientation <= 225) {
                degrees = 0;
            } else if (orientation > 255 && orientation <= 315) {
                degrees = 90;
            } else {
                degrees = mCurrentDegrees;
            }

            if (degrees != mCurrentDegrees) {
                mCurrentDegrees = degrees;
            }
        }
    }
}

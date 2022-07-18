package com.regula.documentreader.custom;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.util.Size;
import android.view.Surface;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.regula.common.utils.CameraUtil;
import com.regula.documentreader.R;
import com.regula.documentreader.api.DocumentReader;
import com.regula.documentreader.api.enums.DocReaderAction;
import com.regula.documentreader.api.enums.eVisualFieldType;
import com.regula.documentreader.api.params.ImageInputParam;
import com.regula.documentreader.api.results.DocumentReaderResults;

public class Camera2Activity extends AppCompatActivity implements ImageReader.OnImageAvailableListener {
    private boolean isProcessingFrame = false;
    private Runnable postInferenceCallback;
    private int sensorOrientation;
    int previewHeight = 0, previewWidth = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2);
        setFragment();
        //let API know, that all previous results should be disposed
        DocumentReader.Instance().startNewSession();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setFragment();
        } else {
            finish();
        }
    }

    protected void setFragment() {
        final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        String cameraId = null;
        try {
            cameraId = manager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        CameraConnectionFragment camera2Fragment =
                CameraConnectionFragment.newInstance(
                        (size, cameraRotation) -> {
                                previewHeight = size.getHeight();
                                previewWidth = size.getWidth();
                                sensorOrientation = cameraRotation - getScreenOrientation();
                        },
                        this,
                        R.layout.camera2_fragment,
                        new Size(1920, 1080));//setup preview size
        camera2Fragment.setCamera(cameraId);
        getSupportFragmentManager().beginTransaction().replace(R.id.container, camera2Fragment).commit();
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        // We need wait until we have some size from onPreviewSizeChosen
        if (previewWidth == 0 || previewHeight == 0) {
            return;
        }
        try {
            final Image image = reader.acquireLatestImage();
            if (image == null) {
                return;
            }
            if (!isProcessingFrame) {
                byte[] data = CameraUtil.convertYUV420888ToNV21(image);
                ImageInputParam params = new ImageInputParam(previewWidth, previewHeight, ImageFormat.NV21);
                DocumentReader.Instance().recognizeVideoFrame(data, params, (i, documentReaderResults, throwable) -> {
                    switch (i) {
                        case DocReaderAction.COMPLETE: //all done, no more frames required, results won't change
                            if (documentReaderResults != null
                                    && documentReaderResults.morePagesAvailable == 0)
                                isProcessingFrame = true;

                            if (documentReaderResults != null) {
                                if (documentReaderResults.morePagesAvailable != 0) { //more pages are available for this document
                                    Toast.makeText(Camera2Activity.this, "Page ready, flip", Toast.LENGTH_LONG).show();
                                    //letting API know, that all frames will be from different page of the same document, merge same field types
                                    DocumentReader.Instance().startNewPage();
                                } else { //no more pages available
                                    startDialog(documentReaderResults);
                                }
                            }
                            isProcessingFrame = false;
                            break;
                        case DocReaderAction.ERROR: //something went wrong
                            isProcessingFrame = false;
                            Toast.makeText(Camera2Activity.this, "Error: " + (throwable != null ? throwable.getMessage() : ""), Toast.LENGTH_LONG).show();
                            break;
                        case DocReaderAction.CANCEL:
                            break;
                    }
                });
            }
            postInferenceCallback =
                    () -> image.close();
            processImage();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }


    private void processImage() {
        postInferenceCallback.run();
    }

    protected int getScreenOrientation() {
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }

    private void startDialog(DocumentReaderResults documentReaderResults) {
        AlertDialog.Builder builder = new AlertDialog.Builder(Camera2Activity.this);
        builder.setPositiveButton("Ok", (dialogInterface, i1) -> {
            DocumentReader.Instance().startNewSession();
            dialogInterface.dismiss();
            isProcessingFrame = false;
        });
        builder.setTitle("Processing finished");
        //getting text field value from results
        builder.setMessage(documentReaderResults.getTextFieldValueByType(eVisualFieldType.FT_SURNAME_AND_GIVEN_NAMES));
        builder.show();
    }
}
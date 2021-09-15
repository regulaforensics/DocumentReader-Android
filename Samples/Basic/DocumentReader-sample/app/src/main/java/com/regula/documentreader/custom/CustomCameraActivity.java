package com.regula.documentreader.custom;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.regula.documentreader.R;
import com.regula.documentreader.api.DocumentReader;
import com.regula.documentreader.api.enums.DocReaderAction;
import com.regula.documentreader.api.enums.eVisualFieldType;
import com.regula.documentreader.api.params.ImageInputParam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;

public class CustomCameraActivity extends AppCompatActivity implements Camera.PreviewCallback {

    private static final String DEBUG = "DEBUG";
    private static final Object lock = new Object();
    private static final int PERMISSIONS_CAMERA = 1100;
    private int mCameraId;
    private Camera mCamera;
    private Camera.Parameters mParams;
    private CameraPreview mPreview;
    private RelativeLayout previewParent;
    private boolean isPreviewReady;
    private Camera.Size previewSize;
    private int cameraOrientation;
    private int previewFormat;
    private boolean recognitionFinished = true;
    private boolean isPauseRecognize = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

//        DocumentReader.Instance().processParams().scenario = "FullProcess";
//        DocumentReader.Instance().processParams().multipageProcessing = false;
        //let API know, that all previous results should be disposed
        DocumentReader.Instance().startNewSession();

        if (ContextCompat.checkSelfPermission(CustomCameraActivity.this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(DEBUG, "OnResume: Asking permissions");
            ActivityCompat.requestPermissions(CustomCameraActivity.this,
                    new String[]{Manifest.permission.CAMERA}, PERMISSIONS_CAMERA);
        } else {
            Log.d(DEBUG, "OnResume: Permissions granted");

            safeCameraOpenInView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        hideNavBarAndStatusBar();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_CAMERA:
                if (grantResults.length == 0
                        || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    CustomCameraActivity.this.finish();
                } else {
                    safeCameraOpenInView();
                }
                break;
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, final Camera camera) {
        //Filling the params with appropriate values
        ImageInputParam params = new ImageInputParam(mParams.getPreviewSize().width, mParams.getPreviewSize().height,  mParams.getPreviewFormat());
        if (!recognitionFinished || isPauseRecognize) {
            //if already completed - ignore, results won't change
            return;
        }

        recognitionFinished = false;
        DocumentReader.Instance().recognizeVideoFrame(data, params, (i, documentReaderResults, throwable) -> {
            switch (i) {
                case DocReaderAction.COMPLETE: //all done, no more frames required, results won't change
                    synchronized (lock) {
                        isPauseRecognize = true;
                    }
                    if (documentReaderResults.morePagesAvailable == 1) { //more pages are available for this document
                        Toast.makeText(CustomCameraActivity.this, "Page ready, flip", Toast.LENGTH_LONG).show();

                        //letting API know, that all frames will be from different page of the same document, merge same field types
                        DocumentReader.Instance().startNewPage();
//                            mPreview.startCameraPreview();
                    } else { //no more pages available
                        AlertDialog.Builder builder = new AlertDialog.Builder(CustomCameraActivity.this);
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
                        builder.setMessage(documentReaderResults.getTextFieldValueByType(eVisualFieldType.FT_SURNAME_AND_GIVEN_NAMES));
                        builder.show();
                    }
                    break;
                case DocReaderAction.ERROR: //something went wrong
                    isPauseRecognize = true;
                    Toast.makeText(CustomCameraActivity.this, "Error: " + (throwable != null ? throwable.getMessage() : ""), Toast.LENGTH_LONG).show();
                    break;
            }

            recognitionFinished = true;
        });
    }

    private Camera getCameraInstance(int id){
        Camera c = null;

        if (id == -1) {
            int numberOfCameras = Camera.getNumberOfCameras();
            Log.d(DEBUG, "Number of cameras found:" + numberOfCameras);
            for (int i = 0; i < numberOfCameras; i++) {
                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(i, info);

                if (info.facing == CAMERA_FACING_BACK) {
                    id=i;
                    synchronized (lock) {
                        mCameraId = i;
                    }
                    break;
                }
            }
        }

        try {
            c = Camera.open(id); // attempt to get a Camera instance
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return c; // returns null if camera is unavailable
    }

    private boolean safeCameraOpenInView() {
        boolean qOpened;
        releaseCameraAndPreview();
        mCamera = getCameraInstance(mCameraId);
        qOpened = (mCamera != null);

        if(qOpened){
            mParams = mCamera.getParameters();

            previewParent = findViewById(R.id.camera_preview);
            mPreview = new CameraPreview(CustomCameraActivity.this, mCamera, this, previewParent);
            previewParent.addView(mPreview, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            mPreview.startCameraPreview();
        }
        return qOpened;
    }

    private void releaseCameraAndPreview() {

        synchronized (lock){
            if (mCamera != null) {
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
            if(mPreview != null) {
                mPreview.getHolder().removeCallback(mPreview);
                mPreview.destroyDrawingCache();
                mPreview.mCamera = null;
            }
        }
    }

    class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

        // SurfaceHolder
        private SurfaceHolder mHolder;

        // Our Camera.
        private Camera mCamera;

        private Camera.PreviewCallback callback;

        // Parent Context.
        private Activity mContext;

        // Camera Sizing (For rotation, orientation changes)
        private Camera.Size mPreviewSize;

        private Camera.Size mPictureSize;

        // List of supported preview sizes
        private List<Camera.Size> mSupportedPreviewSizes;

        // List of supported picture sizes
        private List<Camera.Size> mSupportedPictureSizes;

        // View holding this camera.
        private View mCameraView;


        public CameraPreview(Activity context, Camera camera, Camera.PreviewCallback previewCallback, View cameraView) {
            super(context);

            // Capture the context
            mCameraView = cameraView;
            mContext = context;
            setCamera(camera);
            this.callback = previewCallback;

            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);
            mHolder.setKeepScreenOn(true);
            // deprecated setting, but required on Android versions prior to 3.0
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        /**
         * Begin the preview of the camera input.
         */
        public void startCameraPreview()
        {
            try{
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }

        /**
         * Extract supported preview and flash modes from the camera.
         * @param camera
         */
        private void setCamera(Camera camera)
        {
            // Source: http://stackoverflow.com/questions/7942378/android-camera-will-not-work-startpreview-fails
            mCamera = camera;
            mSupportedPreviewSizes = mParams.getSupportedPreviewSizes();
            if (mSupportedPreviewSizes != null){
                mPreviewSize = getBestPreviewSize(mSupportedPreviewSizes);
                Log.d(DEBUG, "Current preview size: " + mPreviewSize.width + "*" + mPreviewSize.height);
            }

            mSupportedPictureSizes = mParams.getSupportedPictureSizes();
            if(mSupportedPictureSizes!=null){
                mPictureSize = getPictureSize(mSupportedPictureSizes, mPreviewSize);
                Log.d(DEBUG, "Current picture size: " + mPictureSize.width + "*" + mPictureSize.height);
            }

            requestLayout();
        }

        /**
         * The Surface has been created, now tell the camera where to draw the preview.
         * @param holder
         */
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(DEBUG, "surfaceCreated");
            try {
                mCamera.setPreviewDisplay(holder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Dispose of the camera preview.
         * @param holder
         */
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(DEBUG, "surfaceDestroyed");

            if (mCamera != null){
                mCamera.stopPreview();

                synchronized (lock){
                    isPreviewReady=false;
                }
            }
        }

        /**
         * React to surface changed events
         * @param holder
         * @param format
         * @param w
         * @param h
         */
        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            Log.d(DEBUG, "surfaceChanged");

            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.

            if (mHolder.getSurface() == null){
                // preview surface does not exist
                return;
            }

            // stop preview before making changes
            try {
                // Set the auto-focus mode to "continuous"
                if(mParams.getSupportedFocusModes()!=null && mParams.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    mParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                }

                mParams.setPreviewFormat(ImageFormat.NV21);

                // Preview size must exist.
                if(mPreviewSize != null) {
                    mParams.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
                    mParams.setPictureSize(mPictureSize.width, mPictureSize.height);

                    Camera.CameraInfo info = new Camera.CameraInfo();
                    Camera.getCameraInfo(mCameraId, info);

                    ViewGroup.LayoutParams params = previewParent.getLayoutParams();

                    double frameRatio = (double) mPreviewSize.width / (double) mPreviewSize.height;
                    double displayRatio = (double) h / (double) w;

                    if(frameRatio>displayRatio){
                        params.height = (int) (w * frameRatio);
                        params.width = w;
                    } else if(frameRatio<displayRatio){
                        params.height = h;
                        params.width = (int) (h/frameRatio);
                    } else {
                        params.width = w;
                        params.height = h;
                    }

                    previewParent.setLayoutParams(params);

                    synchronized (lock){
                        previewSize = mPreviewSize;
                        cameraOrientation = getCorrectCameraOrientation(info);
                        previewFormat = ImageFormat.NV21;
                    }
                }

                mCamera.stopPreview();
                mCamera.setParameters(mParams);
                mCamera.setDisplayOrientation(cameraOrientation);
                mCamera.setPreviewDisplay(holder);
                mCamera.setPreviewCallback(callback);

                synchronized (lock){
                    isPreviewReady = true;
                }

                mCamera.startPreview();
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        /**
         * Calculate the measurements of the layout
         * @param widthMeasureSpec
         * @param heightMeasureSpec
         */
        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
        {
            // Source: http://stackoverflow.com/questions/7942378/android-camera-will-not-work-startpreview-fails
            final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
            final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
            setMeasuredDimension(width, height);
        }

        private Camera.Size getBestPreviewSize(List<Camera.Size> sizes){
            Log.d(DEBUG, "Getting best suitable size for video frames");
            Camera.Size biggest = null;
            for(Camera.Size size:sizes) {
                if (size.width == 3840 && size.height == 2160) {
                    biggest = size;
                    break;
                }
            }
            if(biggest==null){
                Log.d(DEBUG,  "List of available sizes: ");
                for(Camera.Size size:sizes){
                    Log.d(DEBUG, size.width + "*" + size.height);
                    if(size.width == 1920 && size.height == 1080) {
                        biggest = size;
                        break;
                    }
                    else{
                        if(biggest==null){
                            biggest = size;
                        }else if(size.height>=biggest.height && size.width>=biggest.width){
                            biggest=size;
                        }
                    }
                }
            }

            return biggest;
        }

        private Camera.Size getPictureSize(List<Camera.Size> sizes, Camera.Size camSize) {
            Log.d(DEBUG, "Getting best suitable size for image capturing");

            Camera.Size biggest = null;
            float ratio = (float)camSize.width / (float)camSize.height;

            ArrayList<Camera.Size> sameRatio = new ArrayList<>();
            Log.d(DEBUG,  "List of available sizes: ");
            for (Camera.Size pictureSize : sizes) {
                Log.d(DEBUG, pictureSize.width + " * " + pictureSize.height);
//                if(pictureSize.width>= camSize.width && pictureSize.height>=camSize.height){ //picture size should be bigger than preview size

                if(biggest == null){
                    biggest = pictureSize;
                } else if(pictureSize.height>=biggest.height && pictureSize.width>=biggest.width){
                    biggest=pictureSize;
                }

                float pictureRatio =  (float)pictureSize.width / (float)pictureSize.height;
                if ( ratio == pictureRatio){

                    if(pictureSize.width==1920 && pictureSize.height==1080)
                        return pictureSize;

                    sameRatio.add(pictureSize);
                }
            }
//            }
            Camera.Size biggestSameRatio = null;
            for(Camera.Size sameRatioSize : sameRatio){
                if(biggestSameRatio==null){
                    biggestSameRatio = sameRatioSize;
                } else if(sameRatioSize.width>=biggestSameRatio.width && sameRatioSize.height>=biggestSameRatio.height){
                    biggestSameRatio=sameRatioSize;
                }
            }
            if(biggestSameRatio!=null){
                return biggestSameRatio;
            }

            return biggest;
        }

        int getCorrectCameraOrientation(Camera.CameraInfo info) {

            int rotation = mContext.getWindowManager().getDefaultDisplay().getRotation();
            int degrees = 0;

            switch(rotation){
                case Surface.ROTATION_0:
                    degrees = 0;
                    break;

                case Surface.ROTATION_90:
                    degrees = 90;
                    break;

                case Surface.ROTATION_180:
                    degrees = 180;
                    break;

                case Surface.ROTATION_270:
                    degrees = 270;
                    break;

            }

            int result;
            if(info.facing==Camera.CameraInfo.CAMERA_FACING_FRONT){
                result = (info.orientation + degrees) % 360;
                result = (360 - result) % 360;
            }else{
                result = (info.orientation - degrees + 360) % 360;
            }

            return result;
        }
    }

    protected void hideNavBarAndStatusBar() {
        View decorView = getWindow().getDecorView();
        if(decorView == null)
            return;
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
        );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            decorView.setSystemUiVisibility(decorView.getSystemUiVisibility()
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }
}

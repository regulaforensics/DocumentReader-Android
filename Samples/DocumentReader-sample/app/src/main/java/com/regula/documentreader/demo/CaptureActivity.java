package com.regula.documentreader.demo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.regula.documentreader.api.DocumentReader;
import com.regula.documentreader.api.params.InputParams;

import java.util.List;

import static android.widget.Toast.LENGTH_SHORT;

@SuppressWarnings("deprecation")
public class CaptureActivity extends AppCompatActivity {

    private static final Handler HANDLER = new Handler();
    private static final String DEBUG = "CaptureActivity";
    private static final String IS_LIGHT_ON = "IsLightOn";

    private SharedPreferences mPreferences;
    private SurfaceView mOverlayDrawView;
    private View mOverlayControlView;
    private FrameLayout mPreviewHolder;
    private MediaPlayer mAutoFocusSound;
    private ImageButton mLightBtn;
    private SensorManager mSensorManager;
    private boolean mIsReturning, mSensorWorked = false, mIsLightOn = false, mIsFlashAvailable, mIsAutoFocusing;
    private int mAutoFocusX;
    private int mAutoFocusY;
    private int mCurrentDisplayRotation;
    private int mCameraId;
    private Camera mCamera;

    private final Runnable clearAutoFocusSquareRunnable = new Runnable() {
        @Override
        public void run() {
            clearCanvas();
        }
    };


    private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, final Camera camera) {
            if(!mIsAutoFocusing) {
                Camera.Parameters parameters = camera.getParameters();
                Camera.Size size = parameters.getPreviewSize();
                InputParams params = new InputParams();
                params.imageInputParam.type=parameters.getPreviewFormat();
                params.imageInputParam.height = size.height;
                params.imageInputParam.width = size.width;
                params.processParam.ocr = true;
                params.processParam.mrz = true;
                int result = DocumentReader.Instance().process(data,params);

                if (result == 1) {
                    Log.d(DEBUG, "MRZ detector found an mrz, switching to ResultsActivity");
                    mOverlayDrawView.setOnTouchListener(null);
                    mCamera.cancelAutoFocus();
                    mCamera.setPreviewCallback(null);
                    mCamera.stopPreview();

                    Intent intent = new Intent(CaptureActivity.this,ResultsActivity.class);
                    CaptureActivity.this.startActivity(intent);
                }
            }
        }
    };

    private SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
                if (mIsFlashAvailable && !mSensorWorked) {
                    float lightAmount = event.values[0];
                    Log.d(DEBUG, "Current light amount:" + lightAmount);
                    if (lightAmount < 30)
                        switchLight(true);
                }
                mSensorWorked = true;
            } else if(event.sensor.getType() == Sensor.TYPE_ORIENTATION){
                if(mCamera!=null) {
                    setCameraRotation();
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {  }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

        mPreviewHolder = (FrameLayout) findViewById(R.id.cameraPreviewHolder);
        mOverlayDrawView = new SurfaceView(getApplicationContext());
        mOverlayDrawView.setBackgroundColor(Color.TRANSPARENT);

        mOverlayControlView = getLayoutInflater().inflate(R.layout.overlay_controls, null);
        mOverlayControlView.setBackgroundColor(Color.TRANSPARENT);
        mLightBtn = (ImageButton) mOverlayControlView.findViewById(R.id.lightBtn);
        mPreferences = getSharedPreferences(MainActivity.PREFERENCES, MODE_PRIVATE);

        mAutoFocusSound = MediaPlayer.create(CaptureActivity.this, R.raw.autofocus);
        mAutoFocusSound.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                Log.d(DEBUG, "Media player error What: " + i + " extra: "+ i1);
                return false;
            }
        });

        mCameraId = mPreferences.getInt(MainActivity.SELECTED_CAMERA_ID,-1);
        mIsReturning = savedInstanceState!=null;
    }

    @Override
    protected void onResume() {
        super.onResume();

        mCamera = getCameraInstance();

        if (mCamera == null) {
            Toast.makeText(getApplicationContext(), R.string.no_camera_access, LENGTH_SHORT).show();
            this.finish();
        } else {
            DocumentReader.Instance().startNewDocument();

            CameraPreview mCameraPreview = new CameraPreview(CaptureActivity.this, mCamera, previewCallback);
            Log.d(DEBUG, "OnResume: Camera preview created");

            Camera.Size chosen = getBestPreviewSize(mCamera.getParameters().getSupportedPreviewSizes());
            mCamera.getParameters().setPreviewSize(chosen.width, chosen.height);

            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            int mDisplayWidth = metrics.widthPixels;
            int mDisplayHeight = metrics.heightPixels;

            LinearLayout.LayoutParams params;
            int newHeight = (int) Math.round((double)(chosen.height* mDisplayWidth)/chosen.width);
            int newWidth = (int) Math.round ((double) (mDisplayHeight *chosen.width)/chosen.height);

            int heightDiff = newHeight - mDisplayHeight;
            int widthDiff = newWidth - mDisplayWidth;

            if(heightDiff> 0 && widthDiff >= 0){ //if both are greater than 0 - choose minimum
                if(widthDiff >= heightDiff){
                    params=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, newHeight);
                } else {
                    params=new LinearLayout.LayoutParams(newWidth, ViewGroup.LayoutParams.MATCH_PARENT);
                }
            } else { //otherwise choose maximum
                if(widthDiff >= heightDiff){
                    params=new LinearLayout.LayoutParams(newWidth, mDisplayHeight);
                } else {
                    params=new LinearLayout.LayoutParams(mDisplayWidth, newHeight);
                }
            }
            params.gravity= Gravity.CENTER;

            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            if(mCameraPreview.getParent()==null) {
                mPreviewHolder.addView(mCameraPreview,params);
            }
            mOverlayDrawView.setZOrderMediaOverlay(true);
            mOverlayDrawView.getHolder().setFormat(PixelFormat.TRANSPARENT);
            mOverlayDrawView.setLayoutParams(lp);
            if(mOverlayDrawView.getParent()==null) {
                mPreviewHolder.addView(mOverlayDrawView);
            }
            if(mOverlayControlView.getParent()==null) {
                CaptureActivity.this.addContentView(mOverlayControlView, lp);
            }

            Log.d(DEBUG, "OnResume: All controls added to screen");

            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            if (mSensorManager != null) {
                Log.d(DEBUG, "Sensor manager created");
                Sensor rotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
                if (rotationSensor != null) {
                    boolean isRotationSensorAdded = mSensorManager.registerListener(sensorEventListener, rotationSensor, SensorManager.SENSOR_DELAY_UI);
                    Log.d(DEBUG, "OnResume: Rotation sensor listener added: " + isRotationSensorAdded);
                }

                if (mIsFlashAvailable) {
                    mLightBtn.setVisibility(View.VISIBLE);

                    mLightBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mIsLightOn = mPreferences.getBoolean(IS_LIGHT_ON, false);
                            switchLight(!mIsLightOn);
                        }
                    });
                    if (!mIsReturning) { // app first started
                        mIsReturning = true;
                        Sensor lightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

                        if (lightSensor != null) {
                            boolean isLightSensorAdded = mSensorManager.registerListener(sensorEventListener, lightSensor, 1);
                            Log.d(DEBUG, "OnResume: Light sensor listener added: " + isLightSensorAdded);
                        }
                    } else {  // we are returning to the activity or unable to add light sensor
                        mIsLightOn = mPreferences.getBoolean(IS_LIGHT_ON, false);
                        switchLight(mIsLightOn);
                    }
                } else {
                    mLightBtn.setVisibility(View.GONE);
                }
            }

            if (mCamera.getParameters().getSupportedFocusModes() != null
                    && mCamera.getParameters().getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {

                mOverlayDrawView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View arg0, final MotionEvent arg1) {

                        if (arg1.getAction() == MotionEvent.ACTION_DOWN) {
                            mIsAutoFocusing = true;
                            HANDLER.removeCallbacks(clearAutoFocusSquareRunnable);
                            mAutoFocusX = (int) arg1.getX();
                            mAutoFocusY = (int) arg1.getY();

                            final Camera.Parameters parameters = mCamera.getParameters();
                            final Camera.Size size = parameters.getPreviewSize();

                            drawRect(mOverlayDrawView, mAutoFocusX, mAutoFocusY, size.height / 10,
                                    size.height / 10, Color.YELLOW);

                            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                            mCamera.setParameters(parameters);
                            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                                @Override
                                public void onAutoFocus(boolean success, Camera camera) {
                                    Log.d(DEBUG, "AutoFocus started");
                                    if (success) {
                                        Log.d(DEBUG, "AutoFocus success");
                                        drawRect(mOverlayDrawView, mAutoFocusX, mAutoFocusY, size.height / 10,
                                                size.height / 10, Color.GREEN);
                                        try {
                                            if (mAutoFocusSound != null && !mAutoFocusSound.isPlaying())
                                                mAutoFocusSound.start();
                                        } catch(Exception ex){
                                            ex.printStackTrace();
                                        }
                                    } else {
                                        Log.d(DEBUG, "AutoFocus failed");
                                        drawRect(mOverlayDrawView, mAutoFocusX, mAutoFocusY, size.height / 10,
                                                size.height / 10, Color.RED);
                                    }

                                    if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                                        mCamera.setParameters(parameters);
                                    }

                                    HANDLER.postDelayed(clearAutoFocusSquareRunnable, 1000);
                                    mIsAutoFocusing = false;
                                }
                            });
                        }
                        return false;
                    }
                });
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();

        ViewGroup viewGroup = (ViewGroup) mOverlayControlView.getParent();
        if(viewGroup!=null)
            viewGroup.removeView(mOverlayControlView);

        if (mSensorManager != null && sensorEventListener != null) {
            mSensorManager.unregisterListener(sensorEventListener);
            Log.d(DEBUG, "OnPause: sensor released");
        }

        mPreviewHolder.removeAllViews();

        if(mCamera!=null) {
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
            Log.d(DEBUG, "OnPause: camera released");
        }

        if(mAutoFocusSound!=null)
            mAutoFocusSound.release();

        HANDLER.removeCallbacks(clearAutoFocusSquareRunnable);
    }

    private void switchLight(boolean light){
        Camera.Parameters parameters = mCamera.getParameters();
        if (light) {
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            mCamera.setParameters(parameters);
            mLightBtn.setImageResource(R.drawable.lightbulb_on);
        } else {
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            mCamera.setParameters(parameters);
            mLightBtn.setImageResource(R.drawable.lightbulb_off);
        }

        mIsLightOn = light;
        mPreferences.edit().putBoolean(IS_LIGHT_ON, mIsLightOn).apply();
        Log.d(DEBUG, "Switch light: " + mIsLightOn);
    }

    private void drawRect(SurfaceView view, int x, int y, int width, int height, int color) {
        SurfaceHolder holder = view.getHolder();
        Paint p = new Paint();
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2);

        Canvas c = holder.lockCanvas();
        if (c != null) {
            c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            p.setColor(color);
            c.drawRoundRect(new RectF(x - width / 2, y - height / 2, x + width / 2, y + height / 2), 10, 10, p);
            holder.unlockCanvasAndPost(c);
        }
    }

    private void clearCanvas() {
        SurfaceHolder holder = mOverlayDrawView.getHolder();
        Canvas c = holder.lockCanvas();
        if(c!=null) {
            c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            holder.unlockCanvasAndPost(c);
        }
    }

    private Camera getCameraInstance() {
        try{
            if (CaptureActivity.this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                if (mCameraId == -1) {
                    int numberOfCameras = Camera.getNumberOfCameras();
                    Log.d(DEBUG, "Number of cameras found:" + numberOfCameras);
                    for (int i = 0; i < numberOfCameras; i++) {
                        Camera.CameraInfo info = new Camera.CameraInfo();
                        Camera.getCameraInfo(i, info);

                        if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                            mCameraId = i;
                            break;
                        }
                    }
                }
                mCamera = Camera.open(mCameraId);
                Camera.Parameters params = mCamera.getParameters();

                mIsFlashAvailable = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
                        && params.getSupportedFlashModes() != null
                        && params.getSupportedFlashModes().contains(Camera.Parameters.FLASH_MODE_TORCH);

                if (params.isZoomSupported()) {
                    Log.d(DEBUG, "Current zoom:" + params.getZoom() + " setting zoom to 0");
                    params.setZoom(0);
                }

                Camera.Size previewSize = getBestPreviewSize(params.getSupportedPreviewSizes());
                params.setPreviewSize(previewSize.width, previewSize.height);
                Log.d(DEBUG, "Preview size set to " + previewSize.width + "*" + previewSize.height);

                //hack to make Nexus work )
                Camera.Size pictureSize = getPictureSize(params.getSupportedPictureSizes(), params.getPreviewSize());
                params.setPictureSize(pictureSize.width, pictureSize.height);
                Log.d(DEBUG, "Picture size set to " + pictureSize.width + "*" + pictureSize.height);

                if(params.getSupportedPreviewFormats().contains(ImageFormat.NV21)){
                    params.setPreviewFormat(ImageFormat.NV21);
                    Log.d(DEBUG, "Preview format set to NV21");
                }

                mCamera.setParameters(params);
                mCamera.setErrorCallback(new Camera.ErrorCallback() {
                    @Override
                    public void onError(int error, Camera camera) {
                        Log.d("Error", "Camera error: " + error);
                    }
                });

                return mCamera;
            }
        } catch (Exception ex){
            ex.printStackTrace();
        }
        return null;
    }

    private Camera.Size getBestPreviewSize(List<Camera.Size> sizes){
        Camera.Size biggest = null;
        for(Camera.Size size:sizes){
            if(size.width<=1920 && size.height<=1080) {
                if (size.width == 1920 && size.height == 1080) {
                    biggest = size;
                    break;
                }
                if(biggest==null)
                    biggest=size;
                else{
                    if(size.height>=biggest.height && size.width>=biggest.width){
                        biggest=size;
                    }
                }
            }
        }
        return biggest;
    }

    private Camera.Size getPictureSize(List<Camera.Size> sizes, Camera.Size camSize) {
        if(sizes.contains(camSize)){
            return camSize;
        } else {
            float ratio = camSize.width / camSize.height;
            for (Camera.Size pictureSize : sizes) {
                if ((float) pictureSize.width / pictureSize.height == ratio)
                    return pictureSize;
            }
            return sizes.get(0);
        }
    }

    private void setCameraRotation() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, info);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();

        if (mCurrentDisplayRotation != rotation) {
            Log.d(DEBUG, "Device rotated, rotation: " + rotation);
            mCurrentDisplayRotation = rotation;
            int degrees = 0;
            switch (mCurrentDisplayRotation) {
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

            if(mCamera!=null) {
                int result = (info.orientation - degrees + 360) % 360;
                mCamera.setDisplayOrientation(result);
            }
        }
    }
}

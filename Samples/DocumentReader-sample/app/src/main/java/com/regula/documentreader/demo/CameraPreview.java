package com.regula.documentreader.demo;

import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/** A basic Camera preview class */
@SuppressWarnings("deprecation") //Camera 2 Api not yet used widely
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback{
    private SurfaceHolder mHolder;
    private PreviewCallback cb;
    private Camera mCamera;

    public CameraPreview(Context context, Camera camera, PreviewCallback cb) {
        super(context);
        this.cb = cb;
        mHolder = getHolder();
        mHolder.addCallback(this);
        mCamera = camera;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {}

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (mHolder.getSurface() == null)
            return;

        try {
            mCamera.stopPreview();
        } catch (Exception e) { // ignore: tried to stop a non-existent preview
            e.printStackTrace();
        }

        try {
            holder.setFormat(PixelFormat.TRANSPARENT);

            mCamera.setPreviewDisplay(mHolder);
            mCamera.setPreviewCallback(cb);
            mCamera.startPreview();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

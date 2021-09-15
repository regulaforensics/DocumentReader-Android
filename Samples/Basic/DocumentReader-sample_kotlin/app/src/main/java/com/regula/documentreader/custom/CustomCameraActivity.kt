package com.regula.documentreader.custom

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.hardware.Camera.PreviewCallback
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.regula.documentreader.R
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.enums.DocReaderAction
import com.regula.documentreader.api.enums.eVisualFieldType
import com.regula.documentreader.api.params.ImageInputParam
import java.io.IOException
import java.util.*

class CustomCameraActivity : AppCompatActivity(), PreviewCallback {
    private var mCameraId: Int = 0
    private var mCamera: Camera? = null
    private var mParams: Camera.Parameters? = null
    private var mPreview: CameraPreview? = null
    private var previewParent: RelativeLayout? = null
    private var isPreviewReady: Boolean = false
    private var previewSize: Camera.Size? = null
    private var cameraOrientation: Int = 0
    private var previewFormat: Int = 0
    private var recognitionFinished: Boolean = true
    private var isPauseRecognize: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

//        DocumentReader.Instance().processParams().scenario = "FullProcess";
//        DocumentReader.Instance().processParams().multipageProcessing = false;
        //let API know, that all previous results should be disposed
        DocumentReader.Instance().startNewSession()
        if ((ContextCompat.checkSelfPermission(this@CustomCameraActivity, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED)
        ) {
            Log.d(DEBUG, "OnResume: Asking permissions")
            ActivityCompat.requestPermissions(
                this@CustomCameraActivity,
                arrayOf(Manifest.permission.CAMERA),
                PERMISSIONS_CAMERA
            )
        } else {
            Log.d(DEBUG, "OnResume: Permissions granted")
            safeCameraOpenInView()
        }
    }

    override fun onResume() {
        super.onResume()
        hideNavBarAndStatusBar()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_CAMERA -> if ((grantResults.isEmpty()
                        || grantResults[0] != PackageManager.PERMISSION_GRANTED)
            ) {
                finish()
            } else {
                safeCameraOpenInView()
            }
        }
    }

    override fun onPreviewFrame(data: ByteArray, camera: Camera) {
        //Filling the params with appropriate values
        val params: ImageInputParam = ImageInputParam(
            mParams!!.getPreviewSize().width,
            mParams!!.getPreviewSize().height,
            mParams!!.getPreviewFormat()
        )
        if (!recognitionFinished || isPauseRecognize) {
            //if already completed - ignore, results won't change
            return
        }
        recognitionFinished = false
        DocumentReader.Instance().recognizeVideoFrame(
            data,
            params
        ) { i, documentReaderResults, throwable ->
            when (i) {
                DocReaderAction.COMPLETE -> {
                    synchronized(lock) { isPauseRecognize = true }
                    if (documentReaderResults?.morePagesAvailable == 1) { //more pages are available for this document
                        Toast.makeText(
                            this@CustomCameraActivity,
                            "Page ready, flip",
                            Toast.LENGTH_LONG
                        ).show()

                        //letting API know, that all frames will be from different page of the same document, merge same field types
                        DocumentReader.Instance().startNewPage()
                        //                            mPreview.startCameraPreview();
                    } else { //no more pages available
                        val builder: AlertDialog.Builder =
                            AlertDialog.Builder(this@CustomCameraActivity)
                        builder.setPositiveButton(
                            "Ok"
                        ) { dialogInterface, i ->
                            DocumentReader.Instance().startNewSession()
                            dialogInterface.dismiss()
                            synchronized(lock) { isPauseRecognize = false }
                        }
                        builder.setTitle("Processing finished")
                        //getting text field value from results
                        builder.setMessage(
                            documentReaderResults?.getTextFieldValueByType(
                                eVisualFieldType.FT_SURNAME_AND_GIVEN_NAMES
                            )
                        )
                        builder.show()
                    }
                }
                DocReaderAction.ERROR -> {
                    isPauseRecognize = true
                    Toast.makeText(
                        this@CustomCameraActivity,
                        "Error: " + (if (throwable != null) throwable.message else ""),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            recognitionFinished = true
        }
    }

    private fun getCameraInstance(id: Int): Camera? {
        var id: Int = id
        var c: Camera? = null
        if (id == -1) {
            val numberOfCameras: Int = Camera.getNumberOfCameras()
            Log.d(DEBUG, "Number of cameras found:" + numberOfCameras)
            for (i in 0 until numberOfCameras) {
                val info: CameraInfo = CameraInfo()
                Camera.getCameraInfo(i, info)
                if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                    id = i
                    synchronized(lock, { mCameraId = i })
                    break
                }
            }
        }
        try {
            c = Camera.open(id) // attempt to get a Camera instance
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return c // returns null if camera is unavailable
    }

    private fun safeCameraOpenInView(): Boolean {
        releaseCameraAndPreview()
        mCamera = getCameraInstance(mCameraId)
        val qOpened: Boolean = (mCamera != null)
        if (qOpened) {
            mParams = mCamera!!.parameters
            previewParent = findViewById(R.id.camera_preview)
            mPreview = CameraPreview(this@CustomCameraActivity, mCamera, this, previewParent)
            with(previewParent) {
                this?.addView(
                    mPreview,
                    RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
            }
            mPreview!!.startCameraPreview()
        }
        return qOpened
    }

    private fun releaseCameraAndPreview() {
        synchronized(lock) {
            if (mCamera != null) {
                mCamera!!.setPreviewCallback(null)
                mCamera!!.stopPreview()
                mCamera!!.release()
                mCamera = null
            }
            if (mPreview != null) {
                mPreview!!.holder.removeCallback(mPreview)
                mPreview!!.destroyDrawingCache()
                mPreview!!.mCamera = null
            }
        }
    }

    internal inner class CameraPreview constructor(// Parent Context.
        private val mContext: Activity,
        camera: Camera?,
        previewCallback: PreviewCallback, // View holding this camera.
        private val mCameraView: View?
    ) : SurfaceView(mContext), SurfaceHolder.Callback {
        // SurfaceHolder
        private val mHolder: SurfaceHolder

        // Our Camera.
        var mCamera: Camera? = null
        private val callback: PreviewCallback

        // Camera Sizing (For rotation, orientation changes)
        private var mPreviewSize: Camera.Size? = null
        private var mPictureSize: Camera.Size? = null

        // List of supported preview sizes
        private var mSupportedPreviewSizes: List<Camera.Size>? = null

        // List of supported picture sizes
        private var mSupportedPictureSizes: List<Camera.Size>? = null

        /**
         * Begin the preview of the camera input.
         */
        fun startCameraPreview() {
            try {
                mCamera!!.setPreviewDisplay(mHolder)
                mCamera!!.startPreview()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        /**
         * Extract supported preview and flash modes from the camera.
         * @param camera
         */
        private fun setCamera(camera: Camera?) {
            // Source: http://stackoverflow.com/questions/7942378/android-camera-will-not-work-startpreview-fails
            mCamera = camera
            mSupportedPreviewSizes = mParams!!.supportedPreviewSizes
            if (mSupportedPreviewSizes != null) {
                mPreviewSize = getBestPreviewSize(mSupportedPreviewSizes!!)
                Log.d(
                    DEBUG,
                    "Current preview size: " + mPreviewSize!!.width + "*" + mPreviewSize!!.height
                )
            }
            mSupportedPictureSizes = mParams!!.supportedPreviewSizes
            if (mSupportedPictureSizes != null) {
                mPictureSize = getPictureSize(mSupportedPictureSizes!!, mPreviewSize)
                Log.d(
                    DEBUG,
                    "Current picture size: " + mPictureSize!!.width + "*" + mPictureSize!!.height
                )
            }
            requestLayout()
        }

        /**
         * The Surface has been created, now tell the camera where to draw the preview.
         * @param holder
         */
        override fun surfaceCreated(holder: SurfaceHolder) {
            Log.d(DEBUG, "surfaceCreated")
            try {
                mCamera!!.setPreviewDisplay(holder)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        /**
         * Dispose of the camera preview.
         * @param holder
         */
        override fun surfaceDestroyed(holder: SurfaceHolder) {
            Log.d(DEBUG, "surfaceDestroyed")
            if (mCamera != null) {
                mCamera!!.stopPreview()
                synchronized(lock, { isPreviewReady = false })
            }
        }

        /**
         * React to surface changed events
         * @param holder
         * @param format
         * @param w
         * @param h
         */
        override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
            Log.d(DEBUG, "surfaceChanged")

            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.
            if (mHolder.surface == null) {
                // preview surface does not exist
                return
            }

            // stop preview before making changes
            try {
                // Set the auto-focus mode to "continuous"
                if (mParams!!.supportedFocusModes != null && mParams!!.supportedFocusModes
                        .contains(
                            Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                        )
                ) {
                    mParams!!.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)
                }
                mParams!!.setPreviewFormat(ImageFormat.NV21)

                // Preview size must exist.
                if (mPreviewSize != null) {
                    mParams!!.setPreviewSize(mPreviewSize!!.width, mPreviewSize!!.height)
                    mParams!!.setPictureSize(mPictureSize!!.width, mPictureSize!!.height)
                    val info = CameraInfo()
                    Camera.getCameraInfo(mCameraId, info)
                    val params: ViewGroup.LayoutParams = previewParent!!.getLayoutParams()
                    val frameRatio: Double =
                        mPreviewSize!!.width.toDouble() / mPreviewSize!!.height.toDouble()
                    val displayRatio: Double = h.toDouble() / w.toDouble()
                    if (frameRatio > displayRatio) {
                        params.height = (w * frameRatio).toInt()
                        params.width = w
                    } else if (frameRatio < displayRatio) {
                        params.height = h
                        params.width = (h / frameRatio).toInt()
                    } else {
                        params.width = w
                        params.height = h
                    }
                    previewParent!!.setLayoutParams(params)
                    synchronized(lock) {
                        previewSize = mPreviewSize
                        cameraOrientation = getCorrectCameraOrientation(info)
                        previewFormat = ImageFormat.NV21
                    }
                }
                mCamera!!.stopPreview()
                mCamera!!.setParameters(mParams)
                mCamera!!.setDisplayOrientation(cameraOrientation)
                mCamera!!.setPreviewDisplay(holder)
                mCamera!!.setPreviewCallback(callback)
                synchronized(lock, { isPreviewReady = true })
                mCamera!!.startPreview()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        /**
         * Calculate the measurements of the layout
         * @param widthMeasureSpec
         * @param heightMeasureSpec
         */
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            // Source: http://stackoverflow.com/questions/7942378/android-camera-will-not-work-startpreview-fails
            val width: Int = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec)
            val height: Int = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec)
            setMeasuredDimension(width, height)
        }

        private fun getBestPreviewSize(sizes: List<Camera.Size>): Camera.Size? {
            Log.d(DEBUG, "Getting best suitable size for video frames")
            var biggest: Camera.Size? = null
            for (size: Camera.Size in sizes) {
                if (size.width == 3840 && size.height == 2160) {
                    biggest = size
                    break
                }
            }
            if (biggest == null) {
                Log.d(DEBUG, "List of available sizes: ")
                for (size: Camera.Size in sizes) {
                    Log.d(DEBUG, size.width.toString() + "*" + size.height)
                    if (size.width == 1920 && size.height == 1080) {
                        biggest = size
                        break
                    } else {
                        if (biggest == null) {
                            biggest = size
                        } else if (size.height >= biggest.height && size.width >= biggest.width) {
                            biggest = size
                        }
                    }
                }
            }
            return biggest
        }

        private fun getPictureSize(sizes: List<Camera.Size>, camSize: Camera.Size?): Camera.Size? {
            Log.d(DEBUG, "Getting best suitable size for image capturing")
            var biggest: Camera.Size? = null
            val ratio: Float = camSize!!.width.toFloat() / camSize.height.toFloat()
            val sameRatio: ArrayList<Camera.Size> = ArrayList()
            Log.d(DEBUG, "List of available sizes: ")
            for (pictureSize: Camera.Size in sizes) {
                Log.d(DEBUG, pictureSize.width.toString() + " * " + pictureSize.height)
                //                if(pictureSize.width>= camSize.width && pictureSize.height>=camSize.height){ //picture size should be bigger than preview size
                if (biggest == null) {
                    biggest = pictureSize
                } else if (pictureSize.height >= biggest.height && pictureSize.width >= biggest.width) {
                    biggest = pictureSize
                }
                val pictureRatio: Float = pictureSize.width.toFloat() / pictureSize.height.toFloat()
                if (ratio == pictureRatio) {
                    if (pictureSize.width == 1920 && pictureSize.height == 1080) return pictureSize
                    sameRatio.add(pictureSize)
                }
            }
            //            }
            var biggestSameRatio: Camera.Size? = null
            for (sameRatioSize: Camera.Size in sameRatio) {
                if (biggestSameRatio == null) {
                    biggestSameRatio = sameRatioSize
                } else if (sameRatioSize.width >= biggestSameRatio.width && sameRatioSize.height >= biggestSameRatio.height) {
                    biggestSameRatio = sameRatioSize
                }
            }
            if (biggestSameRatio != null) {
                return biggestSameRatio
            }
            return biggest
        }

        private fun getCorrectCameraOrientation(info: CameraInfo): Int {
            val rotation: Int = mContext.windowManager.defaultDisplay.rotation
            var degrees: Int = 0
            when (rotation) {
                Surface.ROTATION_0 -> degrees = 0
                Surface.ROTATION_90 -> degrees = 90
                Surface.ROTATION_180 -> degrees = 180
                Surface.ROTATION_270 -> degrees = 270
            }
            var result: Int
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                result = (info.orientation + degrees) % 360
                result = (360 - result) % 360
            } else {
                result = (info.orientation - degrees + 360) % 360
            }
            return result
        }

        init {

            // Capture the context
            setCamera(camera)
            callback = previewCallback

            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = holder
            mHolder.addCallback(this)
            mHolder.setKeepScreenOn(true)
            // deprecated setting, but required on Android versions prior to 3.0
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        }
    }

    private fun hideNavBarAndStatusBar() {
        val decorView: View = window.decorView
        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            decorView.systemUiVisibility = (decorView.systemUiVisibility
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

    companion object {
        private val DEBUG: String = "DEBUG"
        private val lock: Any = Any()
        private val PERMISSIONS_CAMERA: Int = 1100
    }
}
package com.regula.customcamera_kotlin.custom

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.Camera
import android.os.Bundle
import android.util.Log
import android.view.OrientationEventListener
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.regula.common.CameraCallbacks
import com.regula.common.CameraFragment
import com.regula.common.RegCameraFragment
import com.regula.common.enums.CommonKeys
import com.regula.customcamera_kotlin.R
import com.regula.documentreader.api.CaptureActivity3
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.enums.DocReaderAction
import com.regula.documentreader.api.enums.eVisualFieldType
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.internal.params.ImageInputParam
import com.regula.documentreader.api.results.DocumentReaderResults

class CustomRegActivity : CaptureActivity3(), CameraCallbacks {
    private var orientationEventListener: OrientationEventListener? = null
    private var mCurrentDegrees = 0
    private var recognitionFinished = true
    private var isPauseRecognize = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var isScenarioExist = false
        for (scenario in DocumentReader.Instance().availableScenarios) {
            if (scenario.name.equals(DocumentReader.Instance().processParams().scenario)) {
                isScenarioExist = true
                break
            }
        }
        if (!isScenarioExist) {
            Log.e(DEBUG, "Scenario: ${DocumentReader.Instance().processParams().scenario} doesn't exist")
            finish()
            return
        }

        if (ContextCompat.checkSelfPermission(this@CustomRegActivity, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(DEBUG, "onCreate: Asking permissions")
            ActivityCompat.requestPermissions(
                this@CustomRegActivity,
                arrayOf(Manifest.permission.CAMERA),
                PERMISSIONS_CAMERA
            )
        } else {
            Log.d(DEBUG, "onCreate: Permissions granted")
            safeCameraOpenInView()
        }
        DocumentReader.Instance().startNewSession()
        setContentView(R.layout.activity_custom_reg)
        val currentOrientation = resources.configuration.orientation
        mCurrentDegrees = if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) 0 else 90
        orientationEventListener = object : OrientationEventListener(this@CustomRegActivity) {
            override fun onOrientationChanged(orientation: Int) {
                orientationChanged(orientation)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        orientationEventListener!!.disable()
    }

    private fun safeCameraOpenInView() {
        val fragmentManager = supportFragmentManager
        cameraFragment = fragmentManager.findFragmentByTag(FRAGMENT_TAG) as RegCameraFragment?
        if (cameraFragment == null) {
            cameraFragment = CameraFragment()
            //            cameraFragment = new Camera2Fragment();
            val args = Bundle()
            args.putInt(CommonKeys.CAMERA_ID, 0)
            cameraFragment.arguments = args
            fragmentManager.beginTransaction().add(R.id.cameraUi, cameraFragment, FRAGMENT_TAG)
                .commit()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_CAMERA) {
            if (grantResults.size == 0
                || grantResults[0] != PackageManager.PERMISSION_GRANTED
            ) {
                finish()
            } else {
                safeCameraOpenInView()
            }
        }
    }

    fun onClickClosed(view: View?) {
        finish()
    }

    override fun onCameraOpened(b: Boolean) {
        orientationEventListener!!.enable()
    }

    override fun onFrame(frame: ByteArray) {
        //Filling the params with appropriate values
        if (!recognitionFinished || isPauseRecognize) {
            //if already completed - ignore, results won't change
            return
        }
        recognitionFinished = false
        val params = ImageInputParam(
            cameraFragment.frameWidth,
            cameraFragment.frameHeight,
            cameraFragment.frameFormat
        )
        if (cameraFragment.cameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            params.rotation = (mCurrentDegrees + cameraFragment.cameraOrientation) % 360
        } else {
            params.rotation = mCurrentDegrees - cameraFragment.cameraOrientation
        }
        recognizeFrame(frame, params)
    }

    private fun recognizeFrame(frame: ByteArray, params: ImageInputParam) {
        DocumentReader.Instance().recognizeVideoFrame(
            frame, params
        ) { action: Int, results: DocumentReaderResults?, error: DocumentReaderException? ->
            when (action) {
                DocReaderAction.COMPLETE or DocReaderAction.TIMEOUT -> {
                    synchronized(lock) { isPauseRecognize = true }
                    if (results != null && results.morePagesAvailable == 1) { //more pages are available for this document
                        Toast.makeText(
                            this@CustomRegActivity,
                            "Page ready, flip",
                            Toast.LENGTH_LONG
                        ).show()

                        //letting API know, that all frames will be from different page of the same document, merge same field types
                        DocumentReader.Instance().startNewPage()
                        //                            mPreview.startCameraPreview();
                    } else { //no more pages available
                        val builder =
                            AlertDialog.Builder(this@CustomRegActivity)
                        builder.setPositiveButton(
                            "Ok"
                        ) { dialogInterface, i ->
                            DocumentReader.Instance().startNewSession()
                            dialogInterface.dismiss()
                            synchronized(lock) {
                                isPauseRecognize = false
                            }
                        }
                        builder.setTitle("Processing finished")
                        //getting text field value from results
                        builder.setMessage(
                            if (results == null) "Empty results" else results.getTextFieldValueByType(
                                eVisualFieldType.FT_SURNAME_AND_GIVEN_NAMES
                            )
                        )
                        builder.show()
                    }
                }
                DocReaderAction.ERROR -> {
                    isPauseRecognize = true
                    Toast.makeText(
                        this@CustomRegActivity,
                        "Error: " + if (error != null) error.message else "",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            recognitionFinished = true
        }
    }

    private fun orientationChanged(orientation: Int) {
        synchronized(lock) {
            val degrees: Int = when (orientation) {
                in 316..360 -> 0
                in 0..45 -> 0
                in 46..135 -> -90
                in 136..225 -> 0
                in 256..315 -> 90
                else -> mCurrentDegrees
            }
            if (degrees != mCurrentDegrees) {
                mCurrentDegrees = degrees
            }
        }
    }

    companion object {
        private const val DEBUG = "DEBUG"
        private const val FRAGMENT_TAG = "cameraFragmentTag"
        private const val PERMISSIONS_CAMERA = 1100
        private val lock = Any()
    }
}
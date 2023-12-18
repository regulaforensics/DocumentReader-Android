package com.regula.customcamera_kotlin.custom

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.os.Bundle
import android.util.Size
import android.view.Surface
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.regula.customcamera_kotlin.R

import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.completions.IDocumentReaderCompletion
import com.regula.documentreader.api.enums.DocReaderAction
import com.regula.documentreader.api.enums.eVisualFieldType
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.results.DocumentReaderResults

open class Camera2Activity : AppCompatActivity(), OnImageAvailableListener {
    private var isProcessingFrame = false
    private var recognitionFinished = false
    private var sensorOrientation = 0
    var previewHeight = 0
    var previewWidth = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera2)
        if ((ContextCompat.checkSelfPermission(this@Camera2Activity, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED)
        ) {
            ActivityCompat.requestPermissions(
                this@Camera2Activity,
                arrayOf(Manifest.permission.CAMERA),
                PERMISSIONS_CAMERA
            )
        } else {
            setFragment()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            setFragment()
        } else {
            finish()
        }
    }

    protected open fun setFragment() {
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        var cameraId: String? = null
        try {
            cameraId = manager.cameraIdList[0]
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        val camera2Fragment = CameraConnectionFragment.newInstance(
            object : CameraConnectionFragment.ConnectionCallback {
                override fun onPreviewSizeChosen(size: Size?, cameraRotation: Int) {
                    previewHeight = size!!.height
                    previewWidth = size.width
                    sensorOrientation = cameraRotation - screenOrientation
                }
            },
            this,
            R.layout.camera2_fragment,
            Size(1920, 1080)//setup preview size
        )
        camera2Fragment.setCamera(cameraId)
        supportFragmentManager.beginTransaction().replace(R.id.container, camera2Fragment).commit()
    }


    override fun onImageAvailable(reader: ImageReader) {
        // We need wait until we have some size from onPreviewSizeChosen
        if (previewWidth == 0 || previewHeight == 0) {
            return
        }

        val image = reader.acquireLatestImage() ?: return
        if (!isProcessingFrame && !recognitionFinished) {
            isProcessingFrame = true
            DocumentReader.Instance().recognizeImage(image, completion)
        }
        image.close()
    }

    private val completion = IDocumentReaderCompletion { i: Int, documentReaderResults: DocumentReaderResults?, throwable: DocumentReaderException? ->
        when (i) {
            DocReaderAction.COMPLETE or DocReaderAction.TIMEOUT -> {
                if (documentReaderResults != null
                    && documentReaderResults.morePagesAvailable == 0
                )
                    recognitionFinished = true

                if (documentReaderResults != null) {
                    if (documentReaderResults.morePagesAvailable != 0) { //more pages are available for this document
                        Toast.makeText(
                            this@Camera2Activity,
                            "Page ready, flip",
                            Toast.LENGTH_LONG
                        ).show()
                        //letting API know, that all frames will be from different page of the same document, merge same field types
                        DocumentReader.Instance().startNewPage()
                    } else { //no more pages available
                        startDialog(documentReaderResults)
                    }
                }
            }
            DocReaderAction.ERROR -> {
                recognitionFinished = false
                Toast.makeText(
                    this@Camera2Activity,
                    "Error: " + if (throwable != null) throwable.message else "",
                    Toast.LENGTH_LONG
                ).show()
            }
            DocReaderAction.CANCEL -> {}
        }
        isProcessingFrame = false
    }

    private val screenOrientation: Int
        get() = when (windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_270 -> 270
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_90 -> 90
            else -> 0
        }

    private fun startDialog(documentReaderResults: DocumentReaderResults) {
        val builder = AlertDialog.Builder(this@Camera2Activity)
        builder.setPositiveButton("Ok") { dialogInterface: DialogInterface, i1: Int ->
            DocumentReader.Instance().startNewSession()
            dialogInterface.dismiss()
            recognitionFinished = false
        }
        builder.setTitle("Processing finished")
        //getting text field value from results
        builder.setMessage(documentReaderResults.getTextFieldValueByType(eVisualFieldType.FT_SURNAME_AND_GIVEN_NAMES))
        builder.show()
    }

    companion object {
        private const val PERMISSIONS_CAMERA: Int = 1100
    }
}
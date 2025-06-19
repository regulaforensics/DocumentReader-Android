package com.regula.demo

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.regula.demo.databinding.ActivityMainBinding
import com.regula.documentreader.api.DocumentReader
import com.regula.common.ble.BLEWrapper
import com.regula.common.ble.RegulaBleService
import com.regula.documentreader.api.config.ScannerConfig
import com.regula.documentreader.api.completions.rfid.IRfidReaderCompletion
import com.regula.documentreader.api.enums.*
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.results.DocumentReaderResults
import com.regula.documentreader.api.results.authenticity.DocumentReaderIdentResult
import com.regula.facesdk.FaceSDK
import com.regula.facesdk.configuration.FaceCaptureConfiguration
import com.regula.facesdk.detection.request.OutputImageCrop
import com.regula.facesdk.detection.request.OutputImageParams
import com.regula.facesdk.enums.ImageType
import com.regula.facesdk.enums.OutputImageCropAspectRatio
import com.regula.facesdk.model.MatchFacesImage
import com.regula.facesdk.model.results.FaceCaptureResponse
import com.regula.facesdk.model.results.matchfaces.MatchFacesResponse
import com.regula.facesdk.model.results.matchfaces.MatchFacesSimilarityThresholdSplit
import com.regula.facesdk.request.MatchFacesRequest

class MainActivity : AppCompatActivity() {

    private var bleManager: BLEWrapper? = null
    private var isBleServiceConnected = false
    private var rfidReading = false
    private var matchFaces = true

    private lateinit var binding: ActivityMainBinding

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        if(FaceSDK.Instance().isInitialized) {
            binding.chbMatchFaces.isChecked = true
            binding.chbMatchFaces.setOnClickListener {
                matchFaces = binding.chbMatchFaces.isChecked
                changeMatchFacesVisibility()
            }
        } else{
            binding.chbMatchFaces.isEnabled = false
            binding.chbMatchFaces.text = "use face matching (unavablable)"
        }

        if(!DocumentReader.Instance().isReady) {
            binding.chbMatchFaces.isEnabled = true
            binding.chbMatchFaces.text = "use face matching"
            binding.showScannerBtn.isEnabled = false
        }

        binding.showScannerBtn.setOnClickListener {
            resetViews()
            val scannerConfig = ScannerConfig.Builder(Scenario.SCENARIO_FULL_AUTH).build()
            DocumentReader.Instance().startScanner(this, scannerConfig) { action, results, error ->
                if (action == DocReaderAction.COMPLETE) {
                    showAuthenticityResults(results)
                    //Checking, if nfc chip reading should be performed
                    if (results?.chipPage != 0 && binding.chbRfid.isChecked) {
                        //starting chip reading
                        DocumentReader.Instance().startRFIDReader(this@MainActivity, object: IRfidReaderCompletion() {
                            override fun onCompleted(
                                rfidAction: Int,
                                rfidResults: DocumentReaderResults?,
                                rfidRrror: DocumentReaderException?
                            ) {
                                if (rfidAction == DocReaderAction.COMPLETE || rfidAction == DocReaderAction.CANCEL) {
                                    showGraphicFieldImage(rfidResults)
                                    if (binding.chbMatchFaces.isChecked) {
                                        faceCapture(rfidResults)
                                    }
                                }
                            }
                        })
                    } else {
                        showGraphicFieldImage(results)
                        if (binding.chbMatchFaces.isChecked) {
                            faceCapture(results)
                        }
                    }
                    Log.d(this@MainActivity.localClassName, "completion raw result: " + results?.rawResult)
                } else {
                    //something happened before all results were ready
                    if (action == DocReaderAction.CANCEL) {
                        Toast.makeText(this@MainActivity, "Scanning was cancelled", Toast.LENGTH_LONG).show()
                    } else if (action == DocReaderAction.ERROR) {
                        Toast.makeText(this@MainActivity, "Error:$error", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        val bleIntent = Intent(this, RegulaBleService::class.java)
        bindService(bleIntent, mBleConnection, 0)
    }

    private fun changeMatchFacesVisibility(){
        when (matchFaces){
            true -> {
                binding.portraitCameraIv.visibility = View.VISIBLE
                binding.textViewSimilarity.visibility = View.VISIBLE
            }
            false -> {
                binding.portraitCameraIv.visibility = View.GONE
                binding.textViewSimilarity.visibility = View.GONE
            }
        }
    }

    private fun matchFaces(first: Bitmap, second: Bitmap) {
        val firstImage = MatchFacesImage(first, ImageType.LIVE)
        val secondImage = MatchFacesImage(second, ImageType.PRINTED)
        val matchFacesRequest = MatchFacesRequest(arrayListOf(firstImage, secondImage))

        val crop = OutputImageCrop(
            OutputImageCropAspectRatio.OUTPUT_IMAGE_CROP_ASPECT_RATIO_3X4
        )
        val outputImageParams = OutputImageParams(crop, Color.WHITE)
        matchFacesRequest.outputImageParams = outputImageParams

        FaceSDK.Instance().matchFaces(this@MainActivity, matchFacesRequest) { matchFacesResponse: MatchFacesResponse ->
            matchFacesResponse.exception?.let {
                val errorBuilder = "Error: ${matchFacesResponse.exception?.message} Details: ${matchFacesResponse.exception?.detailedErrorMessage}"
                binding.textViewSimilarity.text = errorBuilder
            } ?: run {
                val split = MatchFacesSimilarityThresholdSplit(matchFacesResponse.results, 0.75)
                val similarity = if (split.matchedFaces.size > 0) {
                    split.matchedFaces[0].similarity
                } else if (split.unmatchedFaces.size > 0){
                    split.unmatchedFaces[0].similarity
                } else {
                    null
                }


                val text = similarity?.let {
                    "Similarity: " + String.format("%.2f", it * 100) + "%"
                } ?: matchFacesResponse.exception?.let {
                    "Similarity: " + it.message
                } ?: "Similarity: "

                binding.textViewSimilarity.text = text

            }
        }
    }

    private fun faceCapture(results: DocumentReaderResults?) {

        val configuration = FaceCaptureConfiguration.Builder().setCameraSwitchEnabled(true).build()

        FaceSDK.Instance().presentFaceCaptureActivity(this@MainActivity, configuration) { faceCaptureResponse: FaceCaptureResponse? ->
            if (faceCaptureResponse?.image != null) {
                binding.portraitCameraIv.setImageBitmap(faceCaptureResponse.image!!.bitmap)


                var portrait: Bitmap? = null
                results?.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT, eRPRM_ResultType.RFID_RESULT_TYPE_RFID_IMAGE_DATA)?.let {
                    portrait = it
                }
                if(portrait == null) {
                    results?.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT)?.let {
                        portrait = it
                    }
                }

                if (matchFaces) {
                    portrait?.let {
                        matchFaces(faceCaptureResponse.image!!.bitmap, it)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBleServiceConnected) {
            unbindService(mBleConnection)
            isBleServiceConnected = false
        }
    }

    private val mBleConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            isBleServiceConnected = true
            val bleService = (service as RegulaBleService.LocalBinder).service
            bleManager = bleService.bleManager
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBleServiceConnected = false
        }
    }

    private fun resetViews() {
        binding.nameTv.text = ""
        binding.textViewSimilarity.text = ""
        binding.portraitIv.setImageResource(R.drawable.portrait)
        binding.portraitCameraIv.setImageResource(R.drawable.portrait)
        binding.documentImageIv.setImageResource(R.drawable.id)
        binding.uvImageView.setImageResource(R.drawable.id)
        binding.uvImageView.visibility = View.GONE
        binding.irImageView.setImageResource(R.drawable.id)
        binding.irImageView.visibility = View.GONE
        binding.authenticityLayout.visibility = View.GONE
    }

    private fun showGraphicFieldImage(results: DocumentReaderResults?) {
        results?.getTextFieldValueByType(eVisualFieldType.FT_SURNAME_AND_GIVEN_NAMES).let {
            binding.nameTv.visibility = View.VISIBLE
            binding.nameTv.text = it
        }

        // through all text fields
        results?.textResult?.fields?.forEach {
            val value = results.getTextFieldValueByType(it.fieldType, it.lcid)
            Log.d("MainActivity", "Text Field: " + it.getFieldName(this@MainActivity) + " value: " + value);
        }

        results?.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT)?.let {
            binding.portraitIv.setImageBitmap(it)
        }

        results?.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT, eRPRM_ResultType.RFID_RESULT_TYPE_RFID_IMAGE_DATA)?.let {
            binding.portraitIv.setImageBitmap(it)
        }

        results?.getGraphicFieldImageByType(eGraphicFieldType.GF_DOCUMENT_IMAGE)?.let {
            val aspectRatio = it.width.toDouble() / it.height.toDouble()
            val documentImage = Bitmap.createScaledBitmap(it, (480 * aspectRatio).toInt(), 480, false)
            binding.documentImageIv.setImageBitmap(documentImage)
        }

        results?.getGraphicFieldByType(eGraphicFieldType.GF_DOCUMENT_IMAGE, eRPRM_ResultType.RPRM_RESULT_TYPE_RAW_IMAGE,
            0, eRPRM_Lights.RPRM_LIGHT_UV)?.let {
            binding.uvImageView.visibility = View.VISIBLE
            binding.uvImageView.setImageBitmap(resizeBitmap(it.bitmap))
        }

        results?.getGraphicFieldByType(eGraphicFieldType.GF_DOCUMENT_IMAGE, eRPRM_ResultType.RPRM_RESULT_TYPE_RAW_IMAGE,
            0, eRPRM_Lights.RPRM_Light_IR_Full)?.let {
            binding.irImageView.visibility = View.VISIBLE
            binding.irImageView.setImageBitmap(resizeBitmap(it.bitmap))
        }
    }

    private fun resizeBitmap(bitmap: Bitmap?): Bitmap? {
        if (bitmap != null) {
            val aspectRatio = bitmap.width.toDouble() / bitmap.height.toDouble()
            return Bitmap.createScaledBitmap(bitmap, (480 * aspectRatio).toInt(), 480, false)
        }
        return null
    }

    private fun showAuthenticityResults(results: DocumentReaderResults?) {
        results?.authenticityResult?.let {
            binding.authenticityLayout.visibility = View.VISIBLE
            binding.authenticityResultImg.setImageResource(if (results.authenticityResult?.status == eCheckResult.CH_CHECK_OK) R.drawable.correct else R.drawable.incorrect)
            binding.layoutOverallResult.visibility = View.VISIBLE
            binding.overallResultImg.setImageResource(if (results.status.overallStatus == eCheckResult.CH_CHECK_OK) R.drawable.correct else R.drawable.incorrect)
            it.checks.forEach { check ->
                for (element in check.elements) {
                    if (element is DocumentReaderIdentResult) {
                        Log.d(
                            "AuthenticityCheck",
                            "Element status: " + (if (element.status == eCheckResult.CH_CHECK_OK) "Ok" else "Error") + ", percent: " + element.percentValue
                        )
                    } else {
                        Log.d(
                            "AuthenticityCheck",
                            "Element type: " + element.elementType + ", status: " + if (element.status == eCheckResult.CH_CHECK_OK) "Ok" else "Error"
                        )
                    }
                }
            }
        } ?: run {
            binding.authenticityLayout.visibility = View.GONE
        }
    }
}

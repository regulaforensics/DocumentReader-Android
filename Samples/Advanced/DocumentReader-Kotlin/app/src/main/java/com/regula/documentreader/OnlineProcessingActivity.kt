package com.regula.documentreader

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.enums.*
import com.regula.documentreader.api.results.DocumentReaderResults
import com.regula.documentreader.api.utils.Common
import com.regula.documentreader.api.utils.JsonUtil
import com.regula.documentreader.databinding.ActivityOnlineProcessingBinding
import org.json.JSONArray
import org.json.JSONObject


class OnlineProcessingActivity : FragmentActivity() {
    @Transient
    private lateinit var binding: ActivityOnlineProcessingBinding

    @Transient
    private var loadingDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnlineProcessingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Helpers.opaqueStatusBar(binding.root)

        binding.galleryBtn.setOnClickListener(OnClickListenerSerializable {
            recognizeImage()
        })
        binding.camerasBtn.setOnClickListener {
            dispatchTakePictureIntent1()
        }
    }


    private fun dispatchTakePictureIntent1() {

        DocumentReader.Instance().processParams().scenario = Scenario.SCENARIO_CAPTURE;
        DocumentReader.Instance().showScanner(this) { action, results, error ->

            if (action == DocReaderAction.COMPLETE) {
                // results.getGraphicFieldByType()
                var imageResult = results?.getGraphicFieldImageByType(
                    eGraphicFieldType.GF_DOCUMENT_IMAGE,
                    eRPRM_ResultType.RPRM_RESULT_TYPE_RAW_IMAGE,
                    0,
                    eRPRM_Lights.RPRM_LIGHT_WHITE_FULL
                )
                val image = Common.toBase64(imageResult)
                sendToWebApi(image);
            }
        };
    }


    private fun createImageBrowsingRequest() {
        val intent = Intent()
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        intent.action = Intent.ACTION_GET_CONTENT
        imageBrowsingIntentLauncher.launch(Intent.createChooser(intent, "Select Picture"))
    }

    fun recognizeImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                Helpers.PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
            )
        } else
            createImageBrowsingRequest()
    }


    @Transient
    val imageBrowsingIntentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.let { intent ->
                    val imageUris = ArrayList<Uri>()
                    if (intent.clipData == null) {
                        intent.data?.let { uri ->
                            imageUris.add(uri)
                        }
                    } else {
                        intent.clipData?.let { clipData ->
                            for (i in 0 until clipData.itemCount) {
                                imageUris.add(clipData.getItemAt(i).uri)
                            }
                        }
                    }
                    if (imageUris.size > 0) {
                        if (imageUris.size == 1) {
                            val image = Common.toBase64(
                                Helpers.getBitmap(
                                    imageUris[0],
                                    1920, 1080, this
                                )
                            )
                            sendToWebApi(image)
                        }
                    }
                }
            }
        }


    private fun sendToWebApi(image: String) {
        val processParam = JSONObject()
            .put("scenario", "FullProcess")
            .put("doublePageSpread", true)
            .put("measureSystem", 0)
            .put("dateFormat", "dd.MM.yyyy")
            .put("alreadyCropped", false)


        val imageObject = JSONObject()
        JsonUtil.safePutKeyValue(imageObject, "image", image.replace("\n", ""))

        val imageData = JSONObject()
            .put("ImageData", imageObject)
            .put("light", 6)
            .put("page_idx", 0)

        val listArray = JSONArray()
            .put(imageData)

        val output = JSONObject()
            .put("processParam", processParam)
            .put("List", listArray)

        postRequest(output.toString())
    }

    override fun onPause() {
        super.onPause()
        hideDialog()
    }

    private fun showDialog(msg: String): AlertDialog {
        val dialog = MaterialAlertDialogBuilder(this)
        dialog.background = ResourcesCompat.getDrawable(resources, R.drawable.rounded, theme)
        dialog.setTitle(msg)
        dialog.setView(layoutInflater.inflate(R.layout.simple_dialog, binding.root, false))
        dialog.setCancelable(false)
        return dialog.show()
    }

    private fun hideDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    private fun postRequest(jsonInputString: String) {
        loadingDialog = showDialog("Getting results from server")
        MainActivity.ENCRYPTED_RESULT_SERVICE
            .httpPost()
            .header(mapOf("Content-Type" to "application/json; utf-8"))
            .body(jsonInputString)
            .responseString { _, _, result ->
                hideDialog()

                when (result) {
                    is Result.Success -> {
                        val dr = DocumentReaderResults.fromRawResults(result.value)
                        if (dr.processingFinishedStatus == 1) {
                            ResultsActivity.results = dr
                            startActivity(Intent(this, ResultsActivity::class.java))
                        }
                    }
                    is Result.Failure -> {
                        println(result.getException())
                        runOnUiThread {
                            MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)
                                .setTitle("Something went wrong")
                                .setMessage("Check your internet connection and try again")
                                .setPositiveButton("Retry") { _, _ ->
                                    postRequest(jsonInputString)
                                }
                                .setNegativeButton("Cancel") { _, _ ->
                                    hideDialog()
                                }
                                .show()
                        }
                    }
                }
            }
    }
}
package com.regula.documentreader

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.regula.documentreader.api.results.DocumentReaderResults
import com.regula.documentreader.api.internal.utils.Common
import com.regula.documentreader.api.internal.utils.JsonUtil
import com.regula.documentreader.databinding.ActivityOnlineProcessingBinding
import org.json.JSONArray
import org.json.JSONObject
import java.io.File


class OnlineProcessingActivity : FragmentActivity() {
    @Transient
    private lateinit var binding: ActivityOnlineProcessingBinding

    @Transient
    private var loadingDialog: AlertDialog? = null

    private var currentBitmap: Bitmap? = null

    private var latestTmpUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnlineProcessingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Helpers.opaqueStatusBar(binding.root)

        binding.requestBtn.setOnClickListener { sendRequest() }
        binding.imageView.setOnClickListener { onCreatePickerDialog() }
        binding.cancelBtn.setOnClickListener {
            setImage(null)
        }
    }

    private fun onCreatePickerDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setItems(
            R.array.select_image
        ) { _, which ->
            if (which == 0) {
                openImage()
            } else {
                launchCamera()
            }
        }
        builder.create().show()
    }

    private fun openImage() {
        requestStoragePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun launchCamera() {
        requestSinglePermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private val requestSinglePermissionLauncher = registerForActivityResult(
        ActivityResultContracts
            .RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            takeImage()
        } else {
            Toast.makeText(
                this@OnlineProcessingActivity,
                "No permission to use the camera",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun takeImage() {
        lifecycleScope.launchWhenStarted {
            getTmpFileUri().let { uri ->
                latestTmpUri = uri
                takeImageResult.launch(uri)
            }
        }
    }

    private val takeImageResult =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
            if (isSuccess) {
                latestTmpUri?.let { uri ->
                    setImage(
                        Helpers.getBitmap(
                            uri,
                            1920, 1080, this
                        )
                    )
                }
            }
        }

    private fun getTmpFileUri(): Uri {
        val tmpFile = File.createTempFile("tmp_image_file", ".jpg", cacheDir)
            .apply {
                deleteOnExit()
            }

        return FileProvider.getUriForFile(
            applicationContext,
            "${BuildConfig.APPLICATION_ID}.provider",
            tmpFile
        )
    }

    private fun createImageBrowsingRequest() {
        val intent = Intent()
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        intent.action = Intent.ACTION_GET_CONTENT
        imageBrowsingIntentLauncher.launch(Intent.createChooser(intent, "Select Picture"))
    }

    private val requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts
            .RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            createImageBrowsingRequest()
        } else {
            Toast.makeText(
                this@OnlineProcessingActivity,
                "No permission to read storage",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    private val imageBrowsingIntentLauncher =
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
                            setImage(
                                Helpers.getBitmap(
                                    imageUris[0],
                                    1920, 1080, this
                                )
                            )
                        }
                    }
                }
            }
        }

    private fun setImage(bitmap: Bitmap?) {
        currentBitmap = bitmap
        binding.imageView.setImageBitmap(bitmap)
        currentBitmap?.let {
            binding.selectImageTv.visibility = View.GONE
            binding.imageView.setBackgroundColor(getColor(android.R.color.transparent))
        } ?: run {
            cacheDir.deleteRecursively()
            binding.imageView.setBackgroundColor(getColor(R.color.light_1))
            binding.selectImageTv.visibility = View.VISIBLE
        }
    }

    private fun sendRequest() {
        currentBitmap?.let {
            sendToWebApi()
        } ?: run {
            Toast.makeText(
                this@OnlineProcessingActivity,
                "Please select an image",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun sendToWebApi() {
        loadingDialog = showDialog("Preparation request")
        Thread {
            val image = Common.toBase64(currentBitmap)
            val request = preparationRequest(image)
            runOnUiThread {
                hideDialog()
                postRequest(request)
            }
        }.start()
    }

    private fun preparationRequest(image: String): String {
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

        return output.toString()
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
}
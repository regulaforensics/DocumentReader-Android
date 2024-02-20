package com.regula.documentreader

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.completions.IDocumentReaderInitCompletion
import com.regula.documentreader.api.config.ScannerConfig
import com.regula.documentreader.api.enums.DocReaderAction
import com.regula.documentreader.api.enums.Scenario
import com.regula.documentreader.api.enums.eGraphicFieldType
import com.regula.documentreader.api.enums.eVisualFieldType
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.params.DocReaderConfig
import com.regula.documentreader.api.results.DocumentReaderResults
import org.json.JSONObject
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private var loadingDialog: AlertDialog? = null
    private var imageView: ImageView? = null
    private var textView: TextView? = null
    private var showScannerBtn: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
        initializeReader()
    }

    private fun initViews() {
        showScannerBtn = findViewById(R.id.showScannerBtn)
        imageView = findViewById(R.id.imageView)
        textView = findViewById(R.id.textView)

        showScannerBtn!!.setOnClickListener {
            resetViews()

            val scannerConfig = ScannerConfig.Builder(Scenario.SCENARIO_FULL_PROCESS).build()
            DocumentReader.Instance().showScanner(
                this, scannerConfig
            ) { action, results, error ->
                if (action == DocReaderAction.COMPLETE || action == DocReaderAction.TIMEOUT) {
                    processResults(results)
                    Log.d(
                        this@MainActivity.localClassName,
                        "completion raw result: " + results?.rawResult
                    )
                } else {
                    //something happened before all results were ready
                    if (action == DocReaderAction.CANCEL) {
                        Toast.makeText(
                            this@MainActivity,
                            "Scanning was cancelled",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    } else if (action == DocReaderAction.ERROR) {
                        Toast.makeText(
                            this@MainActivity,
                            "Error:$error",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun initializeReader() {
        if (!DocumentReader.Instance().isReady) {
            Executors.newSingleThreadExecutor().execute {
                try {
                    val licInput = resources.openRawResource(R.raw.regula)
                    val available = licInput.available()
                    val license = ByteArray(available)
                    licInput.read(license)
                    licInput.close()
                    val handler = Handler(Looper.getMainLooper())
                    handler.post {
                        val docReaderConfig = DocReaderConfig(license)
                        showDialog("Initializing")
                        DocumentReader.Instance()
                            .initializeReader(this@MainActivity, docReaderConfig, initCompletion)
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    dismissDialog()
                    Toast.makeText(
                        this,
                        "init error: " + ex.localizedMessage,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private val initCompletion =
        IDocumentReaderInitCompletion { result: Boolean, error: DocumentReaderException? ->
            dismissDialog()
            if (result) {
                DocumentReader.Instance().processParams().shouldReturnPackageForReprocess = true;
            } else {
                Toast.makeText(this@MainActivity, "Init failed:$error", Toast.LENGTH_LONG).show()
                return@IDocumentReaderInitCompletion
            }
        }

    private fun processResults(results: DocumentReaderResults?) {
        val encrypted = results?.encryptedContainers
        val answer = encrypted?.let { JSONObject(it) }
        answer?.put("processParam", JSONObject().put("scenario", "FullProcess"))
        postRequest(answer.toString())
    }

    private fun postRequest(jsonInputString: String) {
        showDialog("Getting results from server")
        ENCRYPTED_RESULT_SERVICE
            .httpPost()
            .header(mapOf("Content-Type" to "application/json; utf-8"))
            .body(jsonInputString)
            .responseString { _, _, result ->
                when (result) {
                    is Result.Success -> {
                        result.component1()
                            ?.let {
                                val resultResponse = DocumentReaderResults.fromRawResults(it)
                                runOnUiThread {
                                    displayResults(resultResponse)
                                }
                            }
                    }
                    is Result.Failure -> {
                        println(result.getException())
                        runOnUiThread {
                            MaterialAlertDialogBuilder(this)
                                .setTitle("Something went wrong")
                                .setMessage("Check your internet connection and try again")
                                .setPositiveButton("Retry") { _, _ ->
                                    postRequest(jsonInputString)
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                        }
                    }
                }
                dismissDialog()
            }
    }

    private fun displayResults(results: DocumentReaderResults?) {
        showGraphicFieldImage(results)
        val name = results?.getTextFieldValueByType(eVisualFieldType.FT_SURNAME_AND_GIVEN_NAMES);
        name?.let { textView?.text = it }
    }

    private fun resetViews() {
        imageView?.setImageBitmap(null)
        textView?.text = ""
    }

    private fun showGraphicFieldImage(results: DocumentReaderResults?) {
        val documentImage: Bitmap? =
            if (results?.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT) == null) {
                results?.getGraphicFieldImageByType(eGraphicFieldType.GF_DOCUMENT_IMAGE)
            } else {
                results.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT)
            }
        if (documentImage != null)
            imageView?.setImageBitmap(resizeBitmap(documentImage))
    }

    private fun resizeBitmap(bitmap: Bitmap?): Bitmap? {
        if (bitmap != null) {
            val aspectRatio = bitmap.width.toDouble() / bitmap.height.toDouble()
            return Bitmap.createScaledBitmap(bitmap, (480 * aspectRatio).toInt(), 480, false)
        }
        return null
    }

    private fun dismissDialog() {
        if (loadingDialog != null) {
            loadingDialog!!.dismiss()
        }
    }

    private fun showDialog(msg: String?) {
        dismissDialog()
        val builderDialog = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.simple_dialog, null)
        builderDialog.setTitle(msg)
        builderDialog.setView(dialogView)
        builderDialog.setCancelable(false)
        loadingDialog = builderDialog.show()
    }

    companion object {
        const val ENCRYPTED_RESULT_SERVICE = "https://api.regulaforensics.com/api/process"
    }
}
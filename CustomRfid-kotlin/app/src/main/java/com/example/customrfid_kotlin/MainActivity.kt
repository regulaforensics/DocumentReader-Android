package com.example.customrfid_kotlin

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.customrfid_kotlin.databinding.ActivityMainBinding
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.completions.IDocumentReaderCompletion
import com.regula.documentreader.api.completions.IDocumentReaderInitCompletion
import com.regula.documentreader.api.config.ScannerConfig
import com.regula.documentreader.api.enums.*
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.params.DocReaderConfig
import com.regula.documentreader.api.results.DocumentReaderResults
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var loadingDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        initByLicense()

        binding.startCustomRfidReadingBtn.setOnClickListener {
            showScanner()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_READ_PHONE_STATE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showScanner()
            }
            else -> {
                Toast.makeText(this, "need nfc permission", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RFID_RESULT) {
            if (documentReaderResults != null)
                displayResults(documentReaderResults);
        }
    }

    private fun showScanner() {
        val scannerConfig = ScannerConfig.Builder(Scenario.SCENARIO_MRZ).build()
        DocumentReader.Instance().showScanner(this, scannerConfig, completion)
    }

    private fun displayResults(results: DocumentReaderResults?) {
        if (results?.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT) != null) {
            var documentImage =
                results.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT)
            if (documentImage != null) {
                val aspectRatio = documentImage.width.toDouble() / documentImage.height
                    .toDouble()
                documentImage = Bitmap.createScaledBitmap(
                    documentImage,
                    (480 * aspectRatio).toInt(), 480, false
                )
                binding.resultIv.setImageBitmap(documentImage)
            }
        }
    }

    private fun initByLicense() {
        if (DocumentReader.Instance().isReady) return

        showDialog("initializing")
        Executors.newSingleThreadExecutor().execute {
            initializeReader()
        }
    }

    private fun initializeReader() {
        try {
            val licInput = resources.openRawResource(R.raw.regula)
            val available = licInput.available()
            val license = ByteArray(available)
            licInput.read(license)
            licInput.close()
            val handler = Handler(Looper.getMainLooper())
            handler.post {
                val docReaderConfig = DocReaderConfig(license)
                DocumentReader.Instance()
                DocumentReader.Instance()
                    .initializeReader(this@MainActivity, docReaderConfig, initCompletion)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            Toast.makeText(
                this,
                "init error: " + ex.localizedMessage,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val completion =
        IDocumentReaderCompletion { action, results, error ->
            //processing is finished, all results are ready
            if (action == DocReaderAction.COMPLETE) {
                dismissDialog()
                if ( results!!.chipPage != 0) {
                    //starting chip reading
                    val rfidIntent =
                                Intent(this@MainActivity, CustomRfidActivity::class.java)
                            startActivityForResult(rfidIntent, RFID_RESULT)
                }
            } else {
                //something happened before all results were ready
                if (action == DocReaderAction.CANCEL) {
                    Toast.makeText(this@MainActivity, "Scanning was cancelled", Toast.LENGTH_LONG)
                        .show()
                } else if (action == DocReaderAction.ERROR) {
                    Toast.makeText(this@MainActivity, "Error:$error", Toast.LENGTH_LONG).show()
                }
            }
        }
    private val initCompletion =
        IDocumentReaderInitCompletion { result: Boolean, error: DocumentReaderException? ->
            dismissDialog()
            if (!result) { //Initialization was not successful
                Toast.makeText(this@MainActivity, "Init failed:$error", Toast.LENGTH_LONG).show()
                return@IDocumentReaderInitCompletion
            }
            successfulInit()
        }

    private fun dismissDialog() {
        if (loadingDialog == null) return
        loadingDialog?.dismiss()
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

    private fun successfulInit() {
        binding.startCustomRfidReadingBtn.isEnabled = true
    }

    companion object {
        var documentReaderResults: DocumentReaderResults? = null
        var RFID_RESULT = 100
        val REQUEST_READ_PHONE_STATE = 1
    }
}
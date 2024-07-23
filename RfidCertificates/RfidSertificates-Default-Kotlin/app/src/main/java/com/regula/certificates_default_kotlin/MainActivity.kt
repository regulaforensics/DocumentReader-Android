package com.regula.certificates_default_kotlin

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.regula.certificates_default_kotlin.CertificatesUtil.getRfidCertificate
import com.regula.certificates_default_kotlin.databinding.ActivityMainBinding
import com.regula.documentreader.api.DocumentReader.Instance
import com.regula.documentreader.api.completions.IDocumentReaderCompletion
import com.regula.documentreader.api.completions.IDocumentReaderInitCompletion
import com.regula.documentreader.api.config.ScannerConfig
import com.regula.documentreader.api.completions.rfid.IRfidReaderCompletion
import com.regula.documentreader.api.enums.DocReaderAction
import com.regula.documentreader.api.enums.Scenario
import com.regula.documentreader.api.enums.eCheckResult
import com.regula.documentreader.api.enums.eGraphicFieldType
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.params.DocReaderConfig
import com.regula.documentreader.api.params.rfid.PKDCertificate
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
        Instance().processParams().setLogs(true)
        Instance().setEnableCoreLogs(true)
        initializeReader()

        binding.showScannerBtn.setOnClickListener {
            clearResults()

            val scannerConfig = ScannerConfig.Builder(Scenario.SCENARIO_MRZ).build()

            Instance().showScanner(this, scannerConfig, completion)
        }
    }

    private val completion =
        IDocumentReaderCompletion { action, results, error ->
            //processing is finished, all results are ready
            if (action == DocReaderAction.COMPLETE) {
                dismissDialog()
                readRfid()
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

    private fun initializeReader() {
        showDialog("initializing")
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
                    Instance()
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
    }

    private fun readRfid() {
        // uncomment this line in case you have valid certificates for Terminal Authentication
        // Instance().rfidScenario().ePassportDataGroups().isDG3 = true

        Instance().startRFIDReader(
            this, object: IRfidReaderCompletion() {
                override fun onCompleted(
                    rfidAction: Int,
                    results: DocumentReaderResults?,
                    error: DocumentReaderException?
                ) {
                    if (rfidAction == DocReaderAction.COMPLETE || rfidAction == DocReaderAction.CANCEL) {
                        results?.let {
                            displayResults(it)
                        }
                    } else if (rfidAction == DocReaderAction.ERROR) {
                        Toast.makeText(this@MainActivity, error!!.message, Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        )
    }

    private fun displayResults(documentReaderResults: DocumentReaderResults) {
        displayImage(documentReaderResults)
        displayTextFields(documentReaderResults)
    }

    private fun displayImage(results: DocumentReaderResults?) {
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

    private fun displayTextFields(results: DocumentReaderResults?) {
        if (results?.status?.detailsRFID != null) {
            val rfidResults = results.status.detailsRFID
            val paStatus = "PA status:" + resultStatusParse(rfidResults.pa)
            binding.paStatusTv.text = paStatus

            val taStatus = "TA status: " + resultStatusParse(rfidResults.ta)
            binding.taStatusTv.text = taStatus
        } else {
            binding.paStatusTv.text = "PA status:NONE"
            binding.taStatusTv.text = "TA status:NONE"
        }
    }

    private val initCompletion =
        IDocumentReaderInitCompletion { result: Boolean, error: DocumentReaderException? ->
            dismissDialog()
            if (result) {
                if (Instance().isRFIDAvailableForUse)
                    addRfidCertificates()
                else Toast.makeText(this, "rfid is not available for use", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(this@MainActivity, "Init failed:$error", Toast.LENGTH_LONG).show()
                return@IDocumentReaderInitCompletion
            }
        }

    private fun addRfidCertificates() {
        val certificates: List<PKDCertificate> = ArrayList(getRfidCertificate(assets))
        if (certificates.isNotEmpty()) {
            Instance().addPKDCertificates(certificates)
        }
    }

    private fun clearResults() {
        binding.paStatusTv.text = "PA Status:"
        binding.taStatusTv.text = "TA Status:"
        binding.resultIv.setImageBitmap(null)
    }

    fun setTitleDialog(msg: String?) {
        if (loadingDialog != null) {
            loadingDialog!!.setTitle(msg)
        } else {
            showDialog(msg)
        }
    }

    fun dismissDialog() {
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

    private fun resultStatusParse(value: Int): String {
        when (value) {
            eCheckResult.CH_CHECK_WAS_NOT_DONE -> return "check was not done"
            eCheckResult.CH_CHECK_OK -> return "ok"
            eCheckResult.CH_CHECK_ERROR -> return "error"
        }
        return ""
    }
}
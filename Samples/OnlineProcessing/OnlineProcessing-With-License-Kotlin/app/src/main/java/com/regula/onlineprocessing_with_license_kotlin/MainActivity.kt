package com.regula.onlineprocessing_with_license_kotlin

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.completions.IDocumentReaderCompletion
import com.regula.documentreader.api.completions.IDocumentReaderInitCompletion
import com.regula.documentreader.api.completions.IDocumentReaderPrepareCompletion
import com.regula.documentreader.api.enums.*
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.params.DocReaderConfig
import com.regula.documentreader.api.params.OnlineProcessingConfig
import com.regula.documentreader.api.results.DocumentReaderResults
import com.regula.onlineprocessing_with_license_kotlin.databinding.ActivityMainBinding
import com.regula.onlineprocessing_with_license_kotlin.util.Constants
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private var loadingDialog: AlertDialog? = null
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepareDatabase()
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.showScannerBtn.setOnClickListener {
            binding.surnameTv.text = "Surname:"
            binding.nameTv.text = "Name:"
            binding.resultIv.setImageBitmap(null)
            DocumentReader.Instance().showScanner(this, completion)
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
    }

    private val initCompletion =
        IDocumentReaderInitCompletion { result: Boolean, error: DocumentReaderException? ->
            dismissDialog()

            if (result) {
                if (DocumentReader.Instance().availableScenarios.size > 0) {
                    setupOnlineProcessing()
                    DocumentReader.Instance().processParams()
                        .setScenario(Scenario.SCENARIO_FULL_PROCESS)
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Available scenarios list is empty",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.showScannerBtn.isEnabled = false
                }
            } else {
                Toast.makeText(this@MainActivity, "Init failed:$error", Toast.LENGTH_LONG).show()
                return@IDocumentReaderInitCompletion
            }
        }

    private val completion =
        IDocumentReaderCompletion { action, results, error ->
            //processing is finished, all results are ready
            if (action == DocReaderAction.COMPLETE) {
                dismissDialog()
                displayImage(results)
                displayTextFields(results)
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

    private fun setupOnlineProcessing() {
        val onlineProcessingConfiguration = OnlineProcessingConfig.Builder(OnlineMode.AUTO)
            .setUrl(Constants.BASE_URL)
            .build()
        onlineProcessingConfiguration.processParam.scenario = Scenario.SCENARIO_FULL_PROCESS;

        DocumentReader.Instance().functionality().edit()
            .setOnlineProcessingConfiguration(onlineProcessingConfiguration)
            .setForcePagesCount(2)
            .apply();
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
        if (results?.getTextFieldByType(eVisualFieldType.FT_SURNAME) != null) {
            val surname = "Surname:" + results.getTextFieldValueByType(eVisualFieldType.FT_SURNAME)
            binding.surnameTv.text = surname
        } else {
            binding.surnameTv.text = "Surname:"
        }

        if (results?.getTextFieldByType(eVisualFieldType.FT_GIVEN_NAMES) != null) {
            val name = "Name: " + results.getTextFieldValueByType(eVisualFieldType.FT_GIVEN_NAMES)
            binding.nameTv.text = name
        } else {
            binding.nameTv.text = "Name:"
        }
    }

    private fun prepareDatabase() {
        showDialog("preparing database")
        DocumentReader.Instance()
            .prepareDatabase(//call prepareDatabase not necessary if you have local database at assets/Regula/db.dat
                this@MainActivity,
                "Full",
                object : IDocumentReaderPrepareCompletion {
                    override fun onPrepareProgressChanged(progress: Int) {
                        if (loadingDialog != null)
                            loadingDialog!!.setTitle("Downloading database: $progress%")
                    }

                    override fun onPrepareCompleted(
                        status: Boolean,
                        error: DocumentReaderException?
                    ) {
                        if (!status) {
                            Toast.makeText(
                                this@MainActivity,
                                "Prepare DB failed:$error",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            initializeReader()
                        }
                    }
                })
    }
}
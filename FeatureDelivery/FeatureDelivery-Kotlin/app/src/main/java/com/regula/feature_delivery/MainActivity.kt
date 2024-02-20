package com.regula.feature_delivery

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.play.core.splitcompat.SplitCompat
import com.google.android.play.core.splitinstall.*
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.completions.IDocumentReaderCompletion
import com.regula.documentreader.api.completions.IDocumentReaderInitCompletion
import com.regula.documentreader.api.config.ScannerConfig
import com.regula.documentreader.api.enums.DocReaderAction
import com.regula.documentreader.api.enums.Scenario
import com.regula.documentreader.api.enums.eGraphicFieldType
import com.regula.documentreader.api.enums.eVisualFieldType
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.params.DocReaderConfig
import com.regula.documentreader.api.results.DocumentReaderResults
import com.regula.feature_delivery.databinding.ActivityMainBinding
import java.util.concurrent.Executors

private const val coreModule = "regula_core_sdk"

class MainActivity : AppCompatActivity() {
    private var loadingDialog: AlertDialog? = null
    private lateinit var binding: ActivityMainBinding

    private lateinit var splitInstallManager: SplitInstallManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        splitInstallManager = SplitInstallManagerFactory.create(this)
        binding.showScannerBtn.setOnClickListener {
            binding.surnameTv.text = "Surname:"
            binding.nameTv.text = "Name:"
            binding.resultIv.setImageBitmap(null)
            val scannerConfig = ScannerConfig.Builder(Scenario.SCENARIO_FULL_PROCESS).build()
            DocumentReader.Instance().showScanner(this, scannerConfig, completion)
        }
        binding.loadBtn.setOnClickListener {
            loadAndLaunchModule(coreModule)
        }
        checkModule()
    }

    private fun checkModule() {
        if (splitInstallManager.installedModules.contains(coreModule)) {
            SplitCompat.install(this)
            initializeReader()
        } else {
            binding.loadBtn.visibility = View.VISIBLE;
            binding.nameTv.text = "Regula Core SDK not found"
        }
    }

    override fun onResume() {
        splitInstallManager.registerListener(listener)
        super.onResume()
    }

    override fun onPause() {
        splitInstallManager.unregisterListener(listener)
        super.onPause()
    }

    private val listener = SplitInstallStateUpdatedListener { state ->
        val names = state.moduleNames().joinToString(" - ")
        when (state.status()) {
            SplitInstallSessionStatus.DOWNLOADING -> {
                displayLoadingState(state, "Downloading $names")
            }
            SplitInstallSessionStatus.INSTALLED -> {
                SplitCompat.install(this)
                onSuccessfulLoad(names)
            }
            SplitInstallSessionStatus.INSTALLING -> displayLoadingState(state, "Installing $names")
            SplitInstallSessionStatus.FAILED -> {
                Toast.makeText(
                    this,
                    "Error: ${state.errorCode()} for module ${state.moduleNames()}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun displayLoadingState(state: SplitInstallSessionState, message: String) {
        displayProgress()

        binding.progressBar.max = state.totalBytesToDownload().toInt()
        binding.progressBar.progress = state.bytesDownloaded().toInt()

        updateProgressMessage(message)
    }

    private fun updateProgressMessage(message: String) {
        if (binding.progress.visibility != View.VISIBLE)
            displayProgress()
        binding.progressText.text = message
    }

    private fun displayProgress() {
        binding.progress.visibility = View.VISIBLE
    }

    private fun onSuccessfulLoad(names: String) {
        Toast.makeText(this, "Module $names successful Loaded", Toast.LENGTH_LONG).show()
        binding.loadBtn.visibility = View.GONE;
        binding.nameTv.text = ""
        binding.progress.visibility = View.GONE
        if(!DocumentReader.Instance().isReady) {
            initializeReader()
        }
    }

    private fun loadAndLaunchModule(name: String) {
        updateProgressMessage("Loading module $name")
        if (splitInstallManager.installedModules.contains(name)) {
            updateProgressMessage("Already installed")
            onSuccessfulLoad(name)
            return
        }

        val request = SplitInstallRequest.newBuilder()
            .addModule(name)
            .build()

        splitInstallManager.startInstall(request)
            .addOnFailureListener {
                Toast.makeText(this, "Failure $it", Toast.LENGTH_SHORT).show()
                binding.progress.visibility = View.GONE
            }

        updateProgressMessage("Starting install for $name")
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
                        .initializeReader(this, docReaderConfig, initCompletion)

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
                if (DocumentReader.Instance().availableScenarios.size == 0) {
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
}
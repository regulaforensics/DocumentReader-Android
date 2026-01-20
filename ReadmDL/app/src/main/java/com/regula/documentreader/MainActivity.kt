package com.regula.documentreader

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.enums.eCheckResult
import com.regula.documentreader.api.enums.eGraphicFieldType
import com.regula.documentreader.api.enums.eMDLDeviceEngagement
import com.regula.documentreader.api.enums.eMDLDeviceRetrieval
import com.regula.documentreader.api.enums.eMDLDocRequestPreset
import com.regula.documentreader.api.enums.eMDLIntentToRetain
import com.regula.documentreader.api.enums.eRPRM_ResultType
import com.regula.documentreader.api.enums.eVisualFieldType
import com.regula.documentreader.api.params.mdl.DataRetrieval
import com.regula.documentreader.api.params.rfid.PKDCertificate
import com.regula.documentreader.api.results.DocumentReaderResults
import com.regula.documentreader.databinding.ActivityMainBinding
import com.regula.documentreader.util.Utils


open class MainActivity : AppCompatActivity() {

    private var loadingDialog: AlertDialog? = null

    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val viewModel: MainViewModel by lazy {
        ViewModelProvider(this, MainViewModelFactory(DocumentReader.Instance())).get(MainViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val view = binding.root
        setContentView(view)

        initView()

        viewModel.initLiveData.observe(this) {
            dismissDialog()
            it?.let {
                disableUiElements()
                Toast.makeText(this@MainActivity, "Init failed: ${it.message}", Toast.LENGTH_LONG).show()
            } ?: successfulInit()
        }

        viewModel.showScannerSuccessCompletion.observe(this) { results ->
            dismissDialog()
            displayResults(results)
        }

        viewModel.showScannerErrorCompletion.observe(this) {
            dismissDialog()
            Toast.makeText(this@MainActivity, "Error: $it", Toast.LENGTH_LONG).show()
        }

        viewModel.showScannerCancelCompletion.observe(this) {
            dismissDialog()
            Toast.makeText(this@MainActivity, "Canceled by user", Toast.LENGTH_LONG).show()
        }

        if (DocumentReader.Instance().isReady) {
            successfulInit()
            return
        }

        showDialog("Initializing...")
        viewModel.init (this)
    }

    private fun disableUiElements() {
        binding.readNfc.isEnabled = false
        binding.readBle.isEnabled = false
    }

    private fun successfulInit() {
        DocumentReader.Instance().processParams().debugSaveLogs = true;
        addRfidCertificates()
    }

    private fun addRfidCertificates() {
        val certificates: List<PKDCertificate> =
            Utils.getRfidCertificates(
                this, "Regula/mdl"
            )

        if (!certificates.isEmpty()) {
            DocumentReader.Instance().addPKDCertificates(certificates)
        }
    }

    private fun dismissDialog() {
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

    private fun initView() {
        binding.readBle.setOnClickListener {
            clearResults()
            viewModel.readMDL(this@MainActivity, eMDLDeviceEngagement.QR, DataRetrieval(eMDLDeviceRetrieval.BLE))
        }
        binding.readNfc.setOnClickListener {
            clearResults()
            viewModel.readMDL(this@MainActivity, eMDLDeviceEngagement.NFC, DataRetrieval(eMDLDeviceRetrieval.NFC))
        }
        binding.dl.setOnClickListener {
            clearResults()
            val dataRetrieval = DataRetrieval(eMDLDeviceRetrieval.BLE)
            dataRetrieval.setDocRequestPreset(eMDLDocRequestPreset.DRIVERS_LICENSE, eMDLIntentToRetain.FALSE)
            viewModel.readMDL(this@MainActivity, eMDLDeviceEngagement.QR, dataRetrieval)
        }
        binding.standardId.setOnClickListener {
            clearResults()
            val dataRetrieval = DataRetrieval(eMDLDeviceRetrieval.BLE)
            dataRetrieval.setDocRequestPreset(eMDLDocRequestPreset.STANDARD_ID, eMDLIntentToRetain.FALSE)
            viewModel.readMDL(this@MainActivity, eMDLDeviceEngagement.QR, dataRetrieval)
        }
        binding.travelId.setOnClickListener {
            clearResults()
            val dataRetrieval = DataRetrieval(eMDLDeviceRetrieval.BLE)
            dataRetrieval.setDocRequestPreset(eMDLDocRequestPreset.TRAVEL, eMDLIntentToRetain.FALSE)
            viewModel.readMDL(this@MainActivity, eMDLDeviceEngagement.QR, dataRetrieval)
        }
        binding.age.setOnClickListener {
            clearResults()
            val dataRetrieval = DataRetrieval(eMDLDeviceRetrieval.BLE)
            dataRetrieval.setDocRequestPreset(eMDLDocRequestPreset.AGE, eMDLIntentToRetain.FALSE)
            viewModel.readMDL(this@MainActivity, eMDLDeviceEngagement.QR, dataRetrieval)
        }
    }

    private fun displayResults(results: DocumentReaderResults?) {
        if (results != null) {
            val name = results.getTextFieldValueByType(eVisualFieldType.FT_SURNAME_AND_GIVEN_NAMES)
            if (name != null) {
                binding.nameTv.text = name
            }

            binding.field.text = ""
            // through all text fields
            results.textResult?.fields?.forEach {
                val value = results.getTextFieldValueByType(it.fieldType, it.lcid)
                Log.d("MainActivity", "Text Field: " + it.getFieldName(applicationContext)  + " value: " + value);
                binding.field.append( it.getFieldName(applicationContext)  + ": " + value + "\n")
            }
            results.status.let {
                it.mdl
                Log.d("MainActivity",  "mdl: " + it.mdl)

                binding.field.append("\nmDL: " + getStatus(it.mdl) + "\n")
                binding.field.append("age: " + getStatus(it.age) + "\n")
                binding.field.append("over18: " + getStatus(it.detailsAge.over18) + "\n")
                binding.field.append("over21: " + getStatus(it.detailsAge.over21) + "\n")
                binding.field.append("over25: " + getStatus(it.detailsAge.over25) + "\n")
                binding.field.append("over65: " + getStatus(it.detailsAge.over65) + "\n")
                binding.field.append("overThreshold: " + getStatus(it.detailsAge.overThreshold) + "\n")
                binding.field.append("threshold: " + it.detailsAge.threshold + "\n")
            }

            results.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT)?.let {
                binding.portraitIv.setImageBitmap(it)
            }
            results.getGraphicFieldImageByType(eGraphicFieldType.GF_SIGNATURE)?.let {
                binding.signature.setImageBitmap(it)
            }
            results.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT, eRPRM_ResultType.RFID_RESULT_TYPE_RFID_IMAGE_DATA)?.let {
                binding.portraitIv.setImageBitmap(it)
            }
        }
    }

    fun getStatus(f: Int): String {
        var status = "-"
        if(f == eCheckResult.CH_CHECK_OK)
            status = "OK"
        else if(f == eCheckResult.CH_CHECK_ERROR)
            status = "Error"
        return  status;
    }

    private fun clearResults() {
        binding.nameTv.text = ""
        binding.field.text = ""
        binding.portraitIv.setImageResource(R.drawable.portrait)
        binding.signature.setImageDrawable(null)
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)

        applyEdgeToEdgeInsets()
    }

    private fun applyEdgeToEdgeInsets() {
        val rootView = window.decorView.findViewWithTag<View>("content")
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
                val systemBars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            or WindowInsetsCompat.Type.displayCutout()
                )
                view.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    systemBars.bottom
                )
                insets
            }
        }
    }
}
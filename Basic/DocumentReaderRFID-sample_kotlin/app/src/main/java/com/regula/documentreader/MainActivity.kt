package com.regula.documentreader

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.enums.eGraphicFieldType
import com.regula.documentreader.api.enums.eRFID_Password_Type
import com.regula.documentreader.api.enums.eRPRM_ResultType
import com.regula.documentreader.api.enums.eVisualFieldType
import com.regula.documentreader.api.results.DocumentReaderResults
import com.regula.documentreader.databinding.ActivityMainBinding
import androidx.core.content.edit

open class MainActivity : AppCompatActivity() {
    private var loadingDialog: AlertDialog? = null

    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val sharedPreferences: SharedPreferences by lazy { getSharedPreferences(MY_SHARED_PREFS, MODE_PRIVATE) }
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

        viewModel.rfidCompletion.observe(this) {
            displayResults(it)
        }

        if (DocumentReader.Instance().isReady) {
            successfulInit()
            return
        }

        showDialog("Initializing...")
        viewModel.init (this)
    }

    private fun disableUiElements() {
        binding.showScannerLink.isClickable = false

        binding.showScannerLink.setTextColor(Color.GRAY)
    }


    private fun successfulInit() {
        if (DocumentReader.Instance().isRFIDAvailableForUse)
            binding.showScannerLink.isClickable = true
        else
            binding.showScannerLink.isClickable = false

        sharedPreferences.getString(SCENARIO, null)
            ?.let { DocumentReader.Instance().rfidScenario().fromJson(it) }

        when (DocumentReader.Instance().rfidScenario().getPacePasswordType()) {
            eRFID_Password_Type.PPT_CAN -> {
                binding.canRb.setChecked(true)
                binding.passwordEt.setText(DocumentReader.Instance().rfidScenario().getPassword())
            }

            eRFID_Password_Type.PPT_MRZ -> {
                binding.mrzRb.setChecked(true)
                binding.passwordEt.setText(DocumentReader.Instance().rfidScenario().getMrz())
            }

            eRFID_Password_Type.PPT_PIN -> {
                binding.pinRb.setChecked(true)
                binding.passwordEt.setText(DocumentReader.Instance().rfidScenario().getPassword())
            }

            eRFID_Password_Type.PPT_PUK -> {
                binding.pukRb.setChecked(true)
                binding.passwordEt.setText(DocumentReader.Instance().rfidScenario().getPassword())
            }

            eRFID_Password_Type.PPT_SAI -> {
                binding.saiRb.setChecked(true)
                binding.passwordEt.setText(DocumentReader.Instance().rfidScenario().getPassword())
            }
        }

        binding.canRb.setOnCheckedChangeListener { _: CompoundButton?, b: Boolean ->
            if (b) {
                DocumentReader.Instance().rfidScenario()
                    .setPacePasswordType(eRFID_Password_Type.PPT_CAN)
                binding.passwordEt.setText(DocumentReader.Instance().rfidScenario().getPassword())
            }
        }
        binding.mrzRb.setOnCheckedChangeListener { _: CompoundButton?, b: Boolean ->
            if (b) {
                DocumentReader.Instance().rfidScenario()
                    .setPacePasswordType(eRFID_Password_Type.PPT_MRZ)
                binding.passwordEt.setText(DocumentReader.Instance().rfidScenario().getMrz())
            }
        }
        binding.pinRb.setOnCheckedChangeListener { _: CompoundButton?, b: Boolean ->
            if (b) {
                DocumentReader.Instance().rfidScenario()
                    .setPacePasswordType(eRFID_Password_Type.PPT_PIN)
                binding.passwordEt.setText(DocumentReader.Instance().rfidScenario().getPassword())
            }
        }
        binding.pukRb.setOnCheckedChangeListener { _: CompoundButton?, b: Boolean ->
            if (b) {
                DocumentReader.Instance().rfidScenario()
                    .setPacePasswordType(eRFID_Password_Type.PPT_PUK)
                binding.passwordEt.setText(DocumentReader.Instance().rfidScenario().getPassword())
            }
        }
        binding.saiRb.setOnCheckedChangeListener { _: CompoundButton?, b: Boolean ->
            if (b) {
                DocumentReader.Instance().rfidScenario()
                    .setPacePasswordType(eRFID_Password_Type.PPT_SAI)
                binding.passwordEt.setText(DocumentReader.Instance().rfidScenario().getPassword())
            }
        }

        binding.passwordEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {
            }

            override fun onTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {
                if (DocumentReader.Instance().rfidScenario()
                        .getPacePasswordType() == eRFID_Password_Type.PPT_MRZ
                ) {
                    DocumentReader.Instance().rfidScenario().setMrz(charSequence.toString())
                } else {
                    DocumentReader.Instance().rfidScenario().setPassword(charSequence.toString())
                }
            }

            override fun afterTextChanged(editable: Editable?) {
            }
        })
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
        binding.showScannerLink.setOnClickListener {
            clearResults()
            sharedPreferences.edit {
                putString(
                    SCENARIO,
                    DocumentReader.Instance().rfidScenario().toJson()
                )
            }
            viewModel.startRFIDReader(this@MainActivity)
        }
    }

    private fun displayResults(results: DocumentReaderResults?) {
        if (results != null) {
            val name = results.getTextFieldValueByType(eVisualFieldType.FT_SURNAME_AND_GIVEN_NAMES)
            if (name != null) {
                binding.nameTv.text = name
            }

            // through all text fields
            results.textResult?.fields?.forEach {
                val value = results.getTextFieldValueByType(it.fieldType, it.lcid)
                Log.d("MainActivity", "Text Field: " + it.getFieldName(applicationContext)  + " value: " + value);
            }

            results.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT)?.let {
                binding.portraitIv.setImageBitmap(it)
            }
            results.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT, eRPRM_ResultType.RFID_RESULT_TYPE_RFID_IMAGE_DATA)?.let {
                binding.portraitIv.setImageBitmap(it)
            }
        }
    }

    private fun clearResults() {
        binding.nameTv.text = ""
        binding.portraitIv.setImageResource(R.drawable.portrait)
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

    companion object {
        private const val MY_SHARED_PREFS = "MySharedPrefs"
        private const val SCENARIO = "rfidTestScenario"
    }
}
package com.regula.documentreader

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ContentView
import androidx.annotation.NonNull
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.ViewCompat.onApplyWindowInsets
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.enums.eGraphicFieldType
import com.regula.documentreader.api.enums.eRPRM_ResultType
import com.regula.documentreader.api.enums.eVisualFieldType
import com.regula.documentreader.api.results.DocumentReaderResults
import com.regula.documentreader.databinding.ActivityMainBinding
import com.regula.documentreader.util.Utils

open class MainActivity : AppCompatActivity() {

    private var loadingDialog: AlertDialog? = null

    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val sharedPreferences: SharedPreferences by lazy { getSharedPreferences(MY_SHARED_PREFS, MODE_PRIVATE) }
    private val viewModel: MainViewModel by lazy {
        ViewModelProvider(this, MainViewModelFactory(DocumentReader.Instance(), sharedPreferences)).get(MainViewModel::class.java)
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

            //Checking, if nfc chip reading should be performed
            if (binding.doRfidCb.isChecked && results != null && results.chipPage != 0) {

                //starting chip reading
                viewModel.startRFIDReader(this@MainActivity)
            } else {
                displayResults(results)
            }
        }

        viewModel.rfidCompletion.observe(this) {
            displayResults(it)
        }

        viewModel.showScannerErrorCompletion.observe(this) {
            dismissDialog()
            Toast.makeText(this@MainActivity, "Error: $it", Toast.LENGTH_LONG).show()
        }

        viewModel.showScannerCancelCompletion.observe(this) {
            dismissDialog()
            Toast.makeText(this@MainActivity, "Canceled by user", Toast.LENGTH_LONG).show()
        }

        viewModel.doRfidData.observe(this) {
            binding.doRfidCb.isChecked = it
        }

        if (DocumentReader.Instance().isReady) {
            successfulInit()
            return
        }

        showDialog("Initializing...")
        viewModel.init (this)
    }

    private fun disableUiElements() {
        binding.recognizePdfLink.isClickable = false
        binding.showScannerLink.isClickable = false
        binding.recognizeImageLink.isClickable = false

        binding.recognizePdfLink.setTextColor(Color.GRAY)
        binding.showScannerLink.setTextColor(Color.GRAY)
        binding.recognizeImageLink.setTextColor(Color.GRAY)
    }

    private fun scenarioLv(item: String?) {
        //setting selected scenario to DocumentReader params
        item?.let {
            viewModel.scenario(it)
        }
    }

    private fun successfulInit() {
        //getting current processing scenario and loading available scenarios to ListView
        if (DocumentReader.Instance().availableScenarios.isNotEmpty()) {
            if (DocumentReader.Instance().isRFIDAvailableForUse)
                binding.doRfidCb.visibility = View.VISIBLE
            else
                binding.doRfidCb.visibility = View.GONE

            setScenarios()
            return
        }

        disableUiElements()
        Toast.makeText(
            this@MainActivity,
            "Available scenarios list is empty",
            Toast.LENGTH_SHORT
        ).show()
    }

    // creates and starts image browsing intent
    // results will be handled in onActivityResult method
    private fun createImageBrowsingRequest() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        browsePictureResultLauncher.launch(intent)
    }

    private var browsePictureResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data

            val selectedImage = data?.data
            if (selectedImage == null) {
                Toast.makeText(this@MainActivity, "Something wrong with selected image", Toast.LENGTH_LONG).show()
                return@registerForActivityResult
            }

            val bmp: Bitmap? = Utils.getBitmap(contentResolver, selectedImage, 1920, 1080)
            bmp?.let {
                showDialog("Processing image")
                viewModel.recognize(context = this@MainActivity, it)
            }
        }
    }

    private fun setTitleDialog(msg: String?) {
        loadingDialog?.setTitle(msg) ?: showDialog(msg)
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
        binding.recognizePdfLink.setOnClickListener {
            clearResults()
            showDialog("Processing pdf")
            viewModel.recognizePdf(this)
        }
        binding.recognizeImageLink.setOnClickListener {
            if (!DocumentReader.Instance().isReady) return@setOnClickListener
            clearResults()
            createImageBrowsingRequest()
        }
        binding.showScannerLink.setOnClickListener {
            clearResults()
            viewModel.showScanner(this@MainActivity)
        }
        binding.scenariosList.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView: AdapterView<*>, _: View?, i: Int, _: Long ->
                val adapter = adapterView.adapter as ScenarioAdapter
                scenarioLv(adapter.getItem(i))
                adapter.setSelectedPosition(i)
                adapter.notifyDataSetChanged()
            }
        binding.doRfidCb.setOnCheckedChangeListener { _, checked ->
            viewModel.doRfid(checked)
        }
    }

    fun setScenarios() {
        val scenarios = ArrayList<String>()
        for (scenario in DocumentReader.Instance().availableScenarios) {
            scenarios.add(scenario.name)
        }

        if (scenarios.isNotEmpty()) {
            //setting default scenario
            val adapter = ScenarioAdapter(this@MainActivity, android.R.layout.simple_list_item_1, scenarios)
            adapter.setSelectedPosition(0)
            binding.scenariosList.adapter = adapter
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
            results.getGraphicFieldImageByType(eGraphicFieldType.GF_DOCUMENT_IMAGE)?.let {
                val aspectRatio = it.width.toDouble() / it.height.toDouble()
                val documentImage = Bitmap.createScaledBitmap(it, (480 * aspectRatio).toInt(), 480, false)
                binding.documentImageIv.setImageBitmap(documentImage)
            }
        }
    }

    private fun clearResults() {
        binding.nameTv.text = ""
        binding.portraitIv.setImageResource(R.drawable.portrait)
        binding.documentImageIv.setImageResource(R.drawable.id)
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
    }
}
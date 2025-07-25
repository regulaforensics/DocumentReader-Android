package com.regula.documentreader

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.regula.documentreader.Helpers.Companion.dismissDialog
import com.regula.documentreader.Helpers.Companion.getCheckDescription
import com.regula.documentreader.Helpers.Companion.showBottomValuesDialog
import com.regula.documentreader.Helpers.Companion.showDialog
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.enums.Scenario
import com.regula.documentreader.api.enums.eRPRM_Lights
import com.regula.documentreader.api.params.ImageInputData
import com.regula.documentreader.api.results.DocumentReaderResults
import com.regula.documentreader.databinding.ActivityMainBinding
import com.regula.documentreader.util.Utils

open class MainActivity : AppCompatActivity(), LivenessDialog.DialogListener,
    SetValueDialog.DialogListener {
    private lateinit var livenessDialog: LivenessDialog

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(
            layoutInflater
        )
    }
    private val sharedPreferences: SharedPreferences by lazy {
        getSharedPreferences(
            MY_SHARED_PREFS,
            MODE_PRIVATE
        )
    }
    private val viewModel: MainViewModel by lazy {
        ViewModelProvider(
            this,
            MainViewModelFactory(DocumentReader.Instance(), sharedPreferences)
        ).get(MainViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val view = binding.root
        setContentView(view)

        binding.ivDocumentImage.setImageResource(R.drawable.doc_auth)

        viewModel.initLiveData.observe(this) {
            dismissDialog()
            it?.let {
                disableUiElements()
                prepareWarningDialog("Init failed", it.message)
            } ?: successfulInit()
        }

        viewModel.showScannerSuccessCompletion.observe(this) { results ->
            dismissDialog()
            displayResults(results)
        }

        viewModel.showScannerErrorCompletion.observe(this) {
            dismissDialog()
            prepareWarningDialog("Error", "Error: $it")
        }

        viewModel.showScannerCancelCompletion.observe(this) {
            Toast.makeText(this, "Canceled by user", Toast.LENGTH_LONG).show()
        }

        prepareSimpleDialog("Initializing...", false)
        viewModel.init(this)

        binding.btnResetAll.setOnClickListener {
            viewModel.initAuth()
            resetViewAndParams()
        }
    }

    private fun resetViewAndParams(){
        binding.tvImagePatternsValue.setText(R.string.base_condition)
        binding.tvPhotoEmbeddingValue.setText(R.string.base_condition)
        binding.tvBarcodeFormatValue.setText(R.string.base_condition)
        binding.tvPortraitComparisonValue.setText(R.string.base_condition)
        binding.tvPortraitComparisonValue.visibility = View.GONE
        binding.rBtnUseRfid.isChecked = false
        binding.tvUVLuminescenceValue.setText(R.string.base_condition)
        binding.tvUVFibersValue.setText(R.string.base_condition)
        binding.tvExtendedMRZValue.setText(R.string.base_condition)
        binding.tvExtendedOCRValue.setText(R.string.base_condition)
        binding.tvIRB900Value.setText(R.string.base_condition)
        binding.tvIRVisibilityValue.setText(R.string.base_condition)
        binding.tvIPIValue.setText(R.string.base_condition)
        binding.tvSecurityTextValue.setText(R.string.base_condition)
    }

    private fun disableUiElements() {
        binding.btnStartDocProcessing.isClickable = false
        binding.rBtnShowScanner.isClickable = false
        binding.rBtnRecognizeImageWithLightType.isClickable = false
        binding.rBtnRecognizeImage.isClickable = false

        binding.rBtnShowScanner.isChecked = false

        binding.btnStartDocProcessing.setTextColor(Color.GRAY)
        binding.btnStartDocProcessing.setBackgroundColor(Color.LTGRAY)
        binding.rBtnShowScanner.setTextColor(Color.GRAY)
        binding.rBtnRecognizeImageWithLightType.setTextColor(Color.GRAY)
        binding.rBtnRecognizeImage.setTextColor(Color.GRAY)
    }

    private fun successfulInit() {
        //getting current processing scenario and loading available scenarios to ListView
        if (viewModel.getAvailableScenarios().isNotEmpty()){
            prepareAdditionalParams()
            prepareAuthenticityParams()
            prepareParamsDescriptions()
            viewModel.initAuth()
            initView()
            return
        }

        disableUiElements()
        prepareWarningDialog(
            "Available scenarios list is empty",
            resources.getString(R.string.scenarios_warning)
        )
    }

    private fun createImageBrowsingRequest() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        browsePictureResultLauncher.launch(intent)
    }

    private var browsePictureResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Intent? = result.data

                val selectedImage = data?.data
                if (selectedImage == null) {
                    Toast.makeText(this, "Something wrong with selected image", Toast.LENGTH_LONG).show()
                    return@registerForActivityResult
                }

                val bmp: Bitmap? = Utils.getBitmap(contentResolver, selectedImage, 1920, 1080)
                bmp?.let {
                    prepareSimpleDialog("Processing image", false)
                    viewModel.recognize(context = this@MainActivity, it)
                }
            }
        }

    private fun initView() {
        binding.btnStartDocProcessing.setOnClickListener {
            viewModel.updateRfidReading(binding.rBtnUseRfid.isChecked)

            val selectedId = binding.radioGroup.checkedRadioButtonId
            when (selectedId) {
                R.id.rBtnShowScanner -> viewModel.showScanner(this@MainActivity)
                R.id.rBtnRecognizeImageWithLightType -> startRecognizeImageWithLight()
                R.id.rBtnRecognizeImage -> createImageBrowsingRequest()
                else -> prepareSimpleDialog(
                    "Scan mode not selected",
                    true,
                    resources.getString(R.string.scan_mode_warning)
                )
            }
        }
    }

    private fun prepareAdditionalParams() {
        binding.switchMultipageActivate.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setupMultipage(isChecked)
        }
        binding.switchAlreadyCroppedActivate.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setupAlreadyCropped(isChecked)
        }
    }

    override fun onLivenessCheckValue(checkNameId: Int, checkValue: Boolean?) {
        when (checkNameId) {
            R.string.use_liveness -> {
                viewModel.setupAuthenticityUseLiveness(checkValue)
            }

            R.string.check_holo -> {
                viewModel.setupAuthenticityLivenessCheckHolo(checkValue)
            }

            R.string.check_ed -> {
                viewModel.setupAuthenticityLivenessCheckEd(checkValue)
            }

            R.string.check_ovi -> {
                viewModel.setupAuthenticityLivenessCheckOvi(checkValue)
            }

            R.string.check_mli -> {
                viewModel.setupAuthenticityLivenessCheckMli(checkValue)
            }

            R.string.check_dynaprint -> {
                viewModel.setupAuthenticityLivenessCheckDynaprint(checkValue)
            }

            R.string.check_black_and_white_copy -> {
                viewModel.setupAuthenticityLivenessCheckBlackAndWhiteCopy(checkValue)
            }

            R.string.check_geometry -> {
                viewModel.setupAuthenticityLivenessCheckGeometry(checkValue)
            }
        }
    }

    override fun onValueSubmit(text: String, value: Boolean?, checkId: Int) {
        when (checkId) {
            R.string.use_liveness -> {
                viewModel.setupAuthenticityUseLiveness(value)
                livenessDialog.updateLivenessState(text)
                livenessDialog.setLivenessParamsAviability(viewModel.getUselivenessCheckState())
            }

            R.string.check_holo -> {
                viewModel.setupAuthenticityLivenessCheckHolo(value)
                livenessDialog.updateHoloState(text)
            }

            R.string.check_ed -> {
                viewModel.setupAuthenticityLivenessCheckEd(value)
                livenessDialog.updateEdState(text)
            }

            R.string.check_mli -> {
                viewModel.setupAuthenticityLivenessCheckMli(value)
                livenessDialog.updateMliState(text)
            }

            R.string.check_ovi -> {
                viewModel.setupAuthenticityLivenessCheckOvi(value)
                livenessDialog.updateOviState(text)
            }

            R.string.check_dynaprint -> {
                viewModel.setupAuthenticityLivenessCheckDynaprint(value)
                livenessDialog.updateDynaprintState(text)
            }

            R.string.check_black_and_white_copy -> {
                viewModel.setupAuthenticityLivenessCheckBlackAndWhiteCopy(value)
                livenessDialog.updateBlackAndWhiteCopyState(text)
            }

            R.string.check_geometry -> {
                viewModel.setupAuthenticityLivenessCheckGeometry(value)
                livenessDialog.updateCheckGeometryState(text)
            }


            R.string.check_image_patterns -> {
                viewModel.setupAuthenticityCheckImagePatterns(value)
                binding.tvImagePatternsValue.text = text
            }

            R.string.check_photo_embedding -> {
                viewModel.setupAuthenticityCheckPhotoEmbedding(value)
                binding.tvPhotoEmbeddingValue.text = text
            }

            R.string.check_barcode_format -> {
                viewModel.setupAuthenticityCheckBarcodeFormat(value)
                binding.tvBarcodeFormatValue.text = text
            }

            R.string.check_photo_comparison -> {
                viewModel.setupAuthenticityCheckPhotoComparison(value)
                binding.tvPortraitComparisonValue.text = text
            }

            R.string.check_uv_luminiscence -> {
                viewModel.setupAuthenticityCheckUVLuminiscence(value)
                binding.tvUVLuminescenceValue.text = text
            }

            R.string.check_fibers -> {
                viewModel.setupAuthenticityCheckFibers(value)
                binding.tvUVFibersValue.text = text
            }

            R.string.check_ext_mrz -> {
                viewModel.setupAuthenticityCheckExtMRZ(value)
                binding.tvExtendedMRZValue.text = text
            }

            R.string.check_ext_ocr -> {
                viewModel.setupAuthenticityCheckExtOCR(value)
                binding.tvExtendedOCRValue.text = text
            }

            R.string.check_ir_b_900 -> {
                viewModel.setupAuthenticityCheckIRB900(value)
                binding.tvIRB900Value.text = text
            }

            R.string.check_ir_visibility -> {
                viewModel.setupAuthenticityCheckIRVisibility(value)
                binding.tvIRVisibilityValue.text = text
            }

            R.string.check_ipi -> {
                viewModel.setupAuthenticityCheckIPI(value)
                binding.tvIPIValue.text = text
            }

            R.string.check_security_text -> {
                viewModel.setupAuthenticityCheckSecurityText(value)
                binding.tvSecurityTextValue.text = text
            }
        }
    }

    private fun startRecognizeImageWithLight() {
        val image1 = BitmapFactory.decodeResource(resources, R.drawable.light_type_white)
        val image2 = BitmapFactory.decodeResource(resources, R.drawable.light_type_uv)
        val image3 = BitmapFactory.decodeResource(resources, R.drawable.light_type_ir)
        val imageData1 = ImageInputData(image1, eRPRM_Lights.RPRM_LIGHT_WHITE_FULL)
        val imageData2 = ImageInputData(image2, eRPRM_Lights.RPRM_LIGHT_UV)
        val imageData3 = ImageInputData(image3, eRPRM_Lights.RPRM_Light_IR_Full)

        val dialogView = layoutInflater.inflate(R.layout.dialog_progressbar, null)
        viewModel.recognizeSerialImages(imageData1, imageData2, imageData3, context = this, dialogView = dialogView)
    }

    private fun prepareAuthenticityParams() {
        val scenarios = ArrayList<String>()
        for (scenario in DocumentReader.Instance().availableScenarios) {
            scenarios.add(scenario.name)
        }

        if (scenarios.isNotEmpty()) {
            //setting default scenario
            if (!scenarios.contains(Scenario.SCENARIO_FULL_AUTH)) {
                prepareWarningDialog(
                    "FullAuth not available",
                    resources.getString(R.string.scenario_warning)
                )
                return
            }

            binding.rBtnUseRfid.setOnCheckedChangeListener { compoundButton, value ->
                if (value){
                    binding.overlayPortraitComparison.visibility = View.INVISIBLE
                    binding.tvPortraitComparisonValue.visibility = View.VISIBLE
                } else {
                    binding.overlayPortraitComparison.visibility = View.VISIBLE
                    binding.tvPortraitComparisonValue.visibility = View.INVISIBLE
                }
            }

            binding.ivLiveness.setOnClickListener {
                livenessDialog = LivenessDialog(this, viewModel.getActualLivenessParams(), viewModel.getUselivenessCheckState())
                livenessDialog.show()
            }

            binding.tvBarcodeFormatValue.setOnClickListener {
                showBottomValuesDialog(
                    binding.tvBarcodeFormatName.text.toString(),
                    binding.tvBarcodeFormatValue.text.toString(),
                    R.string.check_barcode_format,
                    this
                )
            }

            binding.tvIPIValue.setOnClickListener {
                showBottomValuesDialog(
                    binding.tvIPIName.text.toString(),
                    binding.tvIPIValue.text.toString(),
                    R.string.check_ipi,
                    this
                )
            }

            binding.tvSecurityTextValue.setOnClickListener {
                showBottomValuesDialog(
                    binding.tvSecurityTextName.text.toString(),
                    binding.tvSecurityTextValue.text.toString(),
                    R.string.check_security_text,
                    this
                )
            }

            binding.tvIRB900Value.setOnClickListener {
                showBottomValuesDialog(
                    binding.tvIRB900Name.text.toString(),
                    binding.tvIRB900Value.text.toString(),
                    R.string.check_ir_b_900,
                    this
                )
            }

            binding.tvIRVisibilityValue.setOnClickListener {
                showBottomValuesDialog(
                    binding.tvIRVisibilityName.text.toString(),
                    binding.tvIRVisibilityValue.text.toString(),
                    R.string.check_ir_visibility,
                    this
                )
            }

            binding.tvImagePatternsValue.setOnClickListener {
                showBottomValuesDialog(
                    binding.tvImagePatternsName.text.toString(),
                    binding.tvImagePatternsValue.text.toString(),
                    R.string.check_image_patterns,
                    this
                )
            }

            binding.tvExtendedMRZValue.setOnClickListener {
                showBottomValuesDialog(
                    binding.tvExtendedMRZName.text.toString(),
                    binding.tvExtendedMRZValue.text.toString(),
                    R.string.check_ext_mrz,
                    this
                )
            }

            binding.tvExtendedOCRValue.setOnClickListener {
                showBottomValuesDialog(
                    binding.tvExtendedOCRName.text.toString(),
                    binding.tvExtendedOCRValue.text.toString(),
                    R.string.check_ext_ocr,
                    this
                )
            }

            binding.tvPhotoEmbeddingValue.setOnClickListener {
                showBottomValuesDialog(
                    binding.tvPhotoEmbeddingName.text.toString(),
                    binding.tvPhotoEmbeddingValue.text.toString(),
                    R.string.check_photo_embedding,
                    this
                )
            }

            binding.tvPortraitComparisonValue.setOnClickListener {
                showBottomValuesDialog(
                    binding.tvPortraitComparisonName.text.toString(),
                    binding.tvPortraitComparisonValue.text.toString(),
                    R.string.check_photo_comparison,
                    this
                )
            }

            binding.tvUVFibersValue.setOnClickListener {
                showBottomValuesDialog(
                    binding.tvUVFibersName.text.toString(),
                    binding.tvUVFibersValue.text.toString(),
                    R.string.check_fibers,
                    this
                )
            }

            binding.tvUVLuminescenceValue.setOnClickListener {
                showBottomValuesDialog(
                    binding.tvUVLuminescenceName.text.toString(),
                    binding.tvUVLuminescenceValue.text.toString(),
                    R.string.check_uv_luminiscence,
                    this
                )
            }
        }
    }

    private fun prepareParamsDescriptions() {
        var checkName: String

        binding.ivAuthenticityInfo.setOnClickListener {
            checkName = resources.getString(R.string.authenticity_checks)
            prepareDescriptionDialog(checkName)
        }

        binding.ivLivenessInfo.setOnClickListener {
            checkName = resources.getString(R.string.liveness_params)
            prepareDescriptionDialog(checkName)
        }
        binding.ivBarcodeFormatInfo.setOnClickListener {
            checkName = resources.getString(R.string.check_barcode_format)
            prepareDescriptionDialog(checkName, R.drawable.auth_doc_barcode_format)
        }
        binding.ivIPIInfo.setOnClickListener {
            checkName = resources.getString(R.string.check_ipi)
            prepareDescriptionDialog(checkName, R.drawable.auth_doc_ipi)
        }
        binding.ivSecurityTextInfo.setOnClickListener {
            checkName = resources.getString(R.string.check_security_text)
            prepareDescriptionDialog(checkName, R.drawable.auth_doc_security_text)
        }
        binding.ivIRB900Info.setOnClickListener {
            checkName = "checkIRB900"
            prepareDescriptionDialog(checkName, R.drawable.auth_doc_ir_b900)
        }
        binding.ivIRVisibilityInfo.setOnClickListener {
            checkName = resources.getString(R.string.check_ir_visibility)
            prepareDescriptionDialog(checkName, R.drawable.auth_doc_ir_visibility)
        }
        binding.ivImagePatternsInfo.setOnClickListener {
            checkName = resources.getString(R.string.check_image_patterns)
            prepareDescriptionDialog(checkName, R.drawable.auth_doc_image_patterns)
        }
        binding.ivExtendedMRZInfo.setOnClickListener {
            checkName = resources.getString(R.string.check_ext_mrz)
            prepareDescriptionDialog(checkName, R.drawable.auth_doc_extended_mrz)
        }
        binding.ivExtendedOCRInfo.setOnClickListener {
            checkName = resources.getString(R.string.check_ext_ocr)
            prepareDescriptionDialog(checkName, R.drawable.auth_doc_extended_ocr)
        }
        binding.ivPhotoEmbeddingInfo.setOnClickListener {
            checkName = resources.getString(R.string.check_photo_embedding)
            prepareDescriptionDialog(checkName, R.drawable.auth_doc_photo_embedding)
        }
        binding.ivPortraitComparisonInfo.setOnClickListener {
            checkName = resources.getString(R.string.check_photo_comparison)
            prepareDescriptionDialog(checkName, R.drawable.auth_doc_portrait_comparison)
        }
        binding.ivUVFibersInfo.setOnClickListener {
            checkName = resources.getString(R.string.check_fibers)
            prepareDescriptionDialog(checkName, R.drawable.auth_doc_uv_fibers)
        }
        binding.ivUVLuminescenceInfo.setOnClickListener {
            checkName = resources.getString(R.string.check_uv_luminiscence)
            prepareDescriptionDialog(checkName, R.drawable.auth_doc_dull_paper)
        }
    }

    private fun prepareDescriptionDialog(title: String, imageResId: Int? = null) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_description, null)

        val imageView = dialogView.findViewById<ImageView>(R.id.ivCheck)
        if (imageResId != null) {
            imageView.setImageResource(imageResId)
        } else {
            imageView.visibility = View.GONE
        }
        dialogView.findViewById<TextView>(R.id.tvDescriptionMsg).text =
            getCheckDescription(title, this)

        showDialog(this, title, true, dialogView)
    }

    private fun prepareWarningDialog(title: String, errorMsg: String?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_warning, null)

        dialogView.findViewById<TextView>(R.id.tvWarningMsg).text = errorMsg

        showDialog(this, title, false, dialogView)
    }

    private fun prepareSimpleDialog(
        title: String,
        cancelable: Boolean,
        msg: String? = null
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_progressbar, null)

        if (msg != null) {
            dialogView.findViewById<TextView>(R.id.tvInfo).text = msg
            dialogView.findViewById<TextView>(R.id.tvInfo).visibility = View.VISIBLE
        }

        showDialog(this, title, cancelable, dialogView)
    }

    private fun displayResults(results: DocumentReaderResults?) {
        if (results != null) {
            ResultsAuthActivity.results = results
            val intent = Intent(this, ResultsAuthActivity::class.java)
            startActivity(intent)
        }
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
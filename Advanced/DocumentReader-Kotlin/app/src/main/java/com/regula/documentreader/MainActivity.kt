package com.regula.documentreader

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.regula.documentreader.Helpers.Companion.colorString
import com.regula.documentreader.Helpers.Companion.drawable
import com.regula.documentreader.Helpers.Companion.getBitmap
import com.regula.documentreader.Scan.Companion.ACTION_TYPE_CUSTOM
import com.regula.documentreader.Scan.Companion.ACTION_TYPE_GALLERY
import com.regula.documentreader.Scan.Companion.ACTION_TYPE_MANUAL_MULTIPAGE_MODE
import com.regula.documentreader.SettingsActivity.Companion.isDataEncryptionEnabled
import com.regula.documentreader.SettingsActivity.Companion.isRfidEnabled
import com.regula.documentreader.api.DocumentReader.Instance
import com.regula.documentreader.api.completions.IDocumentReaderCompletion
import com.regula.documentreader.api.completions.rfid.IRfidReaderCompletion
import com.regula.documentreader.api.config.RecognizeConfig
import com.regula.documentreader.api.config.ScannerConfig
import com.regula.documentreader.api.enums.DocReaderAction
import com.regula.documentreader.api.enums.FrameShapeType
import com.regula.documentreader.api.enums.Scenario
import com.regula.documentreader.api.enums.Scenario.Scenarios
import com.regula.documentreader.api.enums.eRFID_DataFile_Type
import com.regula.documentreader.api.enums.eRFID_NotificationCodes
import com.regula.documentreader.api.errors.DocReaderRfidException
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.internal.parser.DocReaderResultsJsonParser
import com.regula.documentreader.api.params.DocReaderConfig
import com.regula.documentreader.api.params.FaceApiParams
import com.regula.documentreader.api.results.DocumentReaderNotification
import com.regula.documentreader.api.results.DocumentReaderResults
import com.regula.documentreader.databinding.ActivityMainBinding
import com.regula.documentreader.util.LicenseUtil
import org.json.JSONException
import org.json.JSONObject
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : FragmentActivity(), Serializable {
    private var mTimerAnimator: ValueAnimator? = null
    private var isAnimationStarted: Boolean = false
    @Scenarios
    private var currentScenario: String = Scenario.SCENARIO_MRZ

    @Transient
    private lateinit var binding: ActivityMainBinding

    @Transient
    private var loadingDialog: AlertDialog? = null

    @Transient
    private var initDialog: AlertDialog? = null

    @Transient
    val imageBrowsingIntentLauncher =
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
                        loadingDialog = showDialog("Processing image")
                        if (imageUris.size == 1) {
                            getBitmap(imageUris[0], 1920, 1080, this)?.let { bitmap ->
                                val recognizeConfig = RecognizeConfig.Builder(currentScenario).setBitmap(bitmap).build()
                                Instance().recognize(recognizeConfig, completion)
                            }
                        } else {
                            val bitmaps = arrayOfNulls<Bitmap>(imageUris.size)
                            for (i in bitmaps.indices) {
                                bitmaps[i] = getBitmap(imageUris[i], 1920, 1080, this)
                            }
                            val recognizeConfig = RecognizeConfig.Builder(currentScenario).setBitmaps(bitmaps).build()
                            Instance().recognize(recognizeConfig, completion)
                        }
                    }
                }
            }
        }

    @Transient
    val customRfidIntentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            results?.let { displayResults(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (LicenseUtil.readFileFromAssets("Regula", "regula.license", this) == null
            && !isInitializedByBleDevice) showDialog(
            this@MainActivity
        )
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Helpers.opaqueStatusBar(binding.root)
        if (Instance().isReady)
            onInitComplete()

        binding.helpBtn.setOnClickListener(OnClickListenerSerializable {
            BottomSheet.newInstance(
                "Information",
                arrayListOf(
                    BSItem("Documents"),
                    BSItem("Core"),
                    BSItem("Scenarios")
                ),
                true,
            ) { Helpers.openLink(this, it.title) }.show(supportFragmentManager, "")
        })
        binding.settingsBtn.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = CommonRecyclerAdapter(getRvData())
        binding.recyclerView.addItemDecoration(DividerItemDecoration(this, 1))

        val license = LicenseUtil.readFileFromAssets(
            "Regula",
            "regula.license",
            this
        )

        license?.let {
            initDialog = showDialog("Initializing")
            initializeReader(it)
        }
    }

    private fun initializeReader(license: ByteArray) {
        Instance().initializeReader(this@MainActivity, DocReaderConfig(license))
        { success, error_initializeReader ->
            if (initDialog?.isShowing == true)
                initDialog?.dismiss()
            Instance().customization().edit().setShowHelpAnimation(false).apply()

            if (success) onInitComplete()
            else
                Toast.makeText(
                    this@MainActivity,
                    "Init failed:${error_initializeReader?.message}",
                    Toast.LENGTH_LONG
                ).show()
        }
    }

    private fun onInitComplete() {
        val scenarios: Array<String?> = arrayOfNulls(Instance().availableScenarios.size)
        for ((i, scenario) in Instance().availableScenarios.withIndex())
            scenarios[i] = scenario.name
        if (scenarios.isNotEmpty()) {
            currentScenario = scenarios[0] ?: ""
            binding.scenarioPicker.visibility = View.VISIBLE
            binding.scenarioPicker.maxValue = scenarios.size - 1
            binding.scenarioPicker.wrapSelectorWheel = false
            binding.scenarioPicker.displayedValues = scenarios
            binding.scenarioPicker.value = scenarios.indexOf(currentScenario)
            binding.scenarioPicker.setOnValueChangedListener { _, _, newVal ->
                binding.scenarioPicker.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                currentScenario = scenarios[newVal] ?: ""
            }
            binding.recyclerView.visibility = View.VISIBLE
        } else {
            Toast.makeText(
                this@MainActivity,
                "Available scenarios list is empty",
                Toast.LENGTH_SHORT
            ).show()
        }

        Instance().setLocalizationCallback { stringId ->
            if(stringId == "strLookingDocument")
                return@setLocalizationCallback SettingsActivity.customString
            return@setLocalizationCallback null
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

    @Transient
    private val completion = IDocumentReaderCompletion { action, results, error ->
        if(!isAnimationStarted) {
            mTimerAnimator?.let {
                it.start()
                isAnimationStarted = true
            }
        }
        if (action == DocReaderAction.COMPLETE
            || action == DocReaderAction.TIMEOUT) {
            hideDialog()
            cancelAnimation()
            if (Instance().functionality().isManualMultipageMode) {
                if (results?.morePagesAvailable != 0) {
                    Instance().startNewPage()
                    Handler(Looper.getMainLooper()).postDelayed({
                        showScanner()
                    }, 100)
                    return@IDocumentReaderCompletion
                } else {
                    Instance().functionality().edit().setManualMultipageMode(false).apply()
                }
            }
            if (isRfidEnabled && results?.chipPage != 0 && Instance().isRFIDAvailableForUse) {
                Instance().startRFIDReader(this, object: IRfidReaderCompletion() {
                    override fun onChipDetected() {
                        Log.d("Rfid", "Chip detected")
                    }

                    override fun onProgress(notification: DocumentReaderNotification) {
                        rfidProgress(notification.code, notification.value)
                    }

                    override fun onRetryReadChip(exception: DocReaderRfidException) {
                        Log.d("Rfid", "Retry with error: " + exception.errorCode)
                    }

                    override fun onCompleted(
                        rfidAction: Int,
                        results_RFIDReader: DocumentReaderResults?,
                        error: DocumentReaderException?
                    ) {
                        if (rfidAction == DocReaderAction.COMPLETE
                            || rfidAction == DocReaderAction.ERROR
                            || rfidAction == DocReaderAction.TIMEOUT
                            || rfidAction == DocReaderAction.CANCEL)
                            displayResults(results_RFIDReader!!)
                    }
                })
            } else
                displayResults(results!!)
        } else
            if (action == DocReaderAction.CANCEL) {
                if (Instance().functionality().isManualMultipageMode)
                    Instance().functionality().edit().setManualMultipageMode(false).apply()

                Toast.makeText(this, "Scanning was cancelled", Toast.LENGTH_LONG).show()
                hideDialog()
                cancelAnimation()
            }
            else if (action == DocReaderAction.ERROR) {
                Toast.makeText(this, "Error:$error", Toast.LENGTH_LONG).show()
                hideDialog()
                cancelAnimation()
            }
    }

    private fun displayResults(documentReaderResults: DocumentReaderResults) {
        ResultsActivity.results = documentReaderResults
        if (isDataEncryptionEnabled) {
            val input = JSONObject(ResultsActivity.results.rawResult)
            val processParam = JSONObject()
                .put("alreadyCropped", true)
                .put("scenario", "FullProcess")
            val output = JSONObject()
                .put("List", input.getJSONObject("ContainerList").getJSONArray("List"))
                .put("processParam", processParam)
            postRequest(output.toString())
        } else
            startActivity(Intent(this, ResultsActivity::class.java))
    }

    private fun postRequest(jsonInputString: String) {
        loadingDialog = showDialog("Getting results from server")
        ENCRYPTED_RESULT_SERVICE
            .httpPost()
            .header(mapOf("Content-Type" to "application/json; utf-8"))
            .body(jsonInputString)
            .responseString { _, _, result ->
                hideDialog()
                when (result) {
                    is Result.Success -> {
                        val map = result.component1()
                            ?.let { DocReaderResultsJsonParser.parseCoreResults(it) }
                        val results = map?.get("docReaderResults") as DocumentReaderResults
                        ResultsActivity.results = results
                        startActivity(Intent(this, ResultsActivity::class.java))
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
                                .setNegativeButton("Cancel", null)
                                .show()
                        }
                    }
                }
            }
    }

    fun showScanner() {
        val scannerConfig = ScannerConfig.Builder(currentScenario).build()
        Instance().startScanner(this@MainActivity, scannerConfig, completion)
    }

    fun createImageBrowsingRequest() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        imageBrowsingIntentLauncher.launch(intent)
    }

    private fun getRvData(): List<Base> {
        val rvData = mutableListOf<Base>()
        rvData.add(Section("Default"))
        rvData.add(Scan("Default (startScanner)", resetFunctionality = false))
        rvData.add(Scan("Gallery (recognizeImage)", ACTION_TYPE_GALLERY))

        rvData.add(Section("Custom"))
        rvData.add(Scan("Manual multipage mode", ACTION_TYPE_MANUAL_MULTIPAGE_MODE) {
            Instance().functionality().edit().setManualMultipageMode(true).apply()
        })
        rvData.add(Section("Custom camera frame"))
        rvData.add(Scan("Custom border width") {
            Instance().customization().edit().setCameraFrameBorderWidth(10).apply()
        })
        rvData.add(Scan("Custom border color") {
            Instance().customization().edit()
                .setCameraFrameDefaultColor(colorString(Color.RED)).apply()
            Instance().customization().edit()
                .setCameraFrameActiveColor(colorString(Color.MAGENTA)).apply()
        })
        rvData.add(Scan("Custom shape") {
            Instance().customization().edit().setCameraFrameShapeType(FrameShapeType.CORNER).apply()
            Instance().customization().edit().setCameraFrameLineLength(40).apply()
            Instance().customization().edit().setCameraFrameCornerRadius(10f).apply()
            Instance().customization().edit().setCameraFrameLineCap(Paint.Cap.ROUND).apply()
        })
        rvData.add(Scan("Custom offset") {
            Instance().customization().edit().setCameraFrameOffsetWidth(50).apply()
        })
        rvData.add(Scan("Custom aspect ratio") {
            Instance().customization().edit().setCameraFramePortraitAspectRatio(1f).apply()
            Instance().customization().edit().setCameraFrameLandscapeAspectRatio(1f).apply()
        })
        rvData.add(Scan("Custom position") {
            Instance().customization().edit().setCameraFrameVerticalPositionMultiplier(0.5f).apply()
        })
        rvData.add(Section("Custom toolbar"))
        rvData.add(Scan("Custom torch button") {
            Instance().functionality().edit().setShowTorchButton(true).apply()
            Instance().customization().edit().setTorchImageOn(drawable(R.drawable.light_on, this))
                .apply()
            Instance().customization().edit().setTorchImageOff(drawable(R.drawable.light_off, this))
                .apply()
        })
        rvData.add(Scan("Custom camera switch button") {
            Instance().functionality().edit().setShowCameraSwitchButton(true).apply()
            Instance().customization().edit()
                .setCameraSwitchButtonImage(drawable(R.drawable.camera, this)).apply()
        })
        rvData.add(Scan("Custom capture button") {
            Instance().functionality().edit().setShowCaptureButton(true).apply()
            Instance().functionality().edit().setShowCaptureButtonDelayFromStart(0).apply()
            Instance().functionality().edit().setShowCaptureButtonDelayFromDetect(0).apply()
            Instance().customization().edit()
                .setCaptureButtonImage(drawable(R.drawable.capture, this)).apply()
        })
        rvData.add(Scan("Custom close button") {
            Instance().functionality().edit().setShowCloseButton(true).apply()
            Instance().customization().edit().setCloseButtonImage(drawable(R.drawable.close, this))
                .apply()
        })
        rvData.add(Scan("Custom size of the toolbar") {
            Instance().customization().edit().setToolbarSize(120f).apply()
            Instance().customization().edit().setTorchImageOn(drawable(R.drawable.light_on, this))
                .apply()
            Instance().customization().edit().setTorchImageOff(drawable(R.drawable.light_off, this))
                .apply()
            Instance().customization().edit()
                .setCloseButtonImage(drawable(R.drawable.big_close, this)).apply()
        })
        rvData.add(Section("Custom status messages"))
        rvData.add(Scan("Custom text") {
            Instance().customization().edit().setShowStatusMessages(true).apply()
            Instance().customization().edit().setStatus("Custom status").apply()
        })
        rvData.add(Scan("Custom text font") {
            Instance().customization().edit().setShowStatusMessages(true).apply()
            Instance().customization().edit().setStatusTextFont(Typeface.DEFAULT_BOLD).apply()
            Instance().customization().edit().setStatusTextSize(24).apply()
        })
        rvData.add(Scan("Custom text color") {
            Instance().customization().edit().setShowStatusMessages(true).apply()
            Instance().customization().edit().setStatusTextColor(colorString(Color.BLUE)).apply()
        })
        rvData.add(Scan("Custom position") {
            Instance().customization().edit().setShowStatusMessages(true).apply()
            Instance().customization().edit().setStatusPositionMultiplier(0.5f).apply()
        })
        rvData.add(Section("Free custom status"))
        rvData.add(Scan("Free text + position") {
            val status = SpannableString("Hello, world!")
            status.setSpan(AbsoluteSizeSpan(50), 0, status.length, 0)
            status.setSpan(ForegroundColorSpan(Color.RED), 0, status.length, 0)
            status.setSpan(StyleSpan(Typeface.ITALIC), 0, status.length, 0)
            Instance().customization().edit().setCustomLabelStatus(status).apply()
            Instance().customization().edit().setCustomStatusPositionMultiplier(0.5f).apply()
        })
        rvData.add(Scan("Custom Status & Images & Buttons") {
            Instance().customization().edit().setUiCustomizationLayer(getJsonFromAssets("layer.json")).apply()
        })
        rvData.add(Scan("Custom Buttons") {
            Instance().functionality().edit()
                .setShowCloseButton(false)
                .setShowTorchButton(false)
                .apply()

            Instance().customization().edit()
                .setUiCustomizationLayer(getJsonFromAssets("buttons.json"))
                .apply()
        })
        Instance().setOnClickListener {
            println("Button tag: " + it.tag)
        }
        rvData.add(Scan("Custom Status Animated") {
            initAnimation()
        })
        rvData.add(Section("Custom animations"))
        rvData.add(Scan("Help animation image") {
            Instance().customization().edit().setShowHelpAnimation(true).apply()
            Instance().customization().edit()
                .setHelpAnimationImage(drawable(R.drawable.credit_card, this)).apply()
        })
        rvData.add(Scan("Custom the next page animation") {
            Instance().customization().edit().setShowNextPageAnimation(true).apply()
            Instance().customization().edit()
                .setMultipageAnimationFrontImage(drawable(R.drawable.one, this)).apply()
            Instance().customization().edit()
                .setMultipageAnimationBackImage(drawable(R.drawable.two, this)).apply()
        })
        rvData.add(Scan("Custom Liveness animation") {
            // NOTE: for a runtime animation change take a look at `showScanner` completion handler.
            Instance().customization().edit()
                .setLivenessAnimationImage(getDrawable(com.regula.documentreader.api.R.drawable.reg_passport_single)).apply()
            Instance().customization().edit()
                .setLivenessAnimationPositionMultiplier(0.4f).apply()
            Instance().customization().edit()
                .setLivenessAnimationImageScaleType(ImageView.ScaleType.CENTER_INSIDE).apply()

            val matrix = Matrix()
            Instance().customization().edit()
                .setLivenessAnimationImageMatrix(matrix).apply()
        })
        rvData.add(Section("Custom tint color"))
        rvData.add(Scan("Activity indicator") {
            Instance().customization().edit().setActivityIndicatorColor(colorString(Color.RED))
                .apply()
        })
        rvData.add(Scan("Next page button") {
            val typeface = ResourcesCompat.getFont(this, R.font.my_font)
            Instance().functionality().edit().setShowSkipNextPageButton(true).apply()
            Instance().customization().edit()
                .setMultipageButtonBackgroundColor(colorString(Color.RED))
                .setMultipageButtonText("Skip page")
                .setMultipageButtonTextColor("#FFFFFF")
                .setMultipageButtonTextSize(20)
                .setMultipageButtonTextFont(typeface)
                .apply()
        })
        rvData.add(Scan("All visual elements") {
            Instance().customization().edit().setTintColor(colorString(Color.BLUE)).apply()
        })
        rvData.add(Section("Custom background"))
        rvData.add(Scan("No background mask") {
            Instance().customization().edit().setShowBackgroundMask(false).apply()
        })
        rvData.add(Scan("Custom alpha") {
            Instance().customization().edit().setBackgroundMaskAlpha(0.8f).apply()
        })
        rvData.add(Scan("Custom background image") {
            val matrix = Matrix()
            Instance().customization().edit()
                .setBorderBackgroundImage(drawable(R.drawable.viewfinder, this))
                //.setBorderBackgroundImageScaleType(ImageView.ScaleType.MATRIX)
                //.setBorderBackgroundImageMatrix(matrix)
                .apply()
        })

        rvData.add(Section("Addition"))
        rvData.add(Scan("Build-in Face SDK integration", ACTION_TYPE_CUSTOM) {
            setUseFaceApi()
        })
        // whitespace at the bottom of the list for better look and convenience
        rvData.add(Scan("", ACTION_TYPE_CUSTOM))
        return rvData
    }

    private fun setUseFaceApi() {
        val faceApiParams = FaceApiParams()
        faceApiParams.url = "url"
        faceApiParams.mode = "match"
        faceApiParams.threshold = 90
        faceApiParams.proxy = "proxy"
        faceApiParams.proxyUserPwd = "user_pwd"
        faceApiParams.proxyType = 1

        val search = FaceApiParams.Search()
        search.limit = 100
        search.threshold = 0.9f
        search.groupIds = intArrayOf(1, 2, 3)

        faceApiParams.search = search

        Instance().processParams().faceApiParams = faceApiParams
    }

    private fun showDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Error")
            .setMessage("license in assets is missed")
            .setPositiveButton(
                getString(com.regula.documentreader.api.R.string.strAccessibilityCloseButton)
            ) { dialog, which ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun getJsonFromAssets(name: String): JSONObject? {
        val inputStream = this@MainActivity.assets.open(name)
        val jsonString = Scanner(inputStream).useDelimiter("\\A").next()
        try {
            return JSONObject(jsonString)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return null
    }

    private fun cancelAnimation() {
        mTimerAnimator?.let {
            it.cancel()
            isAnimationStarted = false
            mTimerAnimator = null
        }
        Instance().customization().edit().setUiCustomizationLayer(null).apply()
    }

    private fun initAnimation() {
        isAnimationStarted = false
        val jsonObject: JSONObject? = getJsonFromAssets("layer_animation.json")
        updatePosition(jsonObject, 0.5f)
        Instance().customization().edit().setUiCustomizationLayer(jsonObject).apply()
        mTimerAnimator = ValueAnimator.ofFloat(0.5f, 1.5f)
        mTimerAnimator?.let {
            it.duration = 2500
            it.interpolator = AccelerateInterpolator()
            it.repeatMode = ValueAnimator.REVERSE
            it.repeatCount = 20
            it.addUpdateListener(AnimatorUpdateListener { animation ->
                updatePosition(jsonObject, animation.animatedValue as Float)
                Instance().customization().edit().setUiCustomizationLayer(jsonObject)
                    .applyImmediately(this@MainActivity)
            })
        }
    }

    private fun updatePosition(jsonObject: JSONObject?, position: Float) {
        jsonObject?.let {
            val text = "Custom label that showing current time:" + SimpleDateFormat("HH:mm:ss")
                .format(Date())
            (it.getJSONArray("objects")[0] as JSONObject).getJSONObject("label")
                .getJSONObject("position").put("v", position.toDouble())
            (it.getJSONArray("objects")[0] as JSONObject).getJSONObject("label")
                .put("text", text)
        }
    }

    companion object {
        var results: DocumentReaderResults? = null
        var isInitializedByBleDevice: Boolean = false
        const val ENCRYPTED_RESULT_SERVICE = "https://api.regulaforensics.com/api/process"
    }

    fun rfidProgress(code: Int, value: Int) {
        val hiword = code and -0x10000
        val loword = code and 0x0000FFFF
        when (hiword) {
            eRFID_NotificationCodes.RFID_NOTIFICATION_PCSC_READING_DATAGROUP -> if (value == 0) {
                Log.d("Rfid", "Current group: " + String.format(
                    getString(com.regula.documentreader.api.R.string.strReadingRFIDDG),
                    eRFID_DataFile_Type.getTranslation(
                        applicationContext, loword
                    )))
            }
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
}

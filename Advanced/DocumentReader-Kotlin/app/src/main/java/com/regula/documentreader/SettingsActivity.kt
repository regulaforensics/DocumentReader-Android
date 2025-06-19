package com.regula.documentreader

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.regula.documentreader.Helpers.Companion.captureModeIntToString
import com.regula.documentreader.Helpers.Companion.captureModeStringToInt
import com.regula.documentreader.Helpers.Companion.listToString
import com.regula.documentreader.Helpers.Companion.measureSystemIntToString
import com.regula.documentreader.Helpers.Companion.measureSystemStringToInt
import com.regula.documentreader.Helpers.Companion.replaceFragment
import com.regula.documentreader.Helpers.Companion.toIntArray
import com.regula.documentreader.Helpers.Companion.toMutableListString
import com.regula.documentreader.Scan.Companion.ACTION_TYPE_CUSTOM
import com.regula.documentreader.SettingsActivity.Companion.customString
import com.regula.documentreader.SettingsActivity.Companion.isDataEncryptionEnabled
import com.regula.documentreader.api.DocumentReader.Instance
import com.regula.documentreader.api.enums.BarcodeType
import com.regula.documentreader.api.enums.DocReaderFrame.DOCUMENT
import com.regula.documentreader.api.enums.DocReaderFrame.MAX
import com.regula.documentreader.api.enums.DocReaderFrame.NONE
import com.regula.documentreader.api.enums.DocReaderFrame.SCENARIO_DEFAULT
import com.regula.documentreader.api.enums.MRZFormat.FORMAT_1X30
import com.regula.documentreader.api.enums.MRZFormat.FORMAT_1X6
import com.regula.documentreader.api.enums.MRZFormat.FORMAT_2X30
import com.regula.documentreader.api.enums.MRZFormat.FORMAT_2X36
import com.regula.documentreader.api.enums.MRZFormat.FORMAT_2X44
import com.regula.documentreader.api.enums.MRZFormat.FORMAT_3X30
import com.regula.documentreader.api.enums.diDocType.dtDiplomaticPassport
import com.regula.documentreader.api.enums.diDocType.dtDrivingLicense
import com.regula.documentreader.api.enums.diDocType.dtIdentityCard
import com.regula.documentreader.api.enums.diDocType.dtPassport
import com.regula.documentreader.api.enums.diDocType.dtRegistrationCertificate
import com.regula.documentreader.api.enums.diDocType.dtServicePassport
import com.regula.documentreader.api.enums.diDocType.dtTravelDocument
import com.regula.documentreader.api.enums.diDocType.dtVisaID2
import com.regula.documentreader.api.enums.diDocType.dtVisaID3
import com.regula.documentreader.api.enums.eImageQualityCheckType
import com.regula.documentreader.api.params.Functionality
import com.regula.documentreader.api.params.ImageQA
import com.regula.documentreader.databinding.ActivitySettingsBinding
import com.regula.documentreader.databinding.FragmentRvBinding
import org.json.JSONObject

class SettingsActivity : FragmentActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private var applicationSettingsFragment = ApplicationSettingsFragment()
    private var apiSettingsFragment = APISettingsFragment()
    private var selectedTabIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedTabIndex = savedInstanceState?.getInt("selectedTabIndex") ?: 0
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Helpers.opaqueStatusBar(binding.root)
        binding.backBtn.setOnClickListener { finish() }
        replaceFragment(applicationSettingsFragment, this, R.id.settings)
        binding.resetBtn.setOnClickListener { resetDialog() }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab!!.position) {
                    0 -> {
                        selectedTabIndex = 0
                        replaceFragment(
                            applicationSettingsFragment,
                            this@SettingsActivity,
                            R.id.settings
                        )
                    }
                    1 -> {
                        selectedTabIndex = 1
                        replaceFragment(
                            apiSettingsFragment,
                            this@SettingsActivity,
                            R.id.settings
                        )
                    }
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {}
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
        })
        binding.tabLayout.getTabAt(selectedTabIndex)!!.select()
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putInt("selectedTabIndex", selectedTabIndex)
    }

    private fun resetDialog() = MaterialAlertDialogBuilder(this, R.style.ConfirmAlertDialogTheme)
        .setTitle(getString(R.string.strResetAllSettings))
        .setMessage(
            getString(
                if (selectedTabIndex == 0)
                    R.string.strResetAllAppSettingsConfirmation
                else
                    R.string.strResetAllAPISettingsConfirmation
            )
        )
        .setPositiveButton(getString(R.string.reset).uppercase()) { _, _ -> reset() }
        .setNegativeButton(getString(R.string.strCancel).uppercase(), null)
        .show()

    private fun reset() {
        if (selectedTabIndex == 0) {
            isRfidEnabled = false
            isDataEncryptionEnabled = false
            customString = null
            
            val adapter =
                applicationSettingsFragment.binding.recyclerView.adapter as CommonRecyclerAdapter
            adapter.notifyItemRangeChanged(0, adapter.itemCount)
        } else {
            val scenario = Instance().processParams().scenario
            Instance().resetConfiguration()
            Instance().processParams().scenario = scenario
            val adapter =
                apiSettingsFragment.binding.recyclerView.adapter as CommonRecyclerAdapter
            adapter.notifyItemRangeChanged(0, adapter.itemCount)
        }
    }

    companion object {
        var isRfidEnabled = false
        var isDataEncryptionEnabled = false
        var customString:String? = null
    }
}

class ApplicationSettingsFragment : Fragment() {
    private var _binding: FragmentRvBinding? = null
    val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, vg: ViewGroup?, bundle: Bundle?): View {
        _binding = FragmentRvBinding.inflate(inflater, vg, false)
        val sectionsData = mutableListOf<Base>()
        sectionsData.add(Section("RFID"))
        sectionsData.add(
            Switch(
                "Read RFID",
                { SettingsActivity.isRfidEnabled },
                { SettingsActivity.isRfidEnabled = it })
        )
        sectionsData.add(Section("Security"))
        sectionsData.add(
            Switch(
                "Data encryption",
                { isDataEncryptionEnabled },
                { isDataEncryptionEnabled = it })
        )
        sectionsData.add(Section("Custom localization"))
        sectionsData.add (
            InputString(
                "Searching for document custom string",
                {
                    customString?:""
                },
                {
                    customString = if(it == "")  null else it
                })
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.adapter = CommonRecyclerAdapter(sectionsData)
        binding.recyclerView.addItemDecoration(DividerItemDecoration(activity, 1))

        return binding.root
    }
}

class APISettingsFragment : Fragment() {
    private var _binding: FragmentRvBinding? = null
    val binding get() = _binding!!

    val functionality = Instance().functionality()

    @SuppressLint("WrongConstant")
    override fun onCreateView(inflater: LayoutInflater, vg: ViewGroup?, bundle: Bundle?): View {
        _binding = FragmentRvBinding.inflate(inflater, vg, false)
        val sectionsData = mutableListOf<Base>()

        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        val adapter = CommonRecyclerAdapter(sectionsData)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addItemDecoration(DividerItemDecoration(activity, 1))

        sectionsData.add(Section("Buttons"))
        sectionsData.add(
            Switch(
                "Torch button",
                { functionality.isShowTorchButton },
                { functionality.edit().setShowTorchButton(it).apply() })
        )
        sectionsData.add(
            Switch(
                "Turn on torch on camera start",
                { functionality.isTorchTurnedOn },
                { functionality.edit().setTorchTurnedOn(it).apply() })
        )
        sectionsData.add(
            Switch(
                "Camera Switch button",
                { functionality.isShowCameraSwitchButton },
                { functionality.edit().setShowCameraSwitchButton(it).apply() })
        )
        val showCaptureButtonDelayFromStart = Stepper(
            "Capture button delay (from start)",
            "sec",
            { functionality.showCaptureButtonDelayFromStart.toInt() },
            { functionality.edit().setShowCaptureButtonDelayFromStart(it.toLong()).apply() },
            { functionality.isShowCaptureButton })
        val showCaptureButtonDelayFromDetect = Stepper(
            "Capture button delay (from detect)",
            "sec",
            { functionality.showCaptureButtonDelayFromDetect.toInt() },
            { functionality.edit().setShowCaptureButtonDelayFromDetect(it.toLong()).apply() },
            { functionality.isShowCaptureButton })
        sectionsData.add(
            Switch(
                "Capture button",
                { functionality.isShowCaptureButton },
                {
                    functionality.edit().setShowCaptureButton(it).apply()
                    adapter.notifyItemChanged(sectionsData.indexOf(showCaptureButtonDelayFromStart))
                    adapter.notifyItemChanged(sectionsData.indexOf(showCaptureButtonDelayFromDetect))
                })
        )
        sectionsData.add(
            showCaptureButtonDelayFromStart
        )
        sectionsData.add(
            showCaptureButtonDelayFromDetect
        )
        sectionsData.add(
            Switch(
                "Change Frame button",
                { functionality.isShowChangeFrameButton },
                { functionality.edit().setShowChangeFrameButton(it).apply() })
        )
        sectionsData.add(
            Switch(
                "Close button",
                { functionality.isShowCloseButton },
                { functionality.edit().setShowCloseButton(it).apply() })
        )
        sectionsData.add(Section("Document Processing"))
        sectionsData.add(
            Switch(
                "Multipage processing",
                { Instance().processParams().multipageProcessing },
                { Instance().processParams().multipageProcessing = it })
        )
        val generateDoublePageSpreadImage = Switch(
            "Generate double-page spread image",
            { Instance().processParams().generateDoublePageSpreadImage },
            { Instance().processParams().generateDoublePageSpreadImage = it },
            { Instance().processParams().doublePageSpread })
        sectionsData.add(
            Switch(
                "Double-page spread processing",
                { Instance().processParams().doublePageSpread },
                {
                    Instance().processParams().doublePageSpread = it
                    adapter.notifyItemChanged(sectionsData.indexOf(generateDoublePageSpreadImage))
                })
        )
        sectionsData.add(
            generateDoublePageSpreadImage
        )
        sectionsData.add(
            Switch(
                "Manual crop",
                { Instance().processParams().manualCrop },
                { Instance().processParams().manualCrop = it })
        )
        sectionsData.add(
            Switch(
                "Force read Mrz before Locate",
                { Instance().processParams().forceReadMrzBeforeLocate },
                { Instance().processParams().forceReadMrzBeforeLocate = it })
        )
        sectionsData.add(
            Switch(
                "Multiple documents from one image",
                { Instance().processParams().multiDocOnImage },
                { Instance().processParams().multiDocOnImage = it })
        )
        sectionsData.add(
            Switch(
                "No graphics",
                { Instance().processParams().noGraphics },
                { Instance().processParams().noGraphics = it })
        )
        sectionsData.add(
            Switch(
                "Match text field mask",
                { Instance().processParams().matchTextFieldMask },
                { Instance().processParams().matchTextFieldMask = it })
        )
        sectionsData.add(
            Switch(
                "Fast document detection",
                { Instance().processParams().fastDocDetect },
                { Instance().processParams().fastDocDetect = it })
        )

        sectionsData.add(
            Switch(
                "Disable perforation OCR",
                { Instance().processParams().disablePerforationOCR },
                { Instance().processParams().disablePerforationOCR = it })
        )

        sectionsData.add(
            Switch(
                "Respect image quality",
                { Instance().processParams().respectImageQuality },
                { Instance().processParams().respectImageQuality = it })
        )

        sectionsData.add(
            Switch(
                "Split names",
                { Instance().processParams().splitNames },
                { Instance().processParams().splitNames = it })
        )

        sectionsData.add(
            BSMulti(
                "Document group filter",
                "List of specific eligible document types from DocumentType enum to recognize from",
                arrayListOf(
                    BSItem("Passport", dtPassport),
                    BSItem("Identity card", dtIdentityCard),
                    BSItem("Diplomatic passport", dtDiplomaticPassport),
                    BSItem("Service passport", dtServicePassport),
                    BSItem("Travel document", dtTravelDocument),
                    BSItem("ID2 Visa", dtVisaID2),
                    BSItem("ID3 Visa", dtVisaID3),
                    BSItem("Registration certificate", dtRegistrationCertificate),
                    BSItem("Driving license", dtDrivingLicense)
                ),
                {
                    (Instance().processParams().documentGroupFilter?.toMutableList()
                        ?: mutableListOf()).toMutableListString()
                },
                {
                    Instance().processParams().documentGroupFilter =
                        it.toTypedArray().toIntArray()
                }
            ))

        sectionsData.add(Section("Image QA"))
        sectionsData.add(
            Stepper(
                "DPI threshold",
                "",
                {
                    val dpiThresholdValue = Instance().processParams().imageQA.dpiThreshold
                    if (dpiThresholdValue != null) dpiThresholdValue else -1
                },
                {
                    if (it != -1) Instance().processParams().imageQA.dpiThreshold = it
                    else Instance().processParams().imageQA.dpiThreshold = null
                },
                step = 25,
                addMinusOne = true
            )
        )
        sectionsData.add(
            Stepper(
                "Angle threshold",
                "",
                {
                    val angleThresholdValue = Instance().processParams().imageQA.angleThreshold
                    if (angleThresholdValue != null) angleThresholdValue else -1
                },
                {
                    if (it != -1) Instance().processParams().imageQA.angleThreshold = it
                    else Instance().processParams().imageQA.angleThreshold = null
                },
            )
        )
        sectionsData.add(
            Switch(
                "Focus check",
                { Instance().processParams().imageQA.focusCheck },
                { Instance().processParams().imageQA.focusCheck = it })
        )
        sectionsData.add(
            Switch(
                "Glares check",
                { Instance().processParams().imageQA.glaresCheck },
                { Instance().processParams().imageQA.glaresCheck = it })
        )
        sectionsData.add(
            Switch(
                "Colorness check",
                { Instance().processParams().imageQA.colornessCheck },
                { Instance().processParams().imageQA.colornessCheck = it })
        )
        sectionsData.add(
            Switch(
                "Screen Capture",
                { Instance().processParams().imageQA.screenCapture },
                { Instance().processParams().imageQA.screenCapture = it })
        )
        sectionsData.add(
            BSMulti(
                "Expected Pass",
                "Expected Pass Options",
                arrayListOf(
                    BSItem("imageGlares", eImageQualityCheckType.IQC_IMAGE_GLARES),
                    BSItem("imageFocus", eImageQualityCheckType.IQC_IMAGE_FOCUS),
                    BSItem("imageResolution", eImageQualityCheckType.IQC_IMAGE_RESOLUTION),
                    BSItem("imageColorness", eImageQualityCheckType.IQC_IMAGE_COLORNESS),
                    BSItem("imagePerspective", eImageQualityCheckType.IQC_PERSPECTIVE),
                    BSItem("imageBounds", eImageQualityCheckType.IQC_BOUNDS),
                    BSItem("imageCapture", eImageQualityCheckType.IQC_SCREEN_CAPTURE),
                    BSItem("portrait", eImageQualityCheckType.IQC_PORTRAIT),
                    BSItem("handwritten", eImageQualityCheckType.IQC_HANDWRITTEN)
                ),
                {
                    (Instance().processParams().imageQA.expectedPass?.toMutableList()
                        ?: mutableListOf()).toMutableListString()
                },
                {
                    Instance().processParams().imageQA.expectedPass =
                        it.toTypedArray().toIntArray()
                }
            ))

        sectionsData.add(InputDouble("Max glaring part",
            { Instance().processParams().imageQA.glaresCheckParams?.maxGlaringPart },
            {
                Instance().processParams().imageQA.glaresCheckParams =
                    Instance().processParams().imageQA.glaresCheckParams
                        ?: ImageQA.GlaresCheckParams()
                Instance().processParams().imageQA.glaresCheckParams?.maxGlaringPart = it
            }
        ))

        sectionsData.add(InputDouble("Image margin part",
            { Instance().processParams().imageQA.glaresCheckParams?.imgMarginPart },
            {
                Instance().processParams().imageQA.glaresCheckParams =
                    Instance().processParams().imageQA.glaresCheckParams
                        ?: ImageQA.GlaresCheckParams()
                Instance().processParams().imageQA.glaresCheckParams?.imgMarginPart = it
            }
        ))

        sectionsData.add(Section("Restrictions"))
        sectionsData.add(
            BS(
                "Force document format",
                "processParams.forceDocFormat",
                arrayListOf(
                    BSItem("ID1", 0),
                    BSItem("ID2", 1),
                    BSItem("ID3", 2),
                    BSItem("NON", 3),
                    BSItem("A4", 4),
                    BSItem("ID3_x2", 5),
                    BSItem("ID1_90", 10),
                    BSItem("ID1_180", 11),
                    BSItem("ID1_270", 12),
                    BSItem("ID2_180", 13),
                    BSItem("ID3_180", 14),
                    BSItem("Custom", 1000),
                    BSItem("Flexible", 1002)
                ),
                { Instance().processParams().forceDocFormat.toString() },
                { Instance().processParams().forceDocFormat = it.toInt() }
            ))
        sectionsData.add(
            InputInt(
                "Force document ID",
                { Instance().processParams().forceDocID },
                { Instance().processParams().forceDocID = it })
        )
        sectionsData.add(
            InputInt(
                "Minimal holder age",
                { Instance().processParams().minimalHolderAge },
                { Instance().processParams().minimalHolderAge = it })
        )
        sectionsData.add(
            Switch(
                "Update OCR validity by glare",
                { Instance().processParams().updateOCRValidityByGlare },
                { Instance().processParams().updateOCRValidityByGlare = it })
        )
        sectionsData.add(Section("Timeouts"))
        sectionsData.add(
            Stepper(
                "Timeout",
                "sec",
                { Instance().processParams().timeout?.toInt() ?: -1 },
                { Instance().processParams().timeout = it.toDouble() })
        )
        sectionsData.add(
            Stepper(
                "Timeout from first detect",
                "sec",
                { Instance().processParams().timeoutFromFirstDetect?.toInt() ?: -1 },
                { Instance().processParams().timeoutFromFirstDetect = it.toDouble() })
        )
        sectionsData.add(
            Stepper(
                "Timeout from first document type",
                "sec",
                { Instance().processParams().timeoutFromFirstDocType?.toInt() ?: -1 },
                { Instance().processParams().timeoutFromFirstDocType = it.toDouble() })
        )
        sectionsData.add(Section("Display formats"))
        sectionsData.add(
            BS(
                "Date format",
                "processParams.dateFormat",
                arrayListOf(
                    BSItem("dd.MM.yyyy"),
                    BSItem("dd/mm/yyyy"),
                    BSItem("mm/dd/yyyy"),
                    BSItem("dd-mm-yyyy"),
                    BSItem("mm-dd-yyyy"),
                    BSItem("dd/mm/yy")
                ),
                { Instance().processParams().dateFormat },
                { Instance().processParams().dateFormat = it })
        )
        sectionsData.add(
            BS(
                "Measure format",
                "processParams.measureSystem",
                arrayListOf(
                    BSItem("Metric"),
                    BSItem("Imperial")
                ),
                { measureSystemIntToString[Instance().processParams().measureSystem]!! },
                { Instance().processParams().measureSystem = measureSystemStringToInt[it]!! }
            ))
        sectionsData.add(Section("Logs"))
        sectionsData.add(
            Switch(
                "Show logs",
                { Instance().processParams().isLogEnable },
                { Instance().processParams().setLogs(it) })
        )
        sectionsData.add(
            Switch(
                "Save logs",
                { Instance().processParams().debugSaveLogs },
                { Instance().processParams().debugSaveLogs = it })
        )
        sectionsData.add(
            Switch(
                "Save images",
                { Instance().processParams().debugSaveImages },
                { Instance().processParams().debugSaveImages = it })
        )
        sectionsData.add(
            Switch(
                "Save cropped images",
                { Instance().processParams().debugSaveCroppedImages },
                { Instance().processParams().debugSaveCroppedImages = it })
        )
        sectionsData.add(
            Switch(
                "Save RFID session",
                { Instance().processParams().debugSaveRFIDSession },
                { Instance().processParams().debugSaveRFIDSession = it })
        )
        sectionsData.add(Section("Scenarios"))
        val scenarios = arrayListOf<BSItem>()
        for (scenario in Instance().availableScenarios)
            scenarios.add(BSItem(scenario.name))
        sectionsData.add(
            BS(
                "Capture button scenario",
                "processParams.captureButtonScenario",
                scenarios,
                { Instance().processParams().captureButtonScenario },
                { Instance().processParams().captureButtonScenario = it }
            ))
        sectionsData.add(Section("Barcode types"))
        sectionsData.add(
            BSMulti(
                "Do barcodes",
                "processParams.doBarcodes",
                arrayListOf(
                    BSItem(BarcodeType.valueOf(BarcodeType.UNKNOWN)),
                    BSItem(BarcodeType.valueOf(BarcodeType.BCT_CODE128)),
                    BSItem(BarcodeType.valueOf(BarcodeType.CODE39)),
                    BSItem(BarcodeType.valueOf(BarcodeType.EAN8)),
                    BSItem(BarcodeType.valueOf(BarcodeType.ITF)),
                    BSItem(BarcodeType.valueOf(BarcodeType.PDF417)),
                    BSItem(BarcodeType.valueOf(BarcodeType.STF)),
                    BSItem(BarcodeType.valueOf(BarcodeType.MTF)),
                    BSItem(BarcodeType.valueOf(BarcodeType.IATA)),
                    BSItem(BarcodeType.valueOf(BarcodeType.CODABAR)),
                    BSItem(BarcodeType.valueOf(BarcodeType.UPCA)),
                    BSItem(BarcodeType.valueOf(BarcodeType.CODE93)),
                    BSItem(BarcodeType.valueOf(BarcodeType.UPCE)),
                    BSItem(BarcodeType.valueOf(BarcodeType.EAN13)),
                    BSItem(BarcodeType.valueOf(BarcodeType.QRCODE)),
                    BSItem(BarcodeType.valueOf(BarcodeType.AZTEC)),
                    BSItem(BarcodeType.valueOf(BarcodeType.DATAMATRIX)),
                    BSItem(BarcodeType.valueOf(BarcodeType.ALL_1D)),
                    BSItem(BarcodeType.valueOf(BarcodeType.CODE11)),
                    BSItem(BarcodeType.valueOf(BarcodeType.JABCODE))
                ),
                { Instance().processParams().doBarcodes?.toMutableList() ?: mutableListOf() },
                { Instance().processParams().doBarcodes = it.toTypedArray() }
            ))
        sectionsData.add(
            InputInt(
                "Barcode parser type",
                { Instance().processParams().barcodeParserType },
                { Instance().processParams().barcodeParserType = it })
        )
        sectionsData.add(Section("Filters"))
        sectionsData.add(
            BSMulti(
                "Mrz formats filter",
                "processParams.mrzFormatsFilter",
                arrayListOf(
                    BSItem(FORMAT_1X30),
                    BSItem(FORMAT_3X30),
                    BSItem(FORMAT_2X36),
                    BSItem(FORMAT_2X44),
                    BSItem(FORMAT_1X6),
                    BSItem(FORMAT_2X30)
                ),
                { Instance().processParams().mrzFormatsFilter?.toMutableList() ?: mutableListOf() },
                { Instance().processParams().mrzFormatsFilter = it.toTypedArray() }
            ))
        sectionsData.add(
            InputString(
                "Document ID List",
                {
                    if (Instance().processParams().documentIDList == null || Instance().processParams().documentIDList?.isEmpty() == true)
                        return@InputString ""
                    listToString(Instance().processParams().documentIDList?.toList(), context)
                },
                {
                    try {
                        Instance().processParams().documentIDList = it.toIntArray()
                    } catch (e: Exception) {
                    }
                })
        )
        sectionsData.add(
            InputString(
                "Field type filter",
                {
                    if (Instance().processParams().fieldTypesFilter == null || Instance().processParams().fieldTypesFilter?.isEmpty() == true)
                        return@InputString ""
                    listToString(Instance().processParams().fieldTypesFilter?.toList(), context)
                },
                {
                    try {
                        Instance().processParams().fieldTypesFilter = it.toIntArray()
                    } catch (e: Exception) {
                    }
                })
        )
        sectionsData.add(Section("Detection"))
        sectionsData.add(
            Switch(
                "Disable focusing check",
                { Instance().processParams().disableFocusingCheck },
                { Instance().processParams().disableFocusingCheck = it })
        )
        sectionsData.add(
            Switch(
                "Motion detection",
                { functionality.isVideoCaptureMotionControl },
                { functionality.edit().setVideoCaptureMotionControl(it).apply() })
        )
        sectionsData.add(
            Switch(
                "Focusing detection",
                { functionality.isSkipFocusingFrames },
                { functionality.edit().setSkipFocusingFrames(it).apply() })
        )
        sectionsData.add(
            Stepper(
                "Perspective angle",
                "",
                { Instance().processParams().perspectiveAngle },
                { Instance().processParams().perspectiveAngle = it })
        )
        sectionsData.add(
            Switch(
                "Already cropped",
                { Instance().processParams().alreadyCropped },
                { Instance().processParams().alreadyCropped = it })
        )
        sectionsData.add(
            InputDouble(
                "Minimal document area",
                { Instance().processParams().documentAreaMin ?: 1.0 },
                { Instance().processParams().documentAreaMin = it })
        )
        sectionsData.add(Section("Results"))
        sectionsData.add(
            BSMulti(
                "Result types",
                "processParams.resultTypeOutput",
                arrayListOf(
                    BSItem("None", -1),
                    BSItem("Empty", 0),
                    BSItem("Raw Image", 1),
                    BSItem("File Image", 2),
                    BSItem("Mrz OCR Extended", 3),
                    BSItem("Barcodes", 5),
                    BSItem("Graphics", 6),
                    BSItem("Mrz Test Quality", 7),
                    BSItem("Document Types Candidates", 8),
                    BSItem("Chosen Document Type Candidate", 9),
                    BSItem("Documents Info List", 10),
                    BSItem("OCR Lexical Analyze", 15),
                    BSItem("Raw Uncropped Image", 16),
                    BSItem("Visual OCR Extended", 17),
                    BSItem("Bar Codes Text Data", 18),
                    BSItem("Bar Codes Image Data", 19),
                    BSItem("EOS Image", 23),
                    BSItem("Bayer Image", 24),
                    BSItem("Magnetic Stripe", 25),
                    BSItem("Magnetic Stripe Text Data", 26),
                    BSItem("Field File Image", 27),
                    BSItem("Database Check", 28),
                    BSItem("Fingerprint Template ISO", 29),
                    BSItem("Input Image Quality", 30),
                    BSItem("Images", 37),
                    BSItem("Holo Params", 47),
                    BSItem("Mrz Position", 61),
                    BSItem("Barcode Position", 62),
                    BSItem("Document Position", 85),
                    BSItem("Custom", 100),
                    BSItem("RFID Raw Data", 101),
                    BSItem("RFID Text Data", 102),
                    BSItem("RFID Image Data", 103),
                    BSItem("RFID Binary Data", 104),
                    BSItem("RFID Original Graphics", 105)
                ),
                {
                    (Instance().processParams().resultTypeOutput
                        ?.toMutableList() ?: mutableListOf()).toMutableListString()
                },
                { Instance().processParams().resultTypeOutput = it.toTypedArray().toIntArray() }
            ))
        sectionsData.add(
            Switch(
                "Return uncropped image",
                { Instance().processParams().returnUncroppedImage },
                { Instance().processParams().returnUncroppedImage = it })
        )
        sectionsData.add(
            Switch(
                "Integral image",
                { Instance().processParams().integralImage },
                { Instance().processParams().integralImage = it })
        )
        sectionsData.add(
            Stepper(
                "Minimal DPI",
                "",
                { Instance().processParams().minDPI },
                { Instance().processParams().minDPI = it },
                step = 25,
                addMinusOne = true
            )
        )
        sectionsData.add(
            Stepper(
                "Image DPI maximum output",
                "",
                { Instance().processParams().imageDpiOutMax ?: 0 },
                { Instance().processParams().imageDpiOutMax = it },
                step = 25,
                addMinusOne = true
            )
        )
        sectionsData.add(
            Switch(
                "Return cropped barcode",
                { Instance().processParams().returnCroppedBarcode },
                { Instance().processParams().returnCroppedBarcode = it })
        )
        sectionsData.add(Section("Custom params"))
        sectionsData.add(
            InputString(
                "Custom params",
                { Instance().processParams().customParams?.toString() ?: "" },
                {
                    if (it.trim().isEmpty())
                        Instance().processParams().customParams = null
                    else try {
                        Instance().processParams().customParams = JSONObject(it)
                    } catch (e: java.lang.Exception) {
                    }
                })
        )
        sectionsData.add(Section("Scanning mode"))
        sectionsData.add(
            BS(
                "Capture mode",
                "functionality.captureMode",
                arrayListOf(
                    BSItem("Auto"),
                    BSItem("Capture video"),
                    BSItem("Capture frame")
                ),
                { captureModeIntToString[functionality.captureMode]!! },
                { functionality.edit().setCaptureMode(captureModeStringToInt[it]!!).apply() }
            ))
        sectionsData.add(Section("Video settings"))
        sectionsData.add(
            Switch(
                "Adjust zoom level",
                { functionality.isZoomEnabled },
                { functionality.edit().setZoomEnabled(it).apply() })
        )
        sectionsData.add(
            Stepper(
                "Zoom factor",
                "x",
                { functionality.zoomFactor.toInt() },
                { functionality.edit().setZoomFactor(it.toFloat()).apply() })
        )
        sectionsData.add(Section("Extra info"))
        sectionsData.add(
            Switch(
                "Show metadata",
                { functionality.isDisplayMetaData },
                { functionality.edit().setDisplayMetadata(it).apply() })
        )
        sectionsData.add(Section("Camera frame"))
        sectionsData.add(
            BS(
                "Frame type",
                "functionality.cameraFrame",
                arrayListOf(
                    BSItem(NONE),
                    BSItem(DOCUMENT),
                    BSItem(MAX),
                    BSItem(SCENARIO_DEFAULT)
                ),
                { functionality.cameraFrame },
                { functionality.edit().setCameraFrame(it).apply() }
            ))
        sectionsData.add(Section("Licensing"))
        sectionsData.add(
            Stepper(
                "Shift expiry date",
                "month(s)",
                { Instance().processParams().shiftExpiryDate ?: 0 },
                { Instance().processParams().shiftExpiryDate = it },
                step = 1,
                min = Int.MIN_VALUE,
                addMinusOne = false
            )
        )

        sectionsData.add(Scan("", ACTION_TYPE_CUSTOM))

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
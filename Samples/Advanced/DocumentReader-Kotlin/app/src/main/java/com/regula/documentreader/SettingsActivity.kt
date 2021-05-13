package com.regula.documentreader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.regula.documentreader.Helpers.Companion.captureModeIntToString
import com.regula.documentreader.Helpers.Companion.captureModeStringToInt
import com.regula.documentreader.Helpers.Companion.listToString
import com.regula.documentreader.Helpers.Companion.measureSystemIntToString
import com.regula.documentreader.Helpers.Companion.measureSystemStringToInt
import com.regula.documentreader.Helpers.Companion.reloadFragment
import com.regula.documentreader.Helpers.Companion.replaceFragment
import com.regula.documentreader.Helpers.Companion.setProcessParams
import com.regula.documentreader.Helpers.Companion.toIntArray
import com.regula.documentreader.Scan.Companion.ACTION_TYPE_CUSTOM
import com.regula.documentreader.SettingsActivity.Companion.functionality
import com.regula.documentreader.api.DocumentReader.Instance
import com.regula.documentreader.api.enums.DocReaderFrame.*
import com.regula.documentreader.api.params.Functionality
import com.regula.documentreader.api.params.ProcessParam
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
        binding.resetBtn.setOnClickListener { reset() }

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

    private fun reset() {
        when (selectedTabIndex) {
            0 -> {
                isRfidEnabled = false
                useCustomRfidActivity = false
                reloadFragment(applicationSettingsFragment, this)
            }
            1 -> {
                val scenario = Instance().processParams().scenario
                setProcessParams(ProcessParam())
                Instance().processParams().scenario = scenario
                functionality = Functionality()
                reloadFragment(apiSettingsFragment, this)
            }
        }
    }

    companion object {
        var isRfidEnabled = false
        var useCustomRfidActivity = false
        var functionality = Functionality()
    }
}

class ApplicationSettingsFragment : Fragment() {
    private var _binding: FragmentRvBinding? = null
    private val binding get() = _binding!!

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
        sectionsData.add(
            Switch(
                "Use custom RFID activity",
                { SettingsActivity.useCustomRfidActivity },
                { SettingsActivity.useCustomRfidActivity = it })
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.adapter = CommonRecyclerAdapter(sectionsData)
        binding.recyclerView.addItemDecoration(DividerItemDecoration(activity, 1))

        return binding.root
    }
}

class APISettingsFragment : Fragment() {
    private var _binding: FragmentRvBinding? = null
    private val binding get() = _binding!!

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
                "Camera Switch button",
                { functionality.isShowCameraSwitchButton },
                { functionality.edit().setShowCameraSwitchButton(it).apply() })
        )
        sectionsData.add(
            Switch(
                "Capture button",
                { functionality.isShowCaptureButton },
                {
                    functionality.edit().setShowCaptureButton(it).apply()
                    adapter.notifyItemChanged(4)
                    adapter.notifyItemChanged(5)
                })
        )
        sectionsData.add(
            Stepper(
                "Capture button delay (from start)",
                "sec",
                { functionality.showCaptureButtonDelayFromStart.toInt() },
                { functionality.edit().setShowCaptureButtonDelayFromStart(it.toLong()).apply() },
                { functionality.isShowCaptureButton })
        )
        sectionsData.add(
            Stepper(
                "Capture button delay (from detect)",
                "sec",
                { functionality.showCaptureButtonDelayFromDetect.toInt() },
                { functionality.edit().setShowCaptureButtonDelayFromDetect(it.toLong()).apply() },
                { functionality.isShowCaptureButton })
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
        sectionsData.add(
            Switch(
                "Double-page spread processing",
                { Instance().processParams().doublePageSpread },
                { Instance().processParams().doublePageSpread = it })
        )
        sectionsData.add(
            Switch(
                "Manual crop",
                { Instance().processParams().manualCrop },
                { Instance().processParams().manualCrop = it })
        )
        sectionsData.add(Section("Authenticity"))
        sectionsData.add(
            Switch(
                "Check hologram",
                { Instance().processParams().checkHologram },
                { Instance().processParams().checkHologram = it })
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
                    BSItem(".unknown"),
                    BSItem(".code128"),
                    BSItem(".code39"),
                    BSItem(".EAN8"),
                    BSItem(".ITF"),
                    BSItem(".PDF417"),
                    BSItem(".PDF417"),
                    BSItem(".STF"),
                    BSItem(".MTF"),
                    BSItem(".IATA"),
                    BSItem(".CODABAR"),
                    BSItem(".UPCA"),
                    BSItem(".CODE93"),
                    BSItem(".UPCE"),
                    BSItem(".EAN13"),
                    BSItem(".QRCODE"),
                    BSItem(".AZTEC"),
                    BSItem(".DATAMATRIX"),
                    BSItem(".ALL_1D"),
                    BSItem(".code11")
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
            InputString(
                "Document ID List",
                {
                    if (Instance().processParams().documentIDList == null || Instance().processParams().documentIDList.isEmpty())
                        return@InputString ""
                    listToString(Instance().processParams().documentIDList.toList(), context)
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
                    if (Instance().processParams().fieldTypesFilter == null || Instance().processParams().fieldTypesFilter.isEmpty())
                        return@InputString ""
                    listToString(Instance().processParams().fieldTypesFilter.toList(), context)
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
        sectionsData.add(Section("Output images"))
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
                "Minimum DPI",
                "",
                { Instance().processParams().minDPI },
                { Instance().processParams().minDPI = it },
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
        sectionsData.add(Scan("", ACTION_TYPE_CUSTOM))

        return binding.root
    }
}
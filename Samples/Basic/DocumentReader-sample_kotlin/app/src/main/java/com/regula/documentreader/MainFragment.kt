package com.regula.documentreader

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.fragment.app.Fragment
import com.regula.documentreader.BaseActivity.Companion.DO_RFID
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.enums.eGraphicFieldType
import com.regula.documentreader.api.enums.eRPRM_Lights
import com.regula.documentreader.api.enums.eRPRM_ResultType
import com.regula.documentreader.api.enums.eVisualFieldType
import com.regula.documentreader.api.results.DocumentReaderResults
import com.regula.documentreader.util.BluetoothPermissionHelper.isPermissionsGranted
import com.regula.documentreader.util.BluetoothPermissionHelper.requestBlePermissions

class MainFragment : Fragment() {

    var nameTv: TextView? = null
    var showScanner: TextView? = null
    var recognizeImage: TextView? = null
    var recognizePdf: TextView? = null
    var scanFingerprint: TextView? = null

    var portraitIv: ImageView? = null
    var docImageIv: ImageView? = null
    var uvImageView: ImageView? = null
    var irImageView: ImageView? = null

    var doRfidCb: CheckBox? = null

    var scenarioLv: ListView? = null

    @Volatile
    var mCallbacks: MainCallbacks? = null

    private var customCameraRg: RadioGroup? = null
    private var customCameraRb: RadioButton? = null
    private var customCamera2Rb: RadioButton? = null
    private var customCameraRegRb: RadioButton? = null

    companion object {
        var isCustomRfidSelected = false
        var RFID_RESULT = 100
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root: View = inflater.inflate(R.layout.fragment_main, container, false)
        nameTv = root.findViewById(R.id.nameTv)
        showScanner = root.findViewById(R.id.showScannerLink)
        recognizeImage = root.findViewById(R.id.recognizeImageLink)
        recognizePdf = root.findViewById(R.id.recognizePdfLink)
        scanFingerprint = root.findViewById(R.id.fingerScannerLink)

        portraitIv = root.findViewById(R.id.portraitIv)
        docImageIv = root.findViewById(R.id.documentImageIv)
        uvImageView = root.findViewById(R.id.uvImageView)
        irImageView = root.findViewById(R.id.irImageView)
        scenarioLv = root.findViewById(R.id.scenariosList)
        doRfidCb = root.findViewById(R.id.doRfidCb)

        customCameraRg = root.findViewById(R.id.customCameraRg)
        customCameraRb = root.findViewById(R.id.customCameraRb)
        customCamera2Rb = root.findViewById(R.id.customCamera2Rb)
        customCameraRegRb = root.findViewById(R.id.customRegCameraRb)

        initView()
        return root
    }

    override fun onResume() { //used to show scenarios after fragments transaction
        super.onResume()
        if (activity != null && DocumentReader.Instance().isReady) (activity as BaseActivity?)!!.setScenarios()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mCallbacks = activity as MainCallbacks?
    }

    override fun onDetach() {
        super.onDetach()
        mCallbacks = null
    }

    fun initView() {
        recognizePdf!!.setOnClickListener { v: View? -> mCallbacks?.recognizePdf() }
        scanFingerprint!!.setOnClickListener { v: View? ->
//            BlePermissionUtils.askForPermissions(this.getActivity());
            if (!this.requireActivity().packageManager
                    .hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
            ) {
                Toast.makeText(activity, "ble not supported", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            if (!this.requireActivity().packageManager
                    .hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
            ) {
                Toast.makeText(activity, "ble not supported", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            requestBlePermissions(
                this.activity,
                PackageManager.PERMISSION_GRANTED
            )
            if (isPermissionsGranted(
                    activity
                )
            ) {
                val intent =
                    Intent(activity, FingerScannerActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(
                    activity,
                    "ble permission is not granted",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        recognizeImage!!.setOnClickListener { view: View? ->
            if (!DocumentReader.Instance().documentReaderIsReady) return@setOnClickListener
            clearResults()
            mCallbacks?.recognizeImage()
        }
        showScanner!!.setOnClickListener { view: View? ->
            clearResults()
            launchCamera((activity as BaseActivity?)!!.activeCameraMode)
        }
        scenarioLv!!.onItemClickListener =
            OnItemClickListener { adapterView: AdapterView<*>, view: View?, i: Int, l: Long ->
                val adapter = adapterView.adapter as ScenarioAdapter
                mCallbacks?.scenarioLv(adapter.getItem(i))
                adapter.setSelectedPosition(i)
                adapter.notifyDataSetChanged()
            }
    }

    fun displayResults(results: DocumentReaderResults?) {
        if (results != null) {
            val name = results.getTextFieldValueByType(eVisualFieldType.FT_SURNAME_AND_GIVEN_NAMES)
            if (name != null) {
                nameTv!!.text = name
            }

            // through all text fields
            if (results.textResult != null) {
                for (textField in results.textResult!!.fields) {
                    val value = results.getTextFieldValueByType(textField.fieldType, textField.lcid)
                }
            }
            val portrait = results.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT)
            if (portrait != null) {
                portraitIv!!.setImageBitmap(portrait)
            }
            var documentImage =
                results.getGraphicFieldImageByType(eGraphicFieldType.GF_DOCUMENT_IMAGE)
            if (documentImage != null) {
                val aspectRatio = documentImage.width.toDouble() / documentImage.height
                    .toDouble()
                documentImage = Bitmap.createScaledBitmap(
                    documentImage,
                    (480 * aspectRatio).toInt(), 480, false
                )
                docImageIv!!.setImageBitmap(documentImage)
            }

            val uvDocumentReaderGraphicField = results.getGraphicFieldByType(
                eGraphicFieldType.GF_DOCUMENT_IMAGE,
                eRPRM_ResultType.RPRM_RESULT_TYPE_RAW_IMAGE, 0, eRPRM_Lights.RPRM_LIGHT_UV
            )

            if (uvDocumentReaderGraphicField != null) {
                uvImageView!!.visibility = View.VISIBLE
                uvImageView!!.setImageBitmap(resizeBitmap(uvDocumentReaderGraphicField.bitmap))
            }

            val irDocumentReaderGraphicField = results.getGraphicFieldByType(
                eGraphicFieldType.GF_DOCUMENT_IMAGE,
                eRPRM_ResultType.RPRM_RESULT_TYPE_RAW_IMAGE, 0, eRPRM_Lights.RPRM_Light_IR_Full
            )

            if (irDocumentReaderGraphicField != null) {
                irImageView!!.visibility = View.VISIBLE
                irImageView!!.setImageBitmap(resizeBitmap(irDocumentReaderGraphicField.bitmap))
            }
        }
    }

    private fun resizeBitmap(bitmap: Bitmap?): Bitmap? {
        if (bitmap != null) {
            val aspectRatio = bitmap.width.toDouble() / bitmap.height.toDouble()
            return Bitmap.createScaledBitmap(bitmap, (480 * aspectRatio).toInt(), 480, false)
        }
        return null
    }

    fun clearResults() {
        nameTv!!.text = ""
        portraitIv!!.setImageResource(R.drawable.portrait)
        docImageIv!!.setImageResource(R.drawable.id)
    }

    fun setAdapter(adapter: ScenarioAdapter?) {
        scenarioLv!!.adapter = adapter
    }

    fun setDoRfid(rfidAvailable: Boolean, sharedPreferences: SharedPreferences) {
        val doRfid = sharedPreferences.getBoolean(DO_RFID, false)
        doRfidCb!!.isChecked = doRfid
        mCallbacks?.setDoRFID(doRfid)

        if (rfidAvailable) {
            doRfidCb!!.setOnCheckedChangeListener { compoundButton, checked ->
                sharedPreferences.edit().putBoolean(DO_RFID, checked).apply()
                mCallbacks?.setDoRFID(checked)
            }

        } else {
            doRfidCb!!.visibility = View.GONE
        }
    }

    private fun launchCamera(activeCameraMode: Int) {
        when (activeCameraMode) {
            CameraMode.SDK_SHOW_SCANNER -> mCallbacks!!.showScanner()
            CameraMode.SHOW_CAMERA_ACTIVITY -> mCallbacks!!.showCameraActivity()
            CameraMode.SHOW_CUSTOM_CAMERA_ACTIVITY -> mCallbacks!!.showCustomCameraActivity()
            CameraMode.SHOW_CUSTOM_CAMERA_ACTIVITY2 -> mCallbacks!!.showCustomCamera2Activity()
        }
    }

    private object CameraMode {
        const val SDK_SHOW_SCANNER = 0
        const val SHOW_CAMERA_ACTIVITY = 1
        const val SHOW_CUSTOM_CAMERA_ACTIVITY = 2
        const val SHOW_CUSTOM_CAMERA_ACTIVITY2 = 3
    }

    interface MainCallbacks {
        fun showCameraActivity()
        fun showCustomCameraActivity()
        fun showCustomCamera2Activity()
        fun recognizePdf()

        fun scenarioLv(item: String?)
        fun showScanner()
        fun recognizeImage()
        fun setDoRFID(checked: Boolean)
    }
}
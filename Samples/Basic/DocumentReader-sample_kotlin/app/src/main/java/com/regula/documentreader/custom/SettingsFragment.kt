package com.regula.documentreader.custom

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import com.regula.documentreader.BaseActivity
import com.regula.documentreader.MainFragment.MainCallbacks
import com.regula.documentreader.R

class SettingsFragment : Fragment() {
    private var cameraRg: RadioGroup? = null
    private var customRfidRb: RadioButton? = null
    private var defaultRfidRb: RadioButton? = null

    private var customCameraRb: RadioButton? = null
    private var customCamera2Rb: RadioButton? = null
    private var customCameraRegRb: RadioButton? = null
    private var defaultCameraRb: RadioButton? = null

    private var mCallbacks: MainCallbacks? = null
    var isCustomRfidSelected = false
    var RFID_RESULT = 100

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root: View = inflater.inflate(R.layout.fragment_settings, container, false)
        cameraRg = root.findViewById(R.id.customCameraRg)
        customRfidRb = root.findViewById(R.id.customRfidReaderRb)
        defaultRfidRb = root.findViewById(R.id.defaultRfidReaderRb)
        defaultCameraRb = root.findViewById(R.id.defaultScannerRb)
        customCameraRb = root.findViewById(R.id.customCameraRb)
        customCamera2Rb = root.findViewById(R.id.customCamera2Rb)
        customCameraRegRb = root.findViewById(R.id.customRegCameraRb)
        (activity as BaseActivity?)?.let { checkCameraByMode(it.activeCameraMode) }
        (activity as BaseActivity?)?.let { checkRfidByMode(it.rfidMode) }
        setupRfid()
        setupCameraRbListeners()
        return root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mCallbacks = activity as MainCallbacks?
    }

    override fun onDetach() {
        super.onDetach()
        mCallbacks = null
    }

    fun setupRfid() {
        defaultRfidRb!!.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            if (isChecked) (activity as BaseActivity?)!!.rfidMode =
                RfidMode.DEFAULT
        }
        customRfidRb!!.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            if (isChecked) (activity as BaseActivity?)!!.rfidMode =
                RfidMode.CUSTOM
        }
    }

    fun setupCameraRbListeners() {
        defaultCameraRb!!.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            if (isChecked) (activity as BaseActivity?)!!.activeCameraMode =
                CameraMode.SDK_SHOW_SCANNER
        }
        customCameraRb!!.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            if (isChecked) (activity as BaseActivity?)!!.activeCameraMode =
                CameraMode.SHOW_CUSTOM_CAMERA_ACTIVITY
        }
        customCamera2Rb!!.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            if (isChecked) (activity as BaseActivity?)!!.activeCameraMode =
                CameraMode.SHOW_CUSTOM_CAMERA_ACTIVITY2
        }
        customCameraRegRb!!.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            if (isChecked) (activity as BaseActivity?)!!.activeCameraMode =
                CameraMode.SHOW_CAMERA_ACTIVITY
        }
    }

    private fun checkCameraByMode(camMode: Int) {
        when (camMode) {
            CameraMode.SDK_SHOW_SCANNER -> defaultCameraRb!!.isChecked = true
            CameraMode.SHOW_CAMERA_ACTIVITY -> customCameraRegRb!!.isChecked = true
            CameraMode.SHOW_CUSTOM_CAMERA_ACTIVITY -> customCameraRb!!.isChecked = true
            CameraMode.SHOW_CUSTOM_CAMERA_ACTIVITY2 -> customCamera2Rb!!.isChecked = true
        }
    }

    private fun checkRfidByMode(rfidMode: Int) {
        when (rfidMode) {
            RfidMode.DEFAULT -> defaultRfidRb!!.isChecked = true
            RfidMode.CUSTOM -> customRfidRb!!.isChecked = true
        }
    }

    object RfidMode {
        const val DEFAULT = 0
        const val CUSTOM = 1
    }

    private object CameraMode {
        const val SDK_SHOW_SCANNER = 0
        const val SHOW_CAMERA_ACTIVITY = 1
        const val SHOW_CUSTOM_CAMERA_ACTIVITY = 2
        const val SHOW_CUSTOM_CAMERA_ACTIVITY2 = 3
    }
}
package com.regula.documentreader

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.fragment.app.Fragment
import com.regula.documentreader.MainActivity.Companion.DO_RFID
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.enums.*
import com.regula.documentreader.api.results.DocumentReaderResults
import com.regula.documentreader.databinding.FragmentMainBinding


class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    @Volatile
    var mCallbacks: MainCallbacks? = null

    companion object {
        var RFID_RESULT = 100
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        _binding = FragmentMainBinding.inflate(layoutInflater, container, false)
        val view = binding.root
        initView()
        return view
    }

    override fun onResume() { //used to show scenarios after fragments transaction
        super.onResume()
        if (activity != null && DocumentReader.Instance().isReady
            && DocumentReader.Instance().availableScenarios.isNotEmpty())
            (activity as MainActivity?)!!.setScenarios()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mCallbacks = activity as MainCallbacks?
    }

    override fun onDetach() {
        super.onDetach()
        mCallbacks = null
    }

    private fun initView() {
        binding.recognizePdfLink.setOnClickListener {
            clearResults()
            mCallbacks?.recognizePdf()
        }
        binding.recognizeImageLink.setOnClickListener {
            if (!DocumentReader.Instance().isReady) return@setOnClickListener
            clearResults()
            mCallbacks?.recognizeImage()
        }
        binding.showScannerLink.setOnClickListener {
            clearResults()
            mCallbacks!!.showScanner()
        }
        binding.scenariosList.onItemClickListener =
            OnItemClickListener { adapterView: AdapterView<*>, _: View?, i: Int, _: Long ->
                val adapter = adapterView.adapter as ScenarioAdapter
                mCallbacks?.scenarioLv(adapter.getItem(i))
                adapter.setSelectedPosition(i)
                adapter.notifyDataSetChanged()
            }
    }

    fun disableUiElements() {
        binding.recognizePdfLink.isClickable = false
        binding.showScannerLink.isClickable = false
        binding.recognizeImageLink.isClickable = false

        binding.recognizePdfLink.setTextColor(Color.GRAY)
        binding.showScannerLink.setTextColor(Color.GRAY)
        binding.recognizeImageLink.setTextColor(Color.GRAY)
    }

    fun displayResults(results: DocumentReaderResults?) {
        if (results != null) {
            val name = results.getTextFieldValueByType(eVisualFieldType.FT_SURNAME_AND_GIVEN_NAMES)
            if (name != null) {
                binding.nameTv.text = name
            }

            // through all text fields
            results.textResult?.fields?.forEach {
                val value = results.getTextFieldValueByType(it.fieldType, it.lcid)
                Log.d("MainActivity", "Text Field: " + context?.let { it1 -> it.getFieldName(it1) } + " value: " + value);
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

    fun setAdapter(adapter: ScenarioAdapter?) {
        binding.scenariosList.adapter = adapter
    }

    fun setDoRfid(rfidAvailable: Boolean, sharedPreferences: SharedPreferences) {
        val doRfid = sharedPreferences.getBoolean(DO_RFID, false)
        binding.doRfidCb.isChecked = doRfid
        mCallbacks?.setDoRFID(doRfid)

        if (rfidAvailable) {
            binding.doRfidCb.setOnCheckedChangeListener { compoundButton, checked ->
                sharedPreferences.edit().putBoolean(DO_RFID, checked).apply()
                mCallbacks?.setDoRFID(checked)
            }

        } else {
            binding.doRfidCb.visibility = View.GONE
        }
    }

    interface MainCallbacks {
        fun recognizePdf()
        fun scenarioLv(item: String?)
        fun showScanner()
        fun recognizeImage()
        fun setDoRFID(checked: Boolean)
    }
}
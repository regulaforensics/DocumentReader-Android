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
import com.regula.documentreader.BaseActivity.Companion.DO_RFID
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.enums.*
import com.regula.documentreader.api.results.DocumentReaderResults
import com.regula.documentreader.api.results.authenticity.DocumentReaderIdentResult


class MainFragment : Fragment() {

    var nameTv: TextView? = null
    var showScanner: TextView? = null
    var recognizeImage: TextView? = null
    var recognizePdf: TextView? = null

    var portraitIv: ImageView? = null
    var docImageIv: ImageView? = null
    var uvImageView: ImageView? = null
    var irImageView: ImageView? = null

    var doRfidCb: CheckBox? = null

    var scenarioLv: ListView? = null

    private var authenticityLayout: RelativeLayout? = null
    private var authenticityResultImg: ImageView? = null

    @Volatile
    var mCallbacks: MainCallbacks? = null

    companion object {
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

        portraitIv = root.findViewById(R.id.portraitIv)
        docImageIv = root.findViewById(R.id.documentImageIv)
        uvImageView = root.findViewById(R.id.uvImageView)
        irImageView = root.findViewById(R.id.irImageView)
        scenarioLv = root.findViewById(R.id.scenariosList)
        doRfidCb = root.findViewById(R.id.doRfidCb)

        authenticityLayout = root.findViewById(R.id.authenticityLayout);
        authenticityResultImg = root.findViewById(R.id.authenticityResultImg);
        initView()
        return root
    }

    override fun onResume() { //used to show scenarios after fragments transaction
        super.onResume()
        if (activity != null && DocumentReader.Instance().isReady
            && DocumentReader.Instance().availableScenarios.isNotEmpty())
            (activity as BaseActivity?)!!.setScenarios()
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
        recognizeImage!!.setOnClickListener { view: View? ->
            if (!DocumentReader.Instance().isReady) return@setOnClickListener
            clearResults()
            mCallbacks?.recognizeImage()
        }
        showScanner!!.setOnClickListener { view: View? ->
            clearResults()
            mCallbacks!!.showScanner()
        }
        scenarioLv!!.onItemClickListener =
            OnItemClickListener { adapterView: AdapterView<*>, view: View?, i: Int, l: Long ->
                val adapter = adapterView.adapter as ScenarioAdapter
                mCallbacks?.scenarioLv(adapter.getItem(i))
                adapter.setSelectedPosition(i)
                adapter.notifyDataSetChanged()
            }
    }

    fun disableUiElements() {
        recognizePdf!!.isClickable = false
        showScanner!!.isClickable = false
        recognizeImage!!.isClickable = false

        recognizePdf!!.setTextColor(Color.GRAY)
        showScanner!!.setTextColor(Color.GRAY)
        recognizeImage!!.setTextColor(Color.GRAY)
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

            if (results.authenticityResult != null
                && DocumentReader.Instance().functionality().isUseAuthenticator
            ) {
                authenticityLayout!!.visibility = View.VISIBLE
                authenticityResultImg!!.setImageResource(if (results.authenticityResult!!.status == eCheckResult.CH_CHECK_OK) R.drawable.correct else R.drawable.incorrect)
                for (check in results.authenticityResult!!.checks) {
                    for (element in check.elements) {
                        if (element is DocumentReaderIdentResult) {
                            Log.d(
                                "AuthenticityCheck",
                                "Element status: " + (if (element.status == eCheckResult.CH_CHECK_OK) "Ok" else "Error") + ", percent: " + element.percentValue
                            )
                        } else {
                            Log.d(
                                "AuthenticityCheck",
                                "Element type: " + element.elementType + ", status: " + if (element.status == eCheckResult.CH_CHECK_OK) "Ok" else "Error"
                            )
                        }
                    }
                }
            } else {
                authenticityLayout!!.visibility = View.GONE
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
        authenticityLayout!!.visibility = View.GONE;
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

    interface MainCallbacks {
        fun recognizePdf()
        fun scenarioLv(item: String?)
        fun showScanner()
        fun recognizeImage()
        fun setDoRFID(checked: Boolean)
    }
}
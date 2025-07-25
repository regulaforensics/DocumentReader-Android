package com.regula.certificate.pinning

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.regula.certificate.pinning.databinding.ActivityMainBinding
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.completions.IDocumentReaderCompletion
import com.regula.documentreader.api.config.RecognizeConfig
import com.regula.documentreader.api.enums.DocReaderAction
import com.regula.documentreader.api.enums.OnlineMode
import com.regula.documentreader.api.enums.Scenario
import com.regula.documentreader.api.enums.eGraphicFieldType
import com.regula.documentreader.api.enums.eVisualFieldType
import com.regula.documentreader.api.params.OnlineProcessingConfig
import com.regula.documentreader.api.results.DocumentReaderResults

class MainActivity : AppCompatActivity() {
    private var loadingDialog: AlertDialog? = null
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.recognizeBtn.setOnClickListener {
            binding.surnameTv.text = "Surname:"
            binding.nameTv.text = "Name:"
            binding.resultIv.setImageBitmap(null)

            recognize()
        }
    }

    private fun recognize() {
        showDialog("Recognize")
        val recognize = RecognizeConfig.Builder(
            OnlineProcessingConfig.Builder(
                OnlineMode.MANUAL,
                Scenario.SCENARIO_FULL_PROCESS
            ).build()
        )
            .setBitmap(
                BitmapFactory.decodeResource(
                    resources,
                    R.drawable.mrz_sample
                )
            )
            .build()

        DocumentReader.Instance().recognize(this, recognize, completion)
    }


    private val completion =
        IDocumentReaderCompletion { action, results, error ->
            //processing is finished, all results are ready
            if(action != DocReaderAction.PROCESSING_ON_SERVICE)
                dismissDialog()

            if (action == DocReaderAction.COMPLETE) {
                displayImage(results)
                displayTextFields(results)
            } else if (action == DocReaderAction.ERROR) {
                Toast.makeText(this@MainActivity, "Error:$error", Toast.LENGTH_LONG).show()
            }
        }


    private fun displayImage(results: DocumentReaderResults?) {
        if (results?.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT) != null) {
            var documentImage =
                results.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT)
            if (documentImage != null) {
                val aspectRatio = documentImage.width.toDouble() / documentImage.height
                    .toDouble()
                documentImage = Bitmap.createScaledBitmap(
                    documentImage,
                    (480 * aspectRatio).toInt(), 480, false
                )
                binding.resultIv.setImageBitmap(documentImage)
            }
        }
    }

    private fun displayTextFields(results: DocumentReaderResults?) {
        if (results?.getTextFieldByType(eVisualFieldType.FT_SURNAME) != null) {
            val surname = "Surname:" + results.getTextFieldValueByType(eVisualFieldType.FT_SURNAME)
            binding.surnameTv.text = surname
        } else {
            binding.surnameTv.text = "Surname:"
        }

        if (results?.getTextFieldByType(eVisualFieldType.FT_GIVEN_NAMES) != null) {
            val name = "Name: " + results.getTextFieldValueByType(eVisualFieldType.FT_GIVEN_NAMES)
            binding.nameTv.text = name
        } else {
            binding.nameTv.text = "Name:"
        }
    }

    private fun dismissDialog() {
        if (loadingDialog != null) {
            loadingDialog!!.dismiss()
        }
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
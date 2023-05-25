package com.regula.customcamera_kotlin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.regula.customcamera_kotlin.custom.Camera2Activity
import com.regula.customcamera_kotlin.custom.CameraActivity
import com.regula.customcamera_kotlin.custom.CustomRegActivity
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.completions.IDocumentReaderInitCompletion
import com.regula.documentreader.api.enums.Scenario
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.params.DocReaderConfig
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    var loadingDialog: AlertDialog? = null
    var cam1Btn: Button? = null
    var cam2Btn: Button? = null
    var regActivityBtn: Button? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeReader()

        cam1Btn = findViewById(R.id.cameraBtn)
        cam2Btn = findViewById(R.id.camera2Btn)
        regActivityBtn = findViewById(R.id.cameraRegBtn)

        cam1Btn!!.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }
        cam2Btn!!.setOnClickListener {
            val intent = Intent(this, Camera2Activity::class.java)
            startActivity(intent)
        }
        regActivityBtn!!.setOnClickListener {
            val intent = Intent(this, CustomRegActivity::class.java)
            startActivity(intent)
        }
    }


    private fun initializeReader() {
        showDialog("initializing document reader...")
        Executors.newSingleThreadExecutor().execute {
            try {
                val licInput = resources.openRawResource(R.raw.regula)
                val available = licInput.available()
                val license = ByteArray(available)
                licInput.read(license)
                licInput.close()
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    val docReaderConfig = DocReaderConfig(license)
                    DocumentReader.Instance()
                        .initializeReader(this@MainActivity, docReaderConfig, initCompletion)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                Toast.makeText(
                    this,
                    "init error: " + ex.localizedMessage,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private val initCompletion =
        IDocumentReaderInitCompletion { result: Boolean, error: DocumentReaderException? ->
            dismissDialog()
            if (result) {
                DocumentReader.Instance().processParams()
                    .setScenario(Scenario.SCENARIO_MRZ)
            } else {
                Toast.makeText(this@MainActivity, "Init failed:$error", Toast.LENGTH_LONG).show()
                return@IDocumentReaderInitCompletion
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
}
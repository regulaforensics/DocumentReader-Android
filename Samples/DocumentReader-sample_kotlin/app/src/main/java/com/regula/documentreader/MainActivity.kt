package com.regula.documentreader

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapFactory.decodeStream
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.completions.IDocumentReaderCompletion
import com.regula.documentreader.api.completions.IDocumentReaderPrepareCompletion
import com.regula.documentreader.api.enums.DocReaderAction
import com.regula.documentreader.api.enums.eGraphicFieldType
import com.regula.documentreader.api.enums.eRFID_Password_Type
import com.regula.documentreader.api.enums.eVisualFieldType
import com.regula.documentreader.api.results.DocumentReaderResults
import kotlinx.android.synthetic.main.activity_main.*
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.*

class MainActivity : AppCompatActivity() {

    private var nameTv: TextView? = null
    private var showScanner: TextView? = null
    private var recognizeImage: TextView? = null

    private var portraitIv: ImageView? = null
    private var docImageIv: ImageView? = null

    private var doRfidCb: CheckBox? = null

    private var scenarioLv: ListView? = null

    private var sharedPreferences: SharedPreferences? = null
    private var doRfid: Boolean = false
    private var loadingDialog: AlertDialog? = null

    private var selectedPosition: Int = 0

    //DocumentReader processing callback
    private val completion =
        IDocumentReaderCompletion { action, results, error ->
            //processing is finished, all results are ready
            if (action == DocReaderAction.COMPLETE) {
                if (loadingDialog != null && loadingDialog!!.isShowing) {
                    loadingDialog!!.dismiss()
                }

                //Checking, if nfc chip reading should be performed
                if (doRfid && results != null && results.chipPage != 0) {
                    //setting the chip's access key - mrz on car access number
                    var accessKey: String?
                    accessKey = results.getTextFieldValueByType(eVisualFieldType.FT_MRZ_STRINGS)
                    if (accessKey != null && !accessKey.isEmpty()) {
                        accessKey = results.getTextFieldValueByType(eVisualFieldType.FT_MRZ_STRINGS)
                            .replace("^", "").replace("\n", "")
                        DocumentReader.Instance().rfidScenario().setMrz(accessKey)
                        DocumentReader.Instance().rfidScenario().setPacePasswordType(eRFID_Password_Type.PPT_MRZ)
                    } else {
                        accessKey = results.getTextFieldValueByType(eVisualFieldType.FT_CARD_ACCESS_NUMBER)
                        if (accessKey != null && !accessKey.isEmpty()) {
                            DocumentReader.Instance().rfidScenario().setPassword(accessKey)
                            DocumentReader.Instance().rfidScenario().setPacePasswordType(eRFID_Password_Type.PPT_CAN)
                        }
                    }
                    //starting chip reading
                    DocumentReader.Instance().startRFIDReader(this@MainActivity) { rfidAction, results_RFIDReader, _ ->
                        if (rfidAction == DocReaderAction.COMPLETE || rfidAction == DocReaderAction.CANCEL) {
                            displayResults(results_RFIDReader)
                        }
                    }
                } else {
                    displayResults(results)
                }
            } else {
                //something happened before all results were ready
                if (action == DocReaderAction.CANCEL) {
                    Toast.makeText(this@MainActivity, "Scanning was cancelled", Toast.LENGTH_LONG).show()
                } else if (action == DocReaderAction.ERROR) {
                    Toast.makeText(this@MainActivity, "Error:$error", Toast.LENGTH_LONG).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        nameTv = findViewById(R.id.nameTv)
        showScanner = findViewById(R.id.showScannerLink)
        recognizeImage = findViewById(R.id.recognizeImageLink)

        portraitIv = findViewById(R.id.portraitIv)
        docImageIv = findViewById(R.id.documentImageIv)

        scenarioLv = findViewById(R.id.scenariosList)

        doRfidCb = findViewById(R.id.doRfidCb)

        sharedPreferences = getSharedPreferences(MY_SHARED_PREFS, Context.MODE_PRIVATE)
    }

    override fun onResume() {
        super.onResume()

        if (!DocumentReader.Instance().documentReaderIsReady) {
            val initDialog = showDialog("Initializing")

            //Reading the license from raw resource file
            try {
                val licInput = resources.openRawResource(R.raw.regula)
                val available = licInput.available()
                val license = ByteArray(available)

                licInput.read(license)

                //preparing database files, it will be downloaded from network only one time and stored on user device
                DocumentReader.Instance().prepareDatabase(
                    this@MainActivity,
                    "Full",
                    object : IDocumentReaderPrepareCompletion {
                        override fun onPrepareProgressChanged(progress: Int) {
                            initDialog.setTitle("Downloading database: $progress%")
                        }

                        override fun onPrepareCompleted(status: Boolean, error: String) {

                            //Initializing the reader
                            DocumentReader.Instance().initializeReader(
                                this@MainActivity, license
                            ) { success, error_initializeReader ->
                                if (initDialog.isShowing) {
                                    initDialog.dismiss()
                                }

                                DocumentReader.Instance().customization().edit().setShowHelpAnimation(false).apply()

                                //initialization successful
                                if (success) {
                                    showScanner!!.setOnClickListener {
                                        clearResults()

                                        //starting video processing
                                        DocumentReader.Instance().showScanner(this@MainActivity, completion)
                                    }

                                    recognizeImage!!.setOnClickListener {
                                        clearResults()
                                        //checking for image browsing permissions
                                        if (ContextCompat.checkSelfPermission(
                                                this@MainActivity,
                                                Manifest.permission.READ_EXTERNAL_STORAGE
                                            ) != PackageManager.PERMISSION_GRANTED
                                        ) {

                                            ActivityCompat.requestPermissions(
                                                this@MainActivity,
                                                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                                                PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
                                            )
                                        } else {
                                            //start image browsing
                                            createImageBrowsingRequest()
                                        }
                                    }

                                    if (DocumentReader.Instance().isRFIDAvailableForUse) {
                                        //reading shared preferences
                                        doRfid = sharedPreferences!!.getBoolean(DO_RFID, false)
                                        doRfidCb!!.isChecked = doRfid
                                        doRfidCb!!.setOnCheckedChangeListener { _, checked ->
                                            doRfid = checked
                                            sharedPreferences!!.edit().putBoolean(DO_RFID, checked).apply()
                                        }
                                    } else {
                                        doRfidCb!!.visibility = View.GONE
                                    }

                                    //getting current processing scenario and loading available scenarios to ListView
                                    var currentScenario: String? = DocumentReader.Instance().processParams().scenario
                                    val scenarios = ArrayList<String>()
                                    for (scenario in DocumentReader.Instance().availableScenarios) {
                                        scenarios.add(scenario.name)
                                    }

                                    //setting default scenario
                                    if (currentScenario == null || currentScenario.isEmpty()) {
                                        currentScenario = scenarios[0]
                                        DocumentReader.Instance().processParams().scenario = currentScenario
                                    }

                                    val adapter = ScenarioAdapter(
                                        this@MainActivity,
                                        android.R.layout.simple_list_item_1,
                                        scenarios
                                    )
                                    selectedPosition = 0
                                    try {
                                        selectedPosition = adapter.getPosition(currentScenario)
                                    } catch (ex: Exception) {
                                        ex.printStackTrace()
                                    }

                                    scenarioLv!!.adapter = adapter

                                    scenarioLv!!.setSelection(selectedPosition)

                                    scenarioLv!!.onItemClickListener =
                                        AdapterView.OnItemClickListener { _, _, i, _ ->
                                            //setting selected scenario to DocumentReader params
                                            DocumentReader.Instance().processParams().scenario = adapter.getItem(i)
                                            selectedPosition = i
                                            adapter.notifyDataSetChanged()
                                        }

                                } else {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Init failed:$error_initializeReader",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }//Initialization was not successful
                            }
                        }
                    })

                licInput.close()

            } catch (ex: Exception) {
                ex.printStackTrace()
            }

        }
    }


    override fun onPause() {
        super.onPause()

        if (loadingDialog != null) {
            loadingDialog!!.dismiss()
            loadingDialog = null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            //Image browsing intent processed successfully
            if (requestCode == REQUEST_BROWSE_PICTURE) {
                if (data!!.data != null) {
                    val selectedImage = data.data
                    val bmp = getBitmap(selectedImage, 1920, 1080)

                    loadingDialog = showDialog("Processing image")

                    if (bmp != null) {
                        DocumentReader.Instance().recognizeImage(bmp, completion)
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //access to gallery is allowed
                    createImageBrowsingRequest()
                } else {
                    Toast.makeText(this@MainActivity, "Permission required, to browse images", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showDialog(msg: String): AlertDialog {
        val dialog = AlertDialog.Builder(this@MainActivity)
        val dialogView = layoutInflater.inflate(R.layout.simple_dialog, topInfoLayout, false)
        dialog.setTitle(msg)
        dialog.setView(dialogView)
        dialog.setCancelable(false)
        return dialog.show()
    }

    //show received results on the UI
    private fun displayResults(results: DocumentReaderResults?) {
        if (results != null) {
            val name = results.getTextFieldValueByType(eVisualFieldType.FT_SURNAME_AND_GIVEN_NAMES)
            if (name != null) {
                nameTv!!.text = name
            }

            // through all text fields
            if (results.textResult != null && results.textResult!!.fields != null) {
                for (textField in results.textResult!!.fields) {
                    val value = results.getTextFieldValueByType(textField.fieldType, textField.lcid)
                    Log.d("MainActivity", value + "\n")
                }
            }

            val portrait = results.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT)
            if (portrait != null) {
                portraitIv!!.setImageBitmap(portrait)
            }

            var documentImage: Bitmap? = results.getGraphicFieldImageByType(eGraphicFieldType.GF_DOCUMENT_IMAGE)
            if (documentImage != null) {
                val aspectRatio = documentImage.width.toDouble() / documentImage.height.toDouble()
                documentImage = Bitmap.createScaledBitmap(documentImage, (480 * aspectRatio).toInt(), 480, false)
                docImageIv!!.setImageBitmap(documentImage)
            }
        }
    }

    private fun clearResults() {
        nameTv!!.text = ""
        portraitIv!!.setImageResource(R.drawable.portrait)
        docImageIv!!.setImageResource(R.drawable.id)
    }

    // creates and starts image browsing intent
    // results will be handled in onActivityResult method
    private fun createImageBrowsingRequest() {
        val intent = Intent()
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_BROWSE_PICTURE)
    }

    // loads bitmap from uri
    private fun getBitmap(selectedImage: Uri?, targetWidth: Int, targetHeight: Int): Bitmap? {
        val resolver = this@MainActivity.contentResolver
        var `is`: InputStream? = null
        try {
            `is` = resolver.openInputStream(selectedImage!!)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeStream(`is`, null, options)

        //Re-reading the input stream to move it's pointer to start
        try {
            `is` = resolver.openInputStream(selectedImage!!)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight)
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false
        return decodeStream(`is`, null, options)
    }

    // see https://developer.android.com/topic/performance/graphics/load-bitmap.html
    private fun calculateInSampleSize(options: BitmapFactory.Options, bitmapWidth: Int, bitmapHeight: Int): Int {
        // Raw height and width of image
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > bitmapHeight || width > bitmapWidth) {

            val halfHeight = height / 2
            val halfWidth = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize > bitmapHeight && halfWidth / inSampleSize > bitmapWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    internal inner class ScenarioAdapter(context: Context, resource: Int, objects: List<String>) :
        ArrayAdapter<String>(context, resource, objects) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent)

            if (position == selectedPosition) {
                view.setBackgroundColor(Color.LTGRAY)
            } else {
                view.setBackgroundColor(Color.TRANSPARENT)
            }
            return view
        }
    }

    companion object {

        private const val REQUEST_BROWSE_PICTURE = 11
        private const val PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 22
        private const val MY_SHARED_PREFS = "MySharedPrefs"
        private const val DO_RFID = "doRfid"
    }
}

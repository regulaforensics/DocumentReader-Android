package com.regula.documentreader

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.regula.documentreader.LicenseUtil.getLicense
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.completions.IDocumentReaderCompletion
import com.regula.documentreader.api.completions.IDocumentReaderPrepareCompletion
import com.regula.documentreader.api.enums.DocReaderAction
import com.regula.documentreader.api.enums.eGraphicFieldType
import com.regula.documentreader.api.enums.eVisualFieldType
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.params.DocReaderConfig
import com.regula.documentreader.api.results.DocumentReaderResults
import com.regula.documentreader.api.results.DocumentReaderScenario
import com.regula.documentreader.custom.CameraActivity
import com.regula.documentreader.custom.CustomRegActivity
import java.io.FileNotFoundException
import java.io.InputStream


class MainActivity : AppCompatActivity() {
    private var nameTv: TextView? = null
    private var showScanner: TextView? = null
    private var recognizeImage: TextView? = null
    private var portraitIv: ImageView? = null
    private var docImageIv: ImageView? = null
    private var doRfidCb: CheckBox? = null
    private var scenarioLv: ListView? = null
    private var sharedPreferences: SharedPreferences? = null
    private var doRfid = false
    private var loadingDialog: AlertDialog? = null
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
        sharedPreferences = getSharedPreferences(MY_SHARED_PREFS, MODE_PRIVATE)
        initView()
    }

    override fun onResume() {
        super.onResume()
        if (!DocumentReader.Instance().isReady) {
            val initDialog = showDialog("Initializing")

            //preparing database files, it will be downloaded from network only one time and stored on user device
            DocumentReader.Instance().prepareDatabase(
                this@MainActivity,
                "Full",
                object : IDocumentReaderPrepareCompletion {
                    override fun onPrepareProgressChanged(progress: Int) {
                        initDialog.setTitle("Downloading database: $progress%")
                    }

                    override fun onPrepareCompleted(
                        status: Boolean,
                        error: DocumentReaderException?
                    ) {
                        if (status) {
                            initDialog.setTitle("Initializing")
                            initializeReader(initDialog)
                        } else {
                            initDialog.dismiss()
                            Toast.makeText(
                                this@MainActivity,
                                "Prepare DB failed:$error",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                })
        }
    }

    override fun onPause() {
        super.onPause()
        if (loadingDialog != null) {
            loadingDialog!!.dismiss()
            loadingDialog = null
        }
    }

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.let { intent ->
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
                            getBitmap(imageUris[0], 1920, 1080)?.let { bitmap ->
                                DocumentReader.Instance().recognizeImage(bitmap, completion)
                            }
                        } else {
                            val bitmaps = arrayOfNulls<Bitmap>(imageUris.size)
                            for (i in bitmaps.indices) {
                                bitmaps[i] = getBitmap(imageUris[i], 1920, 1080)
                            }
                            DocumentReader.Instance().recognizeImages(bitmaps, completion)
                        }
                    }
                }
            }
        }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    //access to gallery is allowed
                    createImageBrowsingRequest()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Permission required, to browse images",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun initView() {
        recognizeImage!!.setOnClickListener { view: View? ->
            if (!DocumentReader.Instance().isReady) return@setOnClickListener
            clearResults()
            //checking for image browsing permissions
            if ((ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                        != PackageManager.PERMISSION_GRANTED)
            ) {
                ActivityCompat.requestPermissions(
                    this@MainActivity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
                )
            } else {
                //start image browsing
                createImageBrowsingRequest()
            }
        }
        showScanner!!.setOnClickListener {
            if (!DocumentReader.Instance().isReady) return@setOnClickListener
            clearResults()

            //starting video processing
            DocumentReader.Instance().showScanner(this@MainActivity, completion)
        }
        scenarioLv!!.onItemClickListener =
            OnItemClickListener { adapterView: AdapterView<*>, _: View?, i: Int, _: Long ->
                if (!DocumentReader.Instance().isReady) return@OnItemClickListener
                val adapter = adapterView.adapter as ScenarioAdapter

                //setting selected scenario to DocumentReader params
                DocumentReader.Instance().processParams().scenario = adapter.getItem(i)!!
                adapter.setSelectedPosition(i)
                adapter.notifyDataSetChanged()
            }
    }

    private fun initializeReader(initDialog: AlertDialog) {
        val config = DocReaderConfig(getLicense(this))
        config.isLicenseUpdate = true

        DocumentReader.Instance().initializeReader(
            this@MainActivity,
            config
        ) { success, error ->
            if (initDialog.isShowing) {
                initDialog.dismiss()
            }
            if (!success) { //Initialization was not successful
                Toast.makeText(this@MainActivity, "Init failed:$error", Toast.LENGTH_LONG).show()
                return@initializeReader
            }
            setupCustomization()
            setupFunctionality()

            //initialization successful
            if (DocumentReader.Instance().isRFIDAvailableForUse) {
                //reading shared preferences
                doRfid = sharedPreferences!!.getBoolean(DO_RFID, false)
                doRfidCb!!.isChecked = doRfid
                doRfidCb!!.setOnCheckedChangeListener { compoundButton, checked ->
                    doRfid = checked
                    sharedPreferences!!.edit().putBoolean(DO_RFID, checked).apply()
                }
            } else {
                doRfidCb!!.visibility = View.GONE
            }

            //getting current processing scenario and loading available scenarios to ListView
            val scenarios = ArrayList<String>()
            for (scenario: DocumentReaderScenario in DocumentReader.Instance().availableScenarios) {
                scenarios.add(scenario.name)
            }

            //setting default scenario
            DocumentReader.Instance().processParams().scenario = scenarios[0]
            val adapter =
                ScenarioAdapter(this@MainActivity, android.R.layout.simple_list_item_1, scenarios)
            scenarioLv!!.adapter = adapter
        }
    }

    private fun setupCustomization() {
        DocumentReader.Instance().customization().edit().setShowHelpAnimation(false).apply()
    }

    private fun setupFunctionality() {
        DocumentReader.Instance().functionality().edit().setShowCameraSwitchButton(true).apply()
    }

    //DocumentReader processing callback
    private val completion = IDocumentReaderCompletion { action, results, error ->
        //processing is finished, all results are ready
        if (action == DocReaderAction.COMPLETE) {
            if (loadingDialog != null && loadingDialog!!.isShowing) {
                loadingDialog!!.dismiss()
            }

            //Checking, if nfc chip reading should be performed
            if (doRfid && results != null && results.chipPage != 0) {
                //starting chip reading
                DocumentReader.Instance()
                    .startRFIDReader(this@MainActivity) { rfidAction, results, _ ->
                        if (rfidAction == DocReaderAction.COMPLETE || rfidAction == DocReaderAction.CANCEL) {
                            displayResults(results)
                        }
                    }
            } else {
                displayResults(results)
            }
        } else {
            //something happened before all results were ready
            if (action == DocReaderAction.CANCEL) {
                Toast.makeText(this@MainActivity, "Scanning was cancelled", Toast.LENGTH_LONG)
                    .show()
            } else if (action == DocReaderAction.ERROR) {
                Toast.makeText(this@MainActivity, "Error:$error", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showDialog(msg: String): AlertDialog {
        val dialog = AlertDialog.Builder(this@MainActivity)
        val dialogView = layoutInflater.inflate(R.layout.simple_dialog, null)
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
            if (results.textResult != null) {
                for (textField in results.textResult!!.fields) {
                    val value = results.getTextFieldValueByType(textField.fieldType, textField.lcid)
                    Log.d("MainActivity", """  $value""".trimIndent())
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
                    (480 * aspectRatio).toInt(),
                    480,
                    false
                )
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
    // results will be handled by resultLauncher object
    private fun createImageBrowsingRequest() {
        val intent = Intent()
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.action = Intent.ACTION_GET_CONTENT
        resultLauncher.launch(intent)
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
        return BitmapFactory.decodeStream(`is`, null, options)
    }

    // see https://developer.android.com/topic/performance/graphics/load-bitmap.html
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        bitmapWidth: Int,
        bitmapHeight: Int
    ): Int {
        // Raw height and width of image
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > bitmapHeight || width > bitmapWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize > bitmapHeight
                && halfWidth / inSampleSize > bitmapWidth
            ) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    companion object {
        private const val REQUEST_BROWSE_PICTURE = 11
        private const val PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 22
        private const val MY_SHARED_PREFS = "MySharedPrefs"
        private const val DO_RFID = "doRfid"
    }

    fun clickOnShowCameraActivity(view: View?) {
        if (!DocumentReader.Instance().isReady) return
        val cameraIntent = Intent()
        cameraIntent.setClass(this@MainActivity, CameraActivity::class.java)
        startActivity(cameraIntent)
    }

    fun clickOnShowCustomCameraActivity(view: View?) {
        if (!DocumentReader.Instance().isReady) return
        val cameraIntent = Intent()
        cameraIntent.setClass(this@MainActivity, CustomRegActivity::class.java)
        startActivity(cameraIntent)
    }
}
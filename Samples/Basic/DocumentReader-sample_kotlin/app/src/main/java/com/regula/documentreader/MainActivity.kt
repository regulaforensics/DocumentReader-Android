package com.regula.documentreader

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.regula.documentreader.MainFragment.Companion.RFID_RESULT
import com.regula.documentreader.MainFragment.MainCallbacks
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.completions.IDocumentReaderCompletion
import com.regula.documentreader.api.completions.IDocumentReaderInitCompletion
import com.regula.documentreader.api.completions.IDocumentReaderPrepareCompletion
import com.regula.documentreader.api.enums.DocReaderAction
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.params.DocReaderConfig
import com.regula.documentreader.api.results.DocumentReaderResults
import com.regula.documentreader.databinding.ActivityMainBinding

import com.regula.documentreader.util.Utils
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), MainCallbacks {
    var sharedPreferences: SharedPreferences? = null
    private var doRfid = false
    private var loadingDialog: AlertDialog? = null
    private lateinit var mainFragment: MainFragment

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        val fragment = findFragmentByTag(TAG_UI_FRAGMENT)
        if (fragment == null) {
            mainFragment = MainFragment()
            replaceFragment(mainFragment, false)
        } else {
            mainFragment = fragment as MainFragment
        }
        sharedPreferences = getSharedPreferences(MY_SHARED_PREFS, MODE_PRIVATE)

        if (DocumentReader.Instance().isReady) {
            successfulInit()
            return
        }

        showDialog("Preparing database")

        //preparing database files, it will be downloaded from network only one time and stored on user device
        DocumentReader.Instance().prepareDatabase(
            this@MainActivity,
            "Full",
            object : IDocumentReaderPrepareCompletion {
                override fun onPrepareProgressChanged(progress: Int) {
                    setTitleDialog("Downloading database: $progress%")
                }

                override fun onPrepareCompleted(
                    status: Boolean,
                    error: DocumentReaderException?
                ) {
                    if (status) {
                        onPrepareDbCompleted()
                    } else {
                        dismissDialog()
                        Toast.makeText(
                            this@MainActivity,
                            "Prepare DB failed:$error",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            })
    }

    private fun initializeReader() {
        val license = Utils.getLicense(this) ?: return

        showDialog("Initializing")
        val config = DocReaderConfig(license)
        config.setLicenseUpdate(true)

        //Initializing the reader
        DocumentReader.Instance().initializeReader(this@MainActivity, config, initCompletion)
    }

    fun onPrepareDbCompleted() {
        initializeReader()
    }

    override fun recognizePdf() {
        if (!DocumentReader.Instance().isReady) return
        showDialog("Processing pdf")
        Executors.newSingleThreadExecutor().execute {
            val `is`: InputStream
            var buffer: ByteArray? = null
            try {
                `is` = assets.open("Regula/test.pdf")
                val size = `is`.available()
                buffer = ByteArray(size)
                `is`.read(buffer)
                `is`.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            val finalBuffer = buffer
            runOnUiThread {
                DocumentReader.Instance().recognizeImage(finalBuffer!!, completion)
            }
        }
    }

    override fun scenarioLv(item: String?) {
        if (!DocumentReader.Instance().isReady) return

        //setting selected scenario to DocumentReader params
        DocumentReader.Instance().processParams().scenario = item!!
    }

    override fun showScanner() {
        if (!DocumentReader.Instance().isReady) return
        DocumentReader.Instance().showScanner(this@MainActivity, completion)
    }

    override fun recognizeImage() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
            )
        } else {
            //start image browsing
            createImageBrowsingRequest()
        }
    }

    override fun setDoRFID(checked: Boolean) {
        doRfid = checked
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RFID_RESULT) {
            if (documentReaderResults != null)
                mainFragment.displayResults(documentReaderResults)
        }

        //Image browsing intent processed successfully
        if (resultCode != RESULT_OK || requestCode != REQUEST_BROWSE_PICTURE || data!!.data == null) return

        val selectedImage = data.data
        val bmp: Bitmap? = Utils.getBitmap(contentResolver, selectedImage, 1920, 1080)
        showDialog("Processing image")
        DocumentReader.Instance().recognizeImage(bmp!!, completion)
    }

    protected val initCompletion =
        IDocumentReaderInitCompletion { result: Boolean, error: DocumentReaderException? ->
            dismissDialog()
            if (!result) { //Initialization was not successful
                mainFragment?.disableUiElements()
                Toast.makeText(this@MainActivity, "Init failed:$error", Toast.LENGTH_LONG).show()
                return@IDocumentReaderInitCompletion
            }
            successfulInit()
        }

    protected fun successfulInit() {
        setupCustomization()
        setupFunctionality()
        setupProcessParams()
        mainFragment.setDoRfid(
            DocumentReader.Instance().isRFIDAvailableForUse,
            sharedPreferences!!
        )
        //getting current processing scenario and loading available scenarios to ListView
        if (DocumentReader.Instance().availableScenarios.isNotEmpty())
            setScenarios()
        else {
            mainFragment.disableUiElements()
            Toast.makeText(
                this@MainActivity,
                "Available scenarios list is empty",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupCustomization() {
        DocumentReader.Instance().customization().edit().setShowHelpAnimation(false).apply()
    }

    private fun setupFunctionality() {
        DocumentReader.Instance().functionality().edit()
            .setShowCameraSwitchButton(true)
            .apply()
    }

    private fun setupProcessParams() {
        DocumentReader.Instance().processParams().multipageProcessing = true
    }

    @SuppressLint("MissingPermission")
    private val completion =
        IDocumentReaderCompletion { action, results, error ->
            //processing is finished, all results are ready
            if (action == DocReaderAction.COMPLETE || action == DocReaderAction.TIMEOUT) {
                dismissDialog()

                //Checking, if nfc chip reading should be performed
                if (doRfid && results != null && results.chipPage != 0) {
                    //starting chip reading
                    DocumentReader.Instance().startRFIDReader(
                        this@MainActivity
                    ) { rfidAction, results, _ ->
                        if (rfidAction == DocReaderAction.COMPLETE || rfidAction == DocReaderAction.CANCEL) {
                            mainFragment.displayResults(results)
                        }
                    }
                } else {
                    mainFragment.displayResults(results)
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.size > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    //access to gallery is allowed
                    createImageBrowsingRequest()
                } else {
                    Toast.makeText(this, "Permission required, to browse images", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
    }

    // creates and starts image browsing intent
    // results will be handled in onActivityResult method
    private fun createImageBrowsingRequest() {
        val intent = Intent()
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(
            Intent.createChooser(intent, "Select Picture"),
            REQUEST_BROWSE_PICTURE
        )
    }

    override fun onPause() {
        super.onPause()
        if (loadingDialog != null) {
            loadingDialog!!.dismiss()
            loadingDialog = null
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) supportFragmentManager.popBackStack() else super.onBackPressed()
    }

    protected fun setTitleDialog(msg: String?) {
        if (loadingDialog != null) {
            loadingDialog!!.setTitle(msg)
        } else {
            showDialog(msg)
        }
    }

    protected fun dismissDialog() {
        if (loadingDialog != null) {
            loadingDialog!!.dismiss()
        }
    }

    protected fun showDialog(msg: String?) {
        dismissDialog()
        val builderDialog = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.simple_dialog, null)
        builderDialog.setTitle(msg)
        builderDialog.setView(dialogView)
        builderDialog.setCancelable(false)
        loadingDialog = builderDialog.show()
    }

    private fun findFragmentByTag(tag: String?): Fragment? {
        val fm = supportFragmentManager
        return fm.findFragmentByTag(tag)
    }

    private fun replaceFragment(fragment: Fragment, addFragmentInBackstack: Boolean) {
        val backStateName = fragment.javaClass.name
        val manager = supportFragmentManager
        val fragmentPopped = manager.popBackStackImmediate(backStateName, 0)
        if (!fragmentPopped && manager.findFragmentByTag(backStateName) == null) {
            val ft = manager.beginTransaction()
            ft.replace(R.id.fragmentContainer, fragment, backStateName)
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            if (addFragmentInBackstack) ft.addToBackStack(backStateName)
            ft.commit()
        }
    }

    fun setScenarios() {
        val scenarios = ArrayList<String>()
        for (scenario in DocumentReader.Instance().availableScenarios) {
            scenarios.add(scenario.name)
        }

        if (scenarios.isNotEmpty()) {
            //setting default scenario
            if (DocumentReader.Instance().processParams().scenario.isEmpty())
                DocumentReader.Instance().processParams().scenario = scenarios[0]

            val scenarioPosition: Int =
                getScenarioPosition(scenarios, DocumentReader.Instance().processParams().scenario)
            scenarioLv(DocumentReader.Instance().processParams().scenario)
            val adapter =
                ScenarioAdapter(this@MainActivity, android.R.layout.simple_list_item_1, scenarios)
            adapter.setSelectedPosition(scenarioPosition)
            mainFragment.setAdapter(adapter)
        }
    }

    private fun getScenarioPosition(scenarios: List<String>, currentScenario: String): Int {
        var selectedPosition = 0
        for (i in scenarios.indices) {
            if (scenarios[i] == currentScenario) {
                selectedPosition = i
                break
            }
        }
        return selectedPosition
    }

    companion object {
        const val REQUEST_BROWSE_PICTURE = 11
        const val PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 22
        private const val MY_SHARED_PREFS = "MySharedPrefs"
        const val DO_RFID = "doRfid"
        var documentReaderResults: DocumentReaderResults? = null
        private const val TAG_UI_FRAGMENT = "ui_fragment"
        private const val TAG_SETTINGS_FRAGMENT = "settings_fragment"
    }
}
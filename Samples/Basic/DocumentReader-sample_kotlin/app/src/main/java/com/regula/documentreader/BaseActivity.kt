package com.regula.documentreader

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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
import com.regula.documentreader.api.completions.*
import com.regula.documentreader.api.enums.DocReaderAction
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.params.rfid.PKDCertificate
import com.regula.documentreader.api.params.rfid.authorization.PAResourcesIssuer
import com.regula.documentreader.api.params.rfid.authorization.TAChallenge
import com.regula.documentreader.api.results.DocumentReaderResults
import com.regula.documentreader.custom.*
import com.regula.documentreader.custom.SettingsFragment.RfidMode.CUSTOM
import com.regula.documentreader.custom.SettingsFragment.RfidMode.DEFAULT
import com.regula.documentreader.util.CertificatesUtil
import com.regula.documentreader.util.Utils

import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.Executors

abstract class BaseActivity : AppCompatActivity(), MainCallbacks {
    var sharedPreferences: SharedPreferences? = null
    private var doRfid = false
    private var loadingDialog: AlertDialog? = null
    protected var mainFragment: MainFragment? = null
    protected var fragmentContainer: FrameLayout? = null
    protected var settingsFragment: SettingsFragment? = null
    var activeCameraMode = 0
    var rfidMode = 0

    protected abstract fun initializeReader()
    protected abstract fun onPrepareDbCompleted()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fragmentContainer = findViewById(R.id.fragmentContainer)
        mainFragment = findFragmentByTag(TAG_UI_FRAGMENT) as MainFragment?
        if (mainFragment == null) {
            mainFragment = MainFragment()
            replaceFragment(mainFragment!!, false)

        }
        sharedPreferences = getSharedPreferences(MY_SHARED_PREFS, MODE_PRIVATE)

        if (DocumentReader.Instance().isReady) {
            successfulInit()
            return
        }

        showDialog("Preparing database")

        //preparing database files, it will be downloaded from network only one time and stored on user device
        DocumentReader.Instance().prepareDatabase(
            this@BaseActivity,
            "FullAuth",
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
                            this@BaseActivity,
                            "Prepare DB failed:$error",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            })
    }

    override fun showCameraActivity() {
        if (!DocumentReader.Instance().isReady) return
        val cameraIntent = Intent()
        cameraIntent.setClass(this@BaseActivity, CameraActivity::class.java)
        startActivity(cameraIntent)
    }

    override fun showCustomCameraActivity() {
        if (!DocumentReader.Instance().isReady) return
        val cameraIntent = Intent()
        cameraIntent.setClass(this@BaseActivity, CustomRegActivity::class.java)
        startActivity(cameraIntent)
    }

    override fun showCustomCamera2Activity() {
        if (!DocumentReader.Instance().isReady) return
        val cameraIntent = Intent()
        cameraIntent.setClass(this@BaseActivity, Camera2Activity::class.java)
        startActivity(cameraIntent)
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
        DocumentReader.Instance().showScanner(this@BaseActivity, completion)
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
            if (documentReaderResults != null && rfidMode == CUSTOM)
                mainFragment!!.displayResults(documentReaderResults);
        }

        //Image browsing intent processed successfully
        if (resultCode != RESULT_OK || requestCode != REQUEST_BROWSE_PICTURE || data!!.data == null) return

        val selectedImage = data.data
        val bmp: Bitmap? = Utils.getBitmap(contentResolver, selectedImage, 1920, 1080)
        showDialog("Processing image")
        DocumentReader.Instance().recognizeImage(bmp!!, completion)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_actions, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_settings) {
            settingsFragment =
                findFragmentByTag(BaseActivity.TAG_SETTINGS_FRAGMENT) as SettingsFragment?
            if (settingsFragment == null) {
                settingsFragment = SettingsFragment()
                replaceFragment(settingsFragment!!, true)
            }
        }
        return false
    }

    protected val initCompletion =
        IDocumentReaderInitCompletion { result: Boolean, error: DocumentReaderException? ->
            dismissDialog()
            if (!result) { //Initialization was not successful
                Toast.makeText(this@BaseActivity, "Init failed:$error", Toast.LENGTH_LONG).show()
                return@IDocumentReaderInitCompletion
            }
            successfulInit()
        }

    protected fun successfulInit() {
        setupCustomization()
        setupFunctionality()
        mainFragment!!.setDoRfid(
            DocumentReader.Instance().isRFIDAvailableForUse,
            sharedPreferences!!
        )
        //mainFragment.setupUseCustomCamera();
        //getting current processing scenario and loading available scenarios to ListView
        setScenarios()
    }

    private fun setupCustomization() {
        DocumentReader.Instance().customization().edit().setShowHelpAnimation(false).apply()
    }

    private fun setupFunctionality() {
        DocumentReader.Instance().functionality().edit()
            .setUseAuthenticator(true)
            .setShowCameraSwitchButton(true)
            .apply()
    }

    private val completion =
        IDocumentReaderCompletion { action, results, error ->
            //processing is finished, all results are ready
            if (action == DocReaderAction.COMPLETE) {
                dismissDialog()

                //Checking, if nfc chip reading should be performed
                if (doRfid && results != null && results.chipPage != 0) {
                    //starting chip reading
                    when (rfidMode) {
                        CUSTOM -> {
                            val rfidIntent =
                                Intent(this@BaseActivity, CustomRfidActivity::class.java)
                            startActivityForResult(rfidIntent, RFID_RESULT)
                        }
                        DEFAULT -> {
                            DocumentReader.Instance().startRFIDReader(
                                this@BaseActivity,
                            { rfidAction, results, error ->
                                if (rfidAction == DocReaderAction.COMPLETE || rfidAction == DocReaderAction.CANCEL) {
                                    mainFragment!!.displayResults(results)
                                }
                            }, object : IRfidReaderRequest {
                                override fun onRequestPACertificates(
                                    bytes: ByteArray?,
                                    paResourcesIssuer: PAResourcesIssuer?,
                                    completion: IRfidPKDCertificateCompletion
                                ) {
                                    completion.onCertificatesReceived(CertificatesUtil.getRfidCertificates(assets, "Regula/certificates")?.toTypedArray())
                                }

                                override fun onRequestTACertificates(
                                    s: String?,
                                    completion: IRfidPKDCertificateCompletion
                                ) {
                                    completion.onCertificatesReceived(CertificatesUtil.getRfidTACertificates(assets)?.toTypedArray());
                                }

                                override fun onRequestTASignature(
                                    taChallenge: TAChallenge?,
                                    completion: IRfidTASignatureCompletion
                                ) {
                                    completion.onSignatureReceived(null)
                                }
                            })
                        }
                    }
                    addRfidCertificates()
                } else {
                    mainFragment!!.displayResults(results)
                }
            } else {
                //something happened before all results were ready
                if (action == DocReaderAction.CANCEL) {
                    Toast.makeText(this@BaseActivity, "Scanning was cancelled", Toast.LENGTH_LONG)
                        .show()
                } else if (action == DocReaderAction.ERROR) {
                    Toast.makeText(this@BaseActivity, "Error:$error", Toast.LENGTH_LONG).show()
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

    fun findFragmentByTag(tag: String?): Fragment? {
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

    open fun setScenarios() {
        val scenarios = java.util.ArrayList<String>()
        for (scenario in DocumentReader.Instance().availableScenarios) {
            scenarios.add(scenario.name)
        }

        //setting default scenario
        DocumentReader.Instance().processParams().scenario = scenarios[0]
        val adapter =
            ScenarioAdapter(this@BaseActivity, android.R.layout.simple_list_item_1, scenarios)
        mainFragment!!.setAdapter(adapter)
    }

    companion object {
        const val REQUEST_BROWSE_PICTURE = 11
        const val PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 22
        private const val MY_SHARED_PREFS = "MySharedPrefs"
        const val DO_RFID = "doRfid"
        var documentReaderResults //todo move out this
                : DocumentReaderResults? = null
        private const val TAG_UI_FRAGMENT = "ui_fragment"
        private const val TAG_SETTINGS_FRAGMENT = "settings_fragment"
    }

    fun addRfidCertificates() {
        val certificates: MutableList<PKDCertificate> = ArrayList()
        certificates.addAll(CertificatesUtil.getRfidCertificates(assets, "Regula/certificates"))
        if (certificates.size > 0) {
            DocumentReader.Instance().addPKDCertificates(certificates)
        }
    }
}
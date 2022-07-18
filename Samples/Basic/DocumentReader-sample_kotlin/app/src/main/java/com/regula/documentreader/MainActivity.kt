package com.regula.documentreader

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.params.DocReaderConfig
import com.regula.documentreader.util.LicenseUtil


class MainActivity : BaseActivity() {
    private val PERMISSIONS_REQUEST_READ_PHONE_STATE = 33

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (LicenseUtil.readFileFromAssets(
                "Regula",
                "regula.license",
                this
            ) == null
        ) showLicenseMissedDialog(
            this@MainActivity
        )
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@MainActivity, arrayOf(Manifest.permission.READ_PHONE_STATE),
                PERMISSIONS_REQUEST_READ_PHONE_STATE
            )
        }
    }

     override fun initializeReader() {
        showDialog("Initializing")


        val license = LicenseUtil.readFileFromAssets(
            "Regula",
            "regula.license",
            this
        )
        val config = DocReaderConfig(license)
        config.isLicenseUpdate = true

        //Initializing the reader
        DocumentReader.Instance().initializeReader(this@MainActivity, config, initCompletion)
    }

    override fun onPrepareDbCompleted() {
        initializeReader()
        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            initializeReader();
        } else {
            ActivityCompat.requestPermissions(this@MainActivity,
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                PERMISSIONS_REQUEST_READ_PHONE_STATE);
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_READ_PHONE_STATE) {
            if (grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                initializeReader();
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Permission is required to init Document Reader",
                    Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    private fun showLicenseMissedDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Error")
            .setMessage("license in assets is missed")
            .setPositiveButton(
                getString(R.string.strAccessibilityCloseButton)
            ) { dialog, which ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
}
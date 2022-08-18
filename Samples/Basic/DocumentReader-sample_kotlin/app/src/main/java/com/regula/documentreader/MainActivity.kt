package com.regula.documentreader

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.params.DocReaderConfig
import com.regula.documentreader.util.LicenseUtil


class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (LicenseUtil.getLicense(this) == null
        ) showLicenseMissedDialog(this@MainActivity)
    }

    override fun initializeReader() {
        val license = LicenseUtil.getLicense(this) ?: return

        showDialog("Initializing")
        val config = DocReaderConfig(license)
        config.isLicenseUpdate = true

        //Initializing the reader
        DocumentReader.Instance().initializeReader(this@MainActivity, config, initCompletion)

    }

    override fun onPrepareDbCompleted() {
        initializeReader()
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
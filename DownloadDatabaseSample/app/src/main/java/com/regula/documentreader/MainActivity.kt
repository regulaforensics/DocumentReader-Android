package com.regula.documentreader

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.databinding.MainLayoutBinding
import com.regula.documentreader.util.Utils

open class MainActivity : AppCompatActivity() {

    private val binding: MainLayoutBinding by lazy { MainLayoutBinding.inflate(layoutInflater) }
    private val viewModel: MainViewModel by lazy {
        ViewModelProvider(this, MainViewModelFactory(DocumentReader.Instance())).get(MainViewModel::class.java)
    }

    private var progressDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val view = binding.root
        setContentView(view)

        binding.prepareDb.setOnClickListener {
            progressDialog = showDialog("Prepare database") { _, _ ->
                progressDialog?.dismiss()
                viewModel.cancelDbUpdate(this@MainActivity)
            }
            progressDialog?.show()
            viewModel.prepareDatabase(applicationContext)
        }

        binding.removeDb.setOnClickListener {
            viewModel.removeDatabase(applicationContext)
        }

        binding.updateDb.setOnClickListener {
            viewModel.updateDatabase(applicationContext)
        }

        binding.autoUpdateDb.setOnClickListener {
            progressDialog = showDialog("Auto update database") { _, _ ->
                progressDialog?.dismiss()
                viewModel.cancelDbUpdate(this@MainActivity)
            }
            progressDialog?.show()
            viewModel.runAutoUpdate(applicationContext)
        }

        binding.initialize.setOnClickListener {
            progressDialog = showDialog("Initialization")
            progressDialog?.show()
            viewModel.initialize(applicationContext)
        }

        binding.deinitialize.setOnClickListener {
            viewModel.deinitialize()
        }

        try {
            Utils.getLicense(this)
        } catch (e: Exception) {
            binding.initialize.visibility = View.GONE
            binding.deinitialize.visibility = View.GONE
        }

        observeData()
    }

    private fun observeData() {
        viewModel.lockUI.observe(this) { isEnabled ->
            binding.prepareDb.isEnabled = isEnabled
            binding.removeDb.isEnabled = isEnabled
            binding.updateDb.isEnabled = isEnabled
            binding.autoUpdateDb.isEnabled = isEnabled
            binding.initialize.isEnabled = isEnabled
            binding.deinitialize.isEnabled = isEnabled
        }

        viewModel.progressData.observe(this) {
            if (progressDialog == null) {
                progressDialog = showDialog("Downloading database: $it") { _, _ ->
                    progressDialog?.dismiss()
                    viewModel.cancelDbUpdate(this@MainActivity)
                }
                return@observe
            }
            progressDialog?.setTitle("Downloading database: $it")
        }

        viewModel.prepareSuccess.observe(this)  {
            progressDialog?.dismiss()
            Toast.makeText(applicationContext, "Download database completed", Toast.LENGTH_LONG).show()
        }

        viewModel.prepareFailed.observe(this) {
            progressDialog?.dismiss()
            Toast.makeText(applicationContext, "Download database failed with error: $it", Toast.LENGTH_LONG).show()
            Log.e("MainActivity", "DB download error: $it")
        }

        viewModel.dbInfo.observe(this) {
            binding.dbInfo.text = it
        }

        viewModel.initState.observe(this) {
            progressDialog?.dismiss()
            Toast.makeText(applicationContext, it, Toast.LENGTH_LONG).show()
        }
    }

    private fun showDialog(msg: String, clickListener: DialogInterface.OnClickListener? = null): AlertDialog {
        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(msg)
            .setView(layoutInflater.inflate(R.layout.simple_dialog, binding.root, false))
            .setCancelable(false)

        clickListener?.let {
            builder.setPositiveButton("Cancel", clickListener)
        }
        return builder.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        progressDialog?.dismiss()

        viewModel.reset()
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

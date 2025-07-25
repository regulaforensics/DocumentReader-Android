package com.regula.documentreader

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.regula.documentreader.Helpers.Companion.getStatusImage
import com.regula.documentreader.api.results.DocumentReaderResults
import com.regula.documentreader.api.results.authenticity.DocumentReaderAuthenticityCheck
import com.regula.documentreader.api.results.authenticity.DocumentReaderAuthenticityResult
import com.regula.documentreader.databinding.ActivityResultsAuthBinding

class ResultsAuthActivity : AppCompatActivity() {

    private val binding: ActivityResultsAuthBinding by lazy {
        ActivityResultsAuthBinding.inflate(
            layoutInflater
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val view = binding.root
        setContentView(view)

        binding.btnBackToHomePage.setOnClickListener {
            finishResultActivity()
        }

        if (results.authenticityResult?.status != null) {
            binding.ivOverallStatus.setImageResource(getStatusImage(results.authenticityResult?.status!!))
        }

        initAuthData()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        finishResultActivity()
    }

    private fun finishResultActivity() {
        finish()
    }

    private fun initAuthData() {

        setupUi(results.authenticityResult)
    }

    private fun setupUi(authResults: DocumentReaderAuthenticityResult?) {

        if (authResults == null) {
            return
        }

        val pickerSize = authResults.checks.size

        binding.checksPicker.maxValue = pickerSize - 1
        binding.checksPicker.wrapSelectorWheel = false

        val checkNameList = mutableListOf<String>()

        for (check in authResults.checks) {
            checkNameList.add(check.getTypeName(this))
        }

        setupNewCheck(
            authResults.checks[binding.checksPicker.value],
            checkNameList[binding.checksPicker.value]
        )

        binding.checksPicker.displayedValues = checkNameList.toTypedArray()
        binding.checksPicker.setOnValueChangedListener { _, _, newVal ->
            binding.checksPicker.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            setupNewCheck(authResults.checks[newVal], checkNameList[newVal])
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupNewCheck(check: DocumentReaderAuthenticityCheck, checkName: String) {

        binding.tvCheckName.text = checkName
        binding.ivCheckStatus.setImageResource(getStatusImage(check.status))
        binding.tvCheckPageIndex.text = "page [${check.pageIndex}]"

        binding.rvItems.layoutManager = LinearLayoutManager(this)
        binding.rvItems.adapter = AuthItemsAdapter(this, check.elements)
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

    companion object {
        lateinit var results: DocumentReaderResults
    }
}
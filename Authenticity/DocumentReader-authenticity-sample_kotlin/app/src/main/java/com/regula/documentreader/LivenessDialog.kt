package com.regula.documentreader

import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.regula.documentreader.Helpers.Companion.showBottomValuesDialog
import com.regula.documentreader.api.params.LivenessParams
import com.regula.documentreader.databinding.DialogLivenessBinding

class LivenessDialog(
    context: Context,
    actualLivenessParams: LivenessParams?,
    actualLivenessCheckState: Boolean?
) {

    private var _binding: DialogLivenessBinding? = null
    private val binding get() = _binding!!

    private var listener: DialogListener

    private val dialog: Dialog = Dialog(context).apply {
        _binding = DialogLivenessBinding.inflate(this.layoutInflater)
        window?.setBackgroundDrawableResource(R.drawable.rounded)
        setContentView(binding.root)
        setCancelable(true)
    }

    private val textButtonValues: Array<TextView> = arrayOf(
        binding.tvUseLivenessValue,
        binding.tvCheckHoloValue,
        binding.tvCheckEdValue,
        binding.tvCheckMliValue,
        binding.tvCheckOviValue,
        binding.tvCheckDynaprintValue,
        binding.tvCheckBlackAndWhiteCopyValue,
        binding.tvCheckGeometryValue
    )

    init {
        setTextButtonsValues(actualLivenessParams, actualLivenessCheckState)
        setLivenessParamsAviability(actualLivenessCheckState)

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        binding.btnReset.setOnClickListener {
            resetChecks(context)
            dismiss()
        }
        binding.btnSave.setOnClickListener {
            saveStates()
            dismiss()
        }

        binding.tvUseLivenessValue.setOnClickListener {
            showBottomValuesDialog(
                binding.tvUseLiveness.text.toString(),
                binding.tvUseLivenessValue.text.toString(),
                R.string.use_liveness,
                context
            )
        }

        binding.tvCheckHoloValue.setOnClickListener {
            showBottomValuesDialog(
                binding.tvCheckHolo.text.toString(),
                binding.tvCheckHoloValue.text.toString(),
                R.string.check_holo,
                context
            )
        }
        binding.tvCheckEdValue.setOnClickListener {
            showBottomValuesDialog(
                binding.tvCheckED.text.toString(),
                binding.tvCheckEdValue.text.toString(),
                R.string.check_ed,
                context
            )
        }
        binding.tvCheckMliValue.setOnClickListener {
            showBottomValuesDialog(
                binding.tvCheckMLI.text.toString(),
                binding.tvCheckMliValue.text.toString(),
                R.string.check_mli,
                context
            )
        }
        binding.tvCheckOviValue.setOnClickListener {
            showBottomValuesDialog(
                binding.tvCheckOVI.text.toString(),
                binding.tvCheckOviValue.text.toString(),
                R.string.check_ovi,
                context
            )
        }
        binding.tvCheckDynaprintValue.setOnClickListener {
            showBottomValuesDialog(
                binding.tvCheckDynaprint.text.toString(),
                binding.tvCheckDynaprintValue.text.toString(),
                R.string.check_dynaprint,
                context
            )
        }
        binding.tvCheckBlackAndWhiteCopyValue.setOnClickListener {
            showBottomValuesDialog(
                binding.tvBlackAndWhiteCopy.text.toString(),
                binding.tvCheckBlackAndWhiteCopyValue.text.toString(),
                R.string.check_black_and_white_copy,
                context
            )
        }
        binding.tvCheckGeometryValue.setOnClickListener {
            showBottomValuesDialog(
                binding.tvCheckGeometry.text.toString(),
                binding.tvCheckGeometryValue.text.toString(),
                R.string.check_geometry,
                context
            )
        }

        setDialogWidth(0.9f)
        listener = context as DialogListener
    }

    private fun setTextButtonsValues(
        actualLivenessParams: LivenessParams?,
        actualLivenessCheckState: Boolean?
    ) {

        for (textBtn in textButtonValues) {
            when (textBtn.id) {
                R.id.tvUseLivenessValue -> textBtn.text =
                    getNameByValue(actualLivenessCheckState)

                R.id.tvCheckHoloValue -> textBtn.text =
                    getNameByValue(actualLivenessParams?.checkHolo)

                R.id.tvCheckEdValue -> textBtn.text =
                    getNameByValue(actualLivenessParams?.checkED)

                R.id.tvCheckMliValue -> textBtn.text =
                    getNameByValue(actualLivenessParams?.checkMLI)

                R.id.tvCheckOviValue -> textBtn.text =
                    getNameByValue(actualLivenessParams?.checkOVI)

                R.id.tvCheckDynaprintValue -> textBtn.text =
                    getNameByValue(actualLivenessParams?.checkDynaprint)

                R.id.tvCheckBlackAndWhiteCopyValue -> textBtn.text =
                    getNameByValue(actualLivenessParams?.checkBlackAndWhiteCopy)

                R.id.tvCheckGeometryValue -> textBtn.text =
                    getNameByValue(actualLivenessParams?.checkGeometry)
            }
        }
    }

    fun setLivenessParamsAviability(actualLivenessCheckState: Boolean?) {
        if (actualLivenessCheckState == true) {
            binding.overlay.visibility = View.INVISIBLE
            binding.tvCheckHoloValue.visibility = View.VISIBLE
            binding.tvCheckEdValue.visibility = View.VISIBLE
            binding.tvCheckMliValue.visibility = View.VISIBLE
            binding.tvCheckOviValue.visibility = View.VISIBLE
            binding.tvCheckDynaprintValue.visibility = View.VISIBLE
            binding.tvCheckBlackAndWhiteCopyValue.visibility = View.VISIBLE
            binding.tvCheckGeometryValue.visibility = View.VISIBLE
        } else {
            binding.overlay.visibility = View.VISIBLE
            binding.tvCheckHoloValue.visibility = View.INVISIBLE
            binding.tvCheckEdValue.visibility = View.INVISIBLE
            binding.tvCheckMliValue.visibility = View.INVISIBLE
            binding.tvCheckOviValue.visibility = View.INVISIBLE
            binding.tvCheckDynaprintValue.visibility = View.INVISIBLE
            binding.tvCheckBlackAndWhiteCopyValue.visibility = View.INVISIBLE
            binding.tvCheckGeometryValue.visibility = View.INVISIBLE
        }
    }

    private fun getNameByValue(value: Boolean?): String {
        return when (value) {
            true -> "activated"
            false -> "deactivated"
            null -> "default"
        }
    }

    private fun getValueByName(name: String): Boolean? {
        return when (name) {
            "activated" -> true
            "deactivated" -> false
            "default" -> null
            else -> null
        }
    }

    fun updateLivenessState(text: String) {
        binding.tvUseLivenessValue.text = text
    }

    fun updateHoloState(text: String) {
        binding.tvCheckHoloValue.text = text
    }

    fun updateEdState(text: String) {
        binding.tvCheckEdValue.text = text
    }

    fun updateMliState(text: String) {
        binding.tvCheckMliValue.text = text
    }

    fun updateOviState(text: String) {
        binding.tvCheckOviValue.text = text
    }

    fun updateDynaprintState(text: String) {
        binding.tvCheckDynaprintValue.text = text
    }

    fun updateBlackAndWhiteCopyState(text: String) {
        binding.tvCheckBlackAndWhiteCopyValue.text = text
    }

    fun updateCheckGeometryState(text: String) {
        binding.tvCheckGeometryValue.text = text
    }

    interface DialogListener {
        fun onLivenessCheckValue(checkNameId: Int, checkValue: Boolean?)
    }

    private fun resetChecks(context: Context) {

        val listener = context as SetValueDialog.DialogListener
        listener.onValueSubmit("default", checkId = R.string.use_liveness)
        listener.onValueSubmit("default", checkId = R.string.check_holo)
        listener.onValueSubmit("default", checkId = R.string.check_ed)
        listener.onValueSubmit("default", checkId = R.string.check_mli)
        listener.onValueSubmit("default", checkId = R.string.check_ovi)
        listener.onValueSubmit("default", checkId = R.string.check_dynaprint)
        listener.onValueSubmit("default", checkId = R.string.check_black_and_white_copy)
    }

    private fun saveStates() {
        for (textBtn in textButtonValues) {
            when (textBtn.id) {
                R.id.tvUseLivenessValue -> {
                    listener.onLivenessCheckValue(
                        R.string.use_liveness,
                        getValueByName(textBtn.text.toString())
                    )
                }

                R.id.tvCheckHoloValue -> {
                    listener.onLivenessCheckValue(
                        R.string.check_holo,
                        getValueByName(textBtn.text.toString())
                    )
                }

                R.id.tvCheckEdValue -> {
                    listener.onLivenessCheckValue(
                        R.string.check_ed,
                        getValueByName(textBtn.text.toString())
                    )
                }

                R.id.tvCheckMliValue -> {
                    listener.onLivenessCheckValue(
                        R.string.check_mli,
                        getValueByName(textBtn.text.toString())
                    )
                }

                R.id.tvCheckOviValue -> {
                    listener.onLivenessCheckValue(
                        R.string.check_ovi,
                        getValueByName(textBtn.text.toString())
                    )
                }

                R.id.tvCheckDynaprintValue -> {
                    listener.onLivenessCheckValue(
                        R.string.check_dynaprint,
                        getValueByName(textBtn.text.toString())
                    )
                }

                R.id.tvCheckBlackAndWhiteCopyValue -> {
                    listener.onLivenessCheckValue(
                        R.string.check_black_and_white_copy,
                        getValueByName(textBtn.text.toString())
                    )
                }
            }
        }
    }

    fun show() {
        dialog.show()
    }

    fun dismiss() {
        dialog.dismiss()
    }

    private fun setDialogWidth(proportion: Float) {
        val displayMetrics = dialog.context.resources.displayMetrics
        val width = (displayMetrics.widthPixels * proportion).toInt()

        val params: WindowManager.LayoutParams = dialog.window?.attributes ?: return
        params.width = width
        dialog.window?.attributes = params
    }
}
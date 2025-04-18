package com.regula.documentreader

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.regula.documentreader.databinding.DialogValueSetterBinding

@SuppressLint("SetTextI18n")
class SetValueDialog(context: Context, title: String, currentValue: String, checkId: Int) :
    BottomSheetDialogFragment() {

    private var _binding: DialogValueSetterBinding? = null
    private val binding get() = _binding!!
    private var listener: DialogListener

    interface DialogListener {
        fun onValueSubmit(text: String, value: Boolean? = null, checkId: Int)
    }

    private val dialog = Dialog(context).apply {
        _binding = DialogValueSetterBinding.inflate(this.layoutInflater)
        setContentView(binding.root)
        setCancelable(true)
    }

    init {
        setDialogParams()

        listener = context as DialogListener

        binding.tvCheckNameAndState.text = "$title - $currentValue"

        binding.tvActivate.setOnClickListener {
            listener.onValueSubmit("activated", true, checkId)
            dialog.dismiss()
        }
        binding.tvDeactivate.setOnClickListener {
            listener.onValueSubmit("deactivated", false, checkId)
            dialog.dismiss()
        }
        binding.tvDefault.setOnClickListener {
            listener.onValueSubmit("default", checkId = checkId)
            dialog.dismiss()
        }
        binding.tvCancel.setOnClickListener {
            dialog.dismiss()
        }
    }

    fun show() {
        dialog.show()
    }

    private fun setDialogParams() {
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setGravity(Gravity.BOTTOM)
    }
}
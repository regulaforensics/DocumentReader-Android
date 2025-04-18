package com.regula.documentreader

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import com.regula.documentreader.api.enums.eCheckResult

class Helpers {
    companion object {
        private var infoDialog: AlertDialog? = null

        private val links = mapOf(
            "Documents" to "https://docs.regulaforensics.com/home/faq/machine-readable-travel-documents",
            "Core" to "https://docs.regulaforensics.com/android/core",
            "Scenarios" to "https://docs.regulaforensics.com/android/scenarios",
            "Security" to "https://docs.regulaforensics.com/home/faq/security-mechanisms-for-electronic-documents",
            "Results" to "https://docs.regulaforensics.com/android/results/getting-results"
        )

        fun drawable(id: Int, context: Context): Drawable =
            ResourcesCompat.getDrawable(context.resources, id, context.theme)!!

        fun showBottomValuesDialog(
            checkName: String,
            checkCurrentValue: String,
            checkId: Int,
            context: Context
        ) {
            val dialog = SetValueDialog(
                context,
                checkName,
                checkCurrentValue,
                checkId
            )
            dialog.show()
        }

        fun showDialog(
            context: Context,
            title: String,
            cancellable: Boolean = false,
            dialogView: View? = null,
        ) {
            dismissDialog()

            val builderDialog = AlertDialog.Builder(context)
            builderDialog.setTitle(title)
            builderDialog.setCancelable(cancellable)
            if (cancellable) {
                builderDialog.setNegativeButton("Cancel") { dialog, _ ->
                    dismissDialog()
                }
            }
            if (dialogView != null) {
                builderDialog.setView(dialogView)
            }
            infoDialog = builderDialog.show()
        }

        fun dismissDialog() {
            infoDialog?.dismiss()
        }

        fun getStatusImage(status: Int): Int {
            return when (status) {
                eCheckResult.CH_CHECK_OK -> R.drawable.reg_icon_check_ok
                eCheckResult.CH_CHECK_WAS_NOT_DONE -> R.drawable.reg_icon_no_check
                else -> R.drawable.reg_icon_check_fail
            }
        }

        fun getCheckDescription(checkName: String, context: Context): String {
            return when (checkName) {
                context.resources.getString(R.string.liveness_params) -> context.resources.getString(
                    R.string.help_liveness
                )

                context.resources.getString(R.string.check_holo) -> context.resources.getString(R.string.help_holo)
                context.resources.getString(R.string.check_ed) -> context.resources.getString(R.string.help_ed)
                context.resources.getString(R.string.check_mli) -> context.resources.getString(R.string.help_mli)
                context.resources.getString(R.string.check_ovi) -> context.resources.getString(R.string.help_ovi)
                context.resources.getString(R.string.check_dynaprint) -> context.resources.getString(
                    R.string.help_dynaprint
                )

                context.resources.getString(R.string.check_black_and_white_copy) -> context.resources.getString(
                    R.string.help_black_and_white_copy
                )

                context.resources.getString(R.string.check_image_patterns) -> context.resources.getString(
                    R.string.help_check_image_patterns
                )

                context.resources.getString(R.string.check_photo_embedding) -> context.resources.getString(
                    R.string.help_check_photo_embedding
                )

                context.resources.getString(R.string.check_barcode_format) -> context.resources.getString(
                    R.string.help_check_barcode_format
                )

                context.resources.getString(R.string.check_photo_comparison) -> context.resources.getString(
                    R.string.help_check_photo_comparison
                )

                context.resources.getString(R.string.check_uv_luminiscence) -> context.resources.getString(
                    R.string.help_check_uv_luminiscence
                )

                context.resources.getString(R.string.check_fibers) -> context.resources.getString(R.string.help_check_fibers)
                context.resources.getString(R.string.check_ext_mrz) -> context.resources.getString(R.string.help_mrz)
                context.resources.getString(R.string.check_ext_ocr) -> context.resources.getString(R.string.help_check_ext_ocr)
                "checkIRB900" -> "The most common ink to use for the MRZ and personal data printing is B900. Infrared lighting reveals the security elements, demonstrating the high contrast level."
                context.resources.getString(R.string.check_ir_visibility) -> context.resources.getString(
                    R.string.help_check_ir_visibility
                )

                context.resources.getString(R.string.check_letter_screen) -> context.resources.getString(
                    R.string.help_check_letter_screen
                )

                context.resources.getString(R.string.check_axial) -> context.resources.getString(R.string.help_check_axial)
                context.resources.getString(R.string.check_ipi) -> context.resources.getString(R.string.help_check_ipi)
                context.resources.getString(R.string.authenticity_checks) -> context.resources.getString(
                    R.string.help_authenticity
                )

                else -> {
                    "Empty description"
                }
            }
        }
    }
}

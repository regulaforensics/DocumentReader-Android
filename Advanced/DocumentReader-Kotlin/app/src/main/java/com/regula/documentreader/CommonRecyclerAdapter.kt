package com.regula.documentreader

import android.content.Context
import android.text.InputType
import android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.regula.documentreader.Helpers.Companion.adaptImageSize
import com.regula.documentreader.Helpers.Companion.grayedOutAlpha
import com.regula.documentreader.Helpers.Companion.listToString
import com.regula.documentreader.Helpers.Companion.openLink
import com.regula.documentreader.Scan.Companion.ACTION_TYPE_CUSTOM
import com.regula.documentreader.Scan.Companion.ACTION_TYPE_GALLERY
import com.regula.documentreader.Scan.Companion.ACTION_TYPE_MANUAL_MULTIPAGE_MODE
import com.regula.documentreader.Scan.Companion.ACTION_TYPE_SCANNER
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.enums.eCheckResult
import com.regula.documentreader.databinding.*
import java.io.Serializable
import java.util.*


class CommonRecyclerAdapter(private val items: List<Base>) :
    RecyclerView.Adapter<CommonRecyclerAdapter.VH>() {
    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is Section -> SECTION
        is Scan -> BUTTON
        is Switch -> SWITCH
        is Stepper -> STEPPER
        is BS -> BOTTOM_SHEET
        is BSMulti -> BOTTOM_SHEET_MULTI
        is InputInt -> INPUT_INT
        is InputString -> INPUT_STRING
        is TextResult -> TEXT_RESULT
        is Image -> IMAGE
        is ImagePair -> IMAGE_PAIR
        is Status -> STATUS
        is InputDouble -> INPUT_DOUBLE
        is AuthSection -> AUTH_SECTION
        else -> 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val li = LayoutInflater.from(parent.context)
        return when (viewType) {
            SECTION -> VH.SectionVH(RvSectionBinding.inflate(li, parent, false))
            BUTTON -> VH.ScanVH(RvTextBinding.inflate(li, parent, false))
            SWITCH -> VH.SwitchVH(RvSwitchBinding.inflate(li, parent, false))
            STEPPER -> VH.StepperVH(RvStepperBinding.inflate(li, parent, false))
            BOTTOM_SHEET -> VH.BottomSheetVH(RvTitleValueBinding.inflate(li, parent, false))
            BOTTOM_SHEET_MULTI ->
                VH.BottomSheetMultiVH(RvTitleValueBinding.inflate(li, parent, false))

            INPUT_INT -> VH.InputIntVH(RvTitleValueBinding.inflate(li, parent, false))
            INPUT_STRING -> VH.InputStringVH(RvTitleValueBinding.inflate(li, parent, false))
            TEXT_RESULT -> VH.TextResultVH(RvTextResultBinding.inflate(li, parent, false))
            IMAGE -> VH.ImageVH(RvImageBinding.inflate(li, parent, false))
            IMAGE_PAIR -> VH.ImagePairVH(RvImagePairBinding.inflate(li, parent, false))
            STATUS -> VH.StatusVH(RvStatusBinding.inflate(li, parent, false))
            INPUT_DOUBLE -> VH.InputDoubleVH(RvTitleValueBinding.inflate(li, parent, false))
            AUTH_SECTION -> VH.AuthSectionVH(RvAuthSectionBinding.inflate(li, parent, false))
            else -> VH.SectionVH(RvSectionBinding.inflate(li, parent, false))
        }
    }

    override fun getItemCount() = items.size
    override fun onBindViewHolder(vh: VH, i: Int) = vh.bind(items[i])

    sealed class VH(v: View) : RecyclerView.ViewHolder(v) {
        val context: Context = v.context
        abstract fun bind(base: Base)

        class SectionVH(private val binding: RvSectionBinding) : VH(binding.root) {
            override fun bind(base: Base) {
                val section = base as Section
                binding.title.text = section.title
                if (section.helpTag == 0)
                    binding.help.visibility = INVISIBLE
                binding.help.setOnClickListener {
                    when (section.helpTag) {
                        10 -> showBottomSheetHelp(
                            context.getString(R.string.mrz),
                            context.getString(R.string.help_mrz),
                            R.drawable.info_mrz
                        )

                        20 -> showBottomSheetHelp(
                            context.getString(R.string.viz),
                            context.getString(R.string.help_viz),
                            R.drawable.info_viz
                        )

                        30 -> showBottomSheetHelp(
                            context.getString(R.string.barcode),
                            context.getString(R.string.help_barcode),
                            R.drawable.info_barcode
                        )

                        40 -> openLink(context, "Security")
                    }
                }
            }

            private fun showBottomSheetHelp(title: String, text: String, image: Int) =
                BottomSheet.newInstance(
                    title, arrayListOf(HelpBig(text, image)), true, "Close", true
                ) {}.show((context as FragmentActivity).supportFragmentManager, "")
        }

        class AuthSectionVH(private val binding: RvAuthSectionBinding) : VH(binding.root) {
            override fun bind(base: Base) {
                val section = base as AuthSection
                binding.title.text = section.title
                if (section.helpTag == 0)
                    binding.help.visibility = INVISIBLE

                when (section.checkStatus){
                    eCheckResult.CH_CHECK_ERROR ->  binding.completedStatusImage.setImageResource(R.drawable.reg_icon_check_fail)
                    eCheckResult.CH_CHECK_OK -> binding.completedStatusImage.setImageResource(R.drawable.reg_icon_check_ok)
                    eCheckResult.CH_CHECK_WAS_NOT_DONE -> binding.completedStatusImage.setImageResource(R.drawable.reg_icon_no_check)
                }
                binding.help.setOnClickListener {
                    when (section.helpTag) {
                        10 -> showBottomSheetHelp(
                            context.getString(R.string.mrz),
                            context.getString(R.string.help_mrz),
                            R.drawable.info_mrz
                        )

                        20 -> showBottomSheetHelp(
                            context.getString(R.string.viz),
                            context.getString(R.string.help_viz),
                            R.drawable.info_viz
                        )

                        30 -> showBottomSheetHelp(
                            context.getString(R.string.barcode),
                            context.getString(R.string.help_barcode),
                            R.drawable.info_barcode
                        )

                        40 -> openLink(context, "Security")
                    }
                }
            }

            private fun showBottomSheetHelp(title: String, text: String, image: Int) =
                BottomSheet.newInstance(
                    title, arrayListOf(HelpBig(text, image)), true, "Close", true
                ) {}.show((context as FragmentActivity).supportFragmentManager, "")
        }

        class ScanVH(private val binding: RvTextBinding) : VH(binding.root) {
            private lateinit var scan: Scan
            override fun bind(base: Base) {
                scan = base as Scan
                binding.title.text = base.title
                binding.root.setOnClickListener {

                    Helpers.resetCustomization()
                    if (scan.resetFunctionality)
                        Helpers.resetFunctionality()
                    scan.customize()

                    when (scan.actionType) {
                        ACTION_TYPE_SCANNER -> (context as MainActivity).showScanner()
                        ACTION_TYPE_GALLERY -> (context as MainActivity).createImageBrowsingRequest()
                        ACTION_TYPE_CUSTOM -> {
                        }

                        ACTION_TYPE_MANUAL_MULTIPAGE_MODE -> {
                            DocumentReader.Instance().startNewSession()
                            (context as MainActivity).showScanner()
                        }
                    }
                }
            }
        }

        class SwitchVH(private val binding: RvSwitchBinding) : VH(binding.root) {
            override fun bind(base: Base) {
                val switch = base as Switch
                binding.switcher.text = switch.title
                binding.switcher.setOnCheckedChangeListener(null)
                binding.switcher.isChecked = switch.get() ?: false
                binding.switcher.setOnCheckedChangeListener { _, isChecked -> switch.set(isChecked) }

                val enabled = switch.enabled() ?: false
                binding.switcher.isEnabled = enabled
                binding.alphaChanger.alpha = grayedOutAlpha(enabled)
            }
        }

        class StepperVH(private val binding: RvStepperBinding) : VH(binding.root) {
            override fun bind(base: Base) {
                val stepper = base as Stepper
                init(stepper)
                binding.minus.setOnClickListener {
                    if (stepper.get()!! - stepper.step < stepper.min)
                        stepper.set(stepper.min)
                    else
                        stepper.set(stepper.get()!! - stepper.step)
                    init(stepper)
                }
                binding.plus.setOnClickListener {
                    if (stepper.get()!! == -1 && stepper.addMinusOne)
                        stepper.set(stepper.get()!! + 1)
                    stepper.set(stepper.get()!! + stepper.step)
                    init(stepper)
                }
            }

            private fun init(stepper: Stepper) {
                if (stepper.get() == null)
                    stepper.set(-1)
                binding.title.text = stepper.title
                binding.units.text = context.resources.getString(
                    R.string.stepper_value,
                    stepper.get(),
                    stepper.valueUnits
                )
                val enabled = stepper.enabled() ?: false
                binding.plus.isEnabled = enabled
                binding.minus.isEnabled = enabled
                binding.alphaChanger.alpha = grayedOutAlpha(enabled)
                binding.minus.alpha = grayedOutAlpha(stepper.get()!! != stepper.min)
            }
        }

        class BottomSheetVH(@Transient private val binding: RvTitleValueBinding) :
            VH(binding.root), Serializable {
            private var selected: BSItem? = null
            override fun bind(base: Base) {
                val bs = base as BS
                init(bs)
                for (item in bs.items)
                    if (item.value == bs.get()) {
                        selected = item
                        item.selected = true
                    }
                binding.root.setOnClickListener(OnClickListenerSerializable {
                    BottomSheet.newInstance(bs.bsTitle, bs.items, true) {
                        bs.set(it.value)
                        init(bs)
                        if (selected != null)
                            selected?.selected = false
                        it.selected = true
                        selected = it
                    }.show((context as FragmentActivity).supportFragmentManager, "")
                })
            }

            private fun init(bs: BS) {
                binding.title.text = bs.title
                if (bs.get() == null || bs.get()!!.isEmpty())
                    binding.value.text = context.resources.getString(R.string.string_default)
                else
                    binding.value.text = bs.getFromMap()
            }
        }

        class BottomSheetMultiVH(@Transient private val binding: RvTitleValueBinding) :
            VH(binding.root), Serializable {
            override fun bind(base: Base) {
                val bs = base as BSMulti
                init(bs)
                for (item in bs.items)
                    if (bs.get().contains(item.value))
                        item.selected = true
                binding.root.setOnClickListener(OnClickListenerSerializable {
                    BottomSheet.newInstance(bs.bsTitle, bs.items, false, "Close") {
                        if (it.selected) {
                            val list = bs.get()
                            list.remove(it.value)
                            bs.set(list)
                        } else {
                            val list = bs.get()
                            list.add(it.value)
                            bs.set(list)
                        }
                        it.selected = !it.selected
                        init(bs)
                    }.show((context as FragmentActivity).supportFragmentManager, "")
                })
            }

            private fun init(bsMulti: BSMulti) {
                binding.title.text = bsMulti.title
                binding.value.text = listToString(bsMulti.getFromMap(), context)
            }
        }

        class InputIntVH(private val binding: RvTitleValueBinding) : VH(binding.root) {
            override fun bind(base: Base) {
                val input = base as InputInt
                init(input)
                binding.root.setOnClickListener {
                    val editText = EditText(context)
                    editText.inputType = InputType.TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_SIGNED
                    input.get()?.let { editText.setText(it.toString()) }
                    MaterialAlertDialogBuilder(
                        context,
                        R.style.AlertDialogTheme
                    ).setTitle(input.title).setView(editText)
                        .setPositiveButton("Done") { _, _ ->
                            if (editText.text.toString().isEmpty())
                                input.set(null)
                            else try {
                                input.set(editText.text.toString().toInt())
                            } catch (e: Exception) {
                            }
                            init(input)
                        }.setNegativeButton("Cancel", null).show()
                }
            }

            private fun init(inputInt: InputInt) {
                binding.title.text = inputInt.title
                binding.value.text = inputInt.get()?.toString() ?: ""
            }
        }

        class InputDoubleVH(private val binding: RvTitleValueBinding) : VH(binding.root) {
            override fun bind(base: Base) {
                val input = base as InputDouble
                init(input)
                binding.root.setOnClickListener {
                    val editText = EditText(context)
                    editText.inputType = InputType.TYPE_NUMBER_FLAG_DECIMAL
                    input.get()?.let { editText.setText(it.toString()) }
                    MaterialAlertDialogBuilder(
                        context,
                        R.style.AlertDialogTheme
                    ).setTitle(input.title).setView(editText)
                        .setPositiveButton("Done") { _, _ ->
                            if (editText.text.toString().isEmpty())
                                input.set(null)
                            else try {
                                input.set(editText.text.toString().toDouble())
                            } catch (e: Exception) {
                            }
                            init(input)
                        }.setNegativeButton("Cancel", null).show()
                }
            }

            private fun init(inputDouble: InputDouble) {
                binding.title.text = inputDouble.title
                binding.value.text = inputDouble.get()?.toString() ?: ""
            }
        }

        class InputStringVH(private val binding: RvTitleValueBinding) : VH(binding.root) {
            override fun bind(base: Base) {
                val input = base as InputString
                init(input)
                binding.root.setOnClickListener {
                    val editText = EditText(context)
                    editText.setText(input.get())
                    MaterialAlertDialogBuilder(
                        context,
                        R.style.AlertDialogTheme
                    ).setTitle(input.title).setView(editText)
                        .setPositiveButton("Done") { _, _ ->
                            input.set(editText.text.toString())
                            init(input)
                        }.setNegativeButton("Cancel", null).show()
                }
            }

            private fun init(inputString: InputString) {
                binding.title.text = inputString.title
                binding.value.text = inputString.get()
                if (inputString.get().isEmpty())
                    binding.value.text = context.resources.getString(R.string.string_default)
            }
        }

        class TextResultVH(private val binding: RvTextResultBinding) : VH(binding.root) {
            override fun bind(base: Base) {
                val textResult = base as TextResult
                binding.title.text = textResult.title.uppercase(Locale.ROOT)
                binding.value.text = textResult.value
                binding.value.setTextColor(textResult.color)
                binding.lcid.text = textResult.lcid
                binding.pageIndex.text = context.getString(R.string.pageIndex, textResult.pageIndex)
            }
        }

        class ImagePairVH(private val binding: RvImagePairBinding) : VH(binding.root) {
            override fun bind(base: Base) {
                val image = base as ImagePair
                binding.title.text = "#$layoutPosition " + image.title

                if (image.error?.isEmpty() == true) {
                    binding.titleError.visibility = View.GONE
                } else {
                    binding.titleError.visibility = View.VISIBLE
                }
                binding.titleError.text = image.error

                if (image.imageEtalon != null) {
                    image.imageEtalon.let {
                        binding.titleEtalon.text = "(EtalonImage)"
                    }
                    binding.imageEtalon.visibility = View.VISIBLE
                    binding.imageEtalon.setImageBitmap(image.imageEtalon)
                } else {
                    binding.titleEtalon.text = ""
                    binding.imageEtalon.visibility = View.GONE
                }
                if (image.image != null) {
                    binding.image.visibility = View.VISIBLE
                    binding.image.setImageBitmap(image.image)
                    binding.titleImage.text = "(Image)"
                } else {
                    binding.titleImage.text = ""
                    binding.image.visibility = View.GONE
                }
                image.status
                when (image.status) {
                    eCheckResult.CH_CHECK_OK -> {
                        binding.elementStatusImage.setImageResource(R.drawable.reg_icon_check_ok)
                    }

                    eCheckResult.CH_CHECK_ERROR -> {
                        binding.elementStatusImage.setImageResource(R.drawable.reg_icon_check_fail)
                    }

                    eCheckResult.CH_CHECK_WAS_NOT_DONE -> {
                        binding.elementStatusImage.setImageResource(R.drawable.reg_icon_no_check)
                    }

                    else -> {
                        binding.elementStatusImage.setImageBitmap(null)
                    }
                }
            }
        }

        class ImageVH(private val binding: RvImageBinding) : VH(binding.root) {
            override fun bind(base: Base) {
                val image = base as Image
                binding.title.text = image.title.uppercase(Locale.ROOT)

                if (image.title.uppercase(Locale.ROOT).contains("DOCUMENT IMAGE")) {
                    val resizedBitmap = adaptImageSize(image.value, 900)
                    binding.image.setImageBitmap(resizedBitmap)
                } else {
                    binding.image.setImageBitmap(image.value)
                }
            }
        }

        class StatusVH(private val binding: RvStatusBinding) : VH(binding.root) {
            override fun bind(base: Base) {
                val status = base as Status
                binding.title.text = status.title.uppercase(Locale.ROOT)
                when (status.value) {
                    0 -> binding.value.setImageResource(R.drawable.reg_icon_check_ok)
                    1 -> binding.value.setImageResource(R.drawable.reg_icon_check_fail)
                    2 -> binding.value.setImageResource(R.drawable.reg_icon_no_check)
                }
            }
        }
    }

    companion object {
        private const val SECTION = 0
        private const val BUTTON = 1
        private const val SWITCH = 2
        private const val STEPPER = 3
        private const val BOTTOM_SHEET = 4
        private const val BOTTOM_SHEET_MULTI = -2
        private const val INPUT_INT = 5
        private const val INPUT_STRING = 6
        private const val TEXT_RESULT = 7
        private const val IMAGE = 8
        private const val STATUS = 9
        private const val INPUT_DOUBLE = 10
        private const val IMAGE_PAIR = 11
        private const val AUTH_SECTION = 12
    }
}
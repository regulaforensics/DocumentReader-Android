package com.regula.documentreader

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.regula.documentreader.Helpers.Companion.beforeRender
import com.regula.documentreader.Helpers.Companion.dpToPx
import com.regula.documentreader.Helpers.Companion.pxToDp
import com.regula.documentreader.Helpers.Companion.themeColor
import com.regula.documentreader.databinding.BottomSheetBinding
import com.regula.documentreader.databinding.BottomSheetItemBinding
import com.regula.documentreader.databinding.RvHelpBigBinding
import java.io.Serializable

@Suppress("UNCHECKED_CAST")
class BottomSheet :
    BottomSheetDialogFragment() {
    private lateinit var binding: BottomSheetBinding
    private val title: String
        get() = requireArguments().getString("title")!!
    private val items: List<BSItemAbstract>
        get() = requireArguments().getSerializable("items") as List<BSItemAbstract>
    private val dismiss: Boolean
        get() = requireArguments().getBoolean("dismiss")
    private val cancelButtonText: String
        get() = requireArguments().getString("cancelButtonText")!!
    private val fullHeight: Boolean
        get() = requireArguments().getBoolean("fullHeight")
    private val action: (BSItem) -> Unit
        get() = requireArguments().getSerializable("action") as (BSItem) -> Unit

    companion object {
        fun newInstance(
            title: String,
            items: ArrayList<out BSItemAbstract>,
            dismiss: Boolean,
            cancelButtonText: String = "Cancel",
            fullHeight: Boolean = false,
            action: (BSItem) -> Unit
        ): BottomSheet {
            val args = Bundle()
            args.putString("title", title)
            args.putSerializable("items", items)
            args.putBoolean("dismiss", dismiss)
            args.putString("cancelButtonText", cancelButtonText)
            args.putBoolean("fullHeight", fullHeight)
            args.putSerializable("action", action as Serializable)
            val instance = BottomSheet()
            instance.arguments = args
            return instance
        }
    }

    override fun onCreateView(inflater: LayoutInflater, vg: ViewGroup?, bundle: Bundle?): View {
        binding = BottomSheetBinding.inflate(layoutInflater)

        binding.title.text = title
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.adapter =
            BottomSheetRecyclerAdapter(items, action) { if (dismiss) dismiss() }
        binding.recyclerView.addItemDecoration(DividerItemDecoration(activity, 1))
        binding.cancel.text = cancelButtonText
        binding.cancel.setOnClickListener { dismiss() }
        binding.root.beforeRender {
            binding.bottomSheetParent.minHeight =
                countHeight((binding.root.parent.parent as View).height, fullHeight)
        }

        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.setOnShowListener {
            (binding.root.parent as View).let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (view.parent as View).setBackgroundColor(Color.TRANSPARENT)
    }

    private fun countHeight(maxHeightPx: Int, fullHeight: Boolean): Int {
        if (fullHeight) return maxHeightPx

        val maxHeight = requireContext().pxToDp(maxHeightPx)
        var dpHeight = 149 + (items.size * 61)
        if (dpHeight > maxHeight)
            dpHeight = maxHeight
        return requireContext().dpToPx(dpHeight)
    }
}

class BottomSheetRecyclerAdapter(
    private val items: List<BSItemAbstract>,
    private val action: (BSItem) -> Unit,
    private val dismiss: () -> Unit
) : RecyclerView.Adapter<VHAbstract>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHAbstract =
        when (viewType) {
            0 -> VH(
                BottomSheetItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ), action, dismiss
            )
            else -> HelpBigVH(
                RvHelpBigBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

    override fun onBindViewHolder(vh: VHAbstract, i: Int) =
        vh.bind(items[i], i == items.size - 1)

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is BSItem -> 0
        is HelpBig -> 1
        else -> 0
    }

    override fun getItemCount() = items.size
}

abstract class VHAbstract(v: View) : RecyclerView.ViewHolder(v) {
    abstract fun bind(item: BSItemAbstract, isLast: Boolean)
}

class VH(
    private val binding: BottomSheetItemBinding,
    private val action: (BSItem) -> Unit,
    val dismiss: () -> Unit
) : VHAbstract(binding.root) {
    private var context: Context = binding.root.context

    override fun bind(item: BSItemAbstract, isLast: Boolean) {
        item as BSItem
        setBackground(isLast)
        init(item)
        binding.root.setOnClickListener {
            action(item)
            init(item)
            dismiss()
        }
    }

    private fun init(bsItem: BSItem) {
        binding.button.text = bsItem.title
        if (bsItem.selected)
            binding.check.visibility = View.VISIBLE
        else
            binding.check.visibility = View.INVISIBLE
    }

    private fun setBackground(isLast: Boolean) {
        if (isLast)
            binding.button.background = ResourcesCompat.getDrawable(
                context.resources, R.drawable.rounded_bottom, context.theme
            )!!
        else
            binding.button.setBackgroundColor(context.themeColor(R.attr.colorSecondary))
    }
}

class HelpBigVH(private val binding: RvHelpBigBinding) : VHAbstract(binding.root) {
    override fun bind(item: BSItemAbstract, isLast: Boolean) {
        item as HelpBig
        binding.image.setImageResource(item.image)
        binding.text.text = item.title
    }
}
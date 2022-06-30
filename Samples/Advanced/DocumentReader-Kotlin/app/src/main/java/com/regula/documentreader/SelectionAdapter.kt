package com.regula.documentreader

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.regula.documentreader.Helpers.Companion.getTranslation
import com.regula.documentreader.Helpers.Companion.themeColor
import com.regula.documentreader.databinding.RvTitleValueBinding

class SelectionAdapter(private val items: List<Direct>, var selectedId: String) :
    RecyclerView.Adapter<SelectionAdapter.DirectVH>() {
    private lateinit var binding: RvTitleValueBinding
    var enabled = true
    var selectedView: RvTitleValueBinding? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DirectVH {
        binding = RvTitleValueBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DirectVH(binding)
    }

    override fun getItemCount() = items.size
    override fun onBindViewHolder(vh: DirectVH, i: Int) = vh.bind(items[i], i)

    inner class DirectVH(private val binding: RvTitleValueBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val context = binding.root.context
        fun bind(direct: Direct, index: Int) {
            init(direct)

            binding.root.setOnClickListener {
                if (!enabled) return@setOnClickListener
                selectedView!!.root.setBackgroundColor(context.themeColor(R.attr.colorOnPrimary))
                binding.root.setBackgroundColor(context.themeColor(R.attr.colorSecondaryVariant))
                selectedView = binding
                selectedId = direct.title

                val field =
                    DirectResultsActivity.instance!!.fields[DirectResultsActivity.instance!!.selectedFieldIndex]
                field.selectedItem = index
                DirectResultsActivity.instance!!.binding.footerCodeDefinition.text =
                    DirectResultsActivity.instance!!.functionText()
            }
        }

        private fun init(direct: Direct) {
            binding.title.text = getTranslation(direct.type, direct.title, context)
            binding.value.text = context.getString(R.string.check)
            binding.value.setTextColor(context.getColor(R.color.blue))
            binding.value.visibility = View.INVISIBLE
            if (direct.presented)
                binding.value.visibility = View.VISIBLE
            if (selectedId == direct.title && selectedView == null)
                selectedView = binding
            if (selectedId == direct.title)
                binding.root.setBackgroundColor(context.themeColor(R.attr.colorSecondaryVariant))
            else
                binding.root.setBackgroundColor(context.themeColor(R.attr.colorOnPrimary))
        }
    }
}
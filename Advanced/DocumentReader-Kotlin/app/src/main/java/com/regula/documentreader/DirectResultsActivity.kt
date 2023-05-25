package com.regula.documentreader

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.regula.documentreader.Helpers.Companion.LCID
import com.regula.documentreader.Helpers.Companion.ResultType
import com.regula.documentreader.Helpers.Companion.VisualFieldType
import com.regula.documentreader.Helpers.Companion.grayedOutAlpha
import com.regula.documentreader.Helpers.Companion.keyByValue
import com.regula.documentreader.Helpers.Companion.openLink
import com.regula.documentreader.ParameterField.Companion.fieldType
import com.regula.documentreader.ParameterField.Companion.fieldType_lcid
import com.regula.documentreader.ParameterField.Companion.fieldType_lcid_sourceType
import com.regula.documentreader.ParameterField.Companion.fieldType_lcid_sourceType_original
import com.regula.documentreader.ParameterField.Companion.fieldType_original
import com.regula.documentreader.ParameterField.Companion.fieldType_sourceType
import com.regula.documentreader.ParameterField.Companion.fieldType_sourceType_original
import com.regula.documentreader.ParameterField.Companion.lcid
import com.regula.documentreader.ParameterField.Companion.original
import com.regula.documentreader.ParameterField.Companion.sourceType
import com.regula.documentreader.ResultsActivity.Companion.results
import com.regula.documentreader.api.enums.LCID.LATIN
import com.regula.documentreader.api.enums.eRPRM_ResultType.NONE
import com.regula.documentreader.databinding.ActivityDirectResultsBinding
import com.regula.documentreader.databinding.FragmentRvAddParameterBinding
import java.io.Serializable

class DirectResultsActivity : AppCompatActivity() {
    lateinit var binding: ActivityDirectResultsBinding
    var fields = mutableListOf<ParameterField>()
    private lateinit var fragments: Map<Int, ParameterFieldFragment>
    private var parameters = arrayOf(fieldType)
    var selectedFieldIndex: Int = 0

    companion object {
        var instance: DirectResultsActivity? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        binding = ActivityDirectResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Helpers.opaqueStatusBar(binding.root)
        binding.backBtn.setOnClickListener { finish() }
        binding.helpBtn.setOnClickListener { openLink(this, "Results") }

        setupFields()
        Helpers.replaceFragment(fragments[0] as Fragment, this, R.id.recyclerView)

        binding.resultsPicker.maxValue = fields.size - 1
        binding.resultsPicker.wrapSelectorWheel = false
        binding.resultsPicker.displayedValues = fields.map { it.title }.toTypedArray()
        binding.resultsPicker.setOnValueChangedListener { _, _, newVal ->
            binding.resultsPicker.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            Helpers.replaceFragment(fragments[newVal] as Fragment, this, R.id.recyclerView)
            selectedFieldIndex = newVal
        }
    }

    private fun setupFields() {
        val fieldType = ParameterField("filedType", true, fieldType)
        fieldType.presentedItems = results.textResult!!.fields.map { it.fieldType }.toSet().toList()
        fieldType.items = VisualFieldType.values.toList()
        sortFieldItems(fieldType)

        val lcid = ParameterField("lcid", false, lcid)
        lcid.presentedItems = results.textResult!!.fields.map { it.lcid }.toSet().toList()
        lcid.items = LCID.values.toList()
        sortFieldItems(lcid)

        val source = ParameterField("source", false, sourceType)
        val sources = results.textResult!!.fields.map { it.values }.flatten().map { it.sourceType }
        source.presentedItems = sources.toSet().toList()
        source.items = ResultType.values.toList()
        sortFieldItems(source)

        val original = ParameterField("original", false, original)
        original.items = listOf(0, 1)

        fields = mutableListOf(fieldType, lcid, source, original)

        fragments = mapOf(
            Pair(ParameterField.fieldType, ParameterFieldFragment.newInstance(fieldType)),
            Pair(ParameterField.lcid, ParameterFieldFragment.newInstance(lcid)),
            Pair(sourceType, ParameterFieldFragment.newInstance(source)),
            Pair(ParameterField.original, ParameterFieldFragment.newInstance(original)),
        )
    }

    private fun sortFieldItems(filed: ParameterField) {
        val notPresented = filed.items.toSet().subtract(filed.presentedItems)
        filed.items = filed.presentedItems.sorted() + notPresented.sorted()
    }

    private fun indicesFromFields(): List<Int> {
        val indices = mutableListOf<Int>()
        for (index in 0 until fields.size)
            if (fields[index].isOn)
                indices.add(index)
        return indices
    }

    private fun argumentsValues(indices: List<Int>): List<Int> {
        val arguments = mutableListOf<Int>()
        for (index in indices) {
            val selectedIndex = fields[index].selectedItem
            val intValue = fields[index].items[selectedIndex]
            arguments.add(intValue)
        }
        return arguments
    }

    private fun arguments(indices: List<Int>): List<String> {
        val arguments = mutableListOf<String>()
        for (index in indices) {
            val selectedIndex = fields[index].selectedItem
            val intValue = fields[index].items[selectedIndex]
            val arg = argument(fields[index].parameter, intValue)
            arguments.add(arg)
        }
        return arguments
    }

    private fun argument(parameters: Int, intValue: Int): String {
        when (parameters) {
            fieldType ->
                return keyByValue(VisualFieldType, intValue)
            lcid ->
                return keyByValue(LCID, intValue)
            sourceType ->
                return keyByValue(ResultType, intValue)
            original ->
                return if (intValue == 0) "false" else "true"
        }
        return "n/a"
    }

    private fun functionFormat(parameters: Array<Int>): String {
        var format = "Unknown configuration"
        when {
            parameters contentEquals arrayOf(fieldType) -> format =
                "getTextFieldByType(%@).value()!!.value!!"
            parameters contentEquals fieldType_lcid -> format =
                "getTextFieldByType(%@, %@).value()!!.value!!"
            parameters contentEquals fieldType_lcid_sourceType -> format =
                "getTextFieldValueByType(%@, %@, %@)"
            parameters contentEquals fieldType_lcid_sourceType_original -> format =
                "getTextFieldValueByType(%@, %@, %@, %@)"
            parameters contentEquals fieldType_sourceType -> format =
                "getTextFieldValueByType(%@, %@)"
            parameters contentEquals fieldType_sourceType_original -> format =
                "getTextFieldValueByTypeAndSource(%@, %@, %@)"
            parameters contentEquals fieldType_original -> format =
                "getTextFieldValueByType(%@, LATIN, NONE, %@)"
        }
        return format
    }

    private fun functionOutput(parameters: Array<Int>, args: List<Int>): String {
        var output: String? = "n/a"
        try {
            when {
                parameters contentEquals arrayOf(fieldType) -> output =
                    results.getTextFieldByType(args[0])!!.value()!!.value!!
                parameters contentEquals fieldType_lcid -> output =
                    results.getTextFieldByType(args[0], args[1])!!.value()!!.value!!
                parameters contentEquals fieldType_lcid_sourceType -> output =
                    results.getTextFieldValueByType(args[0], args[1], args[2])
                parameters contentEquals fieldType_lcid_sourceType_original -> output =
                    results.getTextFieldValueByType(args[0], args[1], args[2], args[3] != 0)
                parameters contentEquals fieldType_sourceType -> output =
                    results.getTextFieldValueByTypeAndSource(args[0], args[1])
                parameters contentEquals fieldType_sourceType_original ->
                    results.getTextFieldValueByTypeAndSource(args[0], args[1], args[2] != 0)
                parameters contentEquals fieldType_original -> output =
                    results.getTextFieldValueByType(args[0], LATIN, NONE, args[1] != 0)
            }
        } catch (e: Exception) {
        }
        return output ?: "n/a"
    }

    private fun stringWithFormat(str: String, args: List<String>): String {
        var result = str
        for (arg in args)
            result = result.replaceFirst("%@", arg)

        return result
    }

    fun functionText(): String {
        parameters = indicesFromFields().toTypedArray()
        val args = arguments(parameters.toList())
        val output = stringWithFormat(functionFormat(parameters), args)

        if (output != "Unknown configuration") {
            val argsInt = argumentsValues(parameters.toList())
            val text = functionOutput(parameters, argsInt)
            binding.footerResult.text = text
        }

        return output
    }
}

class ParameterFieldFragment : Fragment() {
    private var _binding: FragmentRvAddParameterBinding? = null
    private val binding get() = _binding!!
    private val parameterField: ParameterField
        get() = requireArguments().getSerializable("parameterField") as ParameterField

    companion object {
        fun newInstance(parameterField: ParameterField): ParameterFieldFragment {
            val args = Bundle()
            args.putSerializable("parameterField", parameterField as Serializable)
            val instance = ParameterFieldFragment()
            instance.arguments = args
            return instance
        }
    }

    override fun onCreateView(inflater: LayoutInflater, vg: ViewGroup?, bundle: Bundle?): View {
        _binding = FragmentRvAddParameterBinding.inflate(inflater, vg, false)
        val sectionsData = mutableListOf<Direct>()

        for (item in parameterField.items)
            sectionsData.add(
                Direct(
                    keyByValue(parameterField.parameter, item),
                    parameterField.presentedItems.contains(item),
                    parameterField.parameter
                )
            )

        binding.addParameter.isEnabled = parameterField.parameter != fieldType
        binding.recyclerView.adapter = SelectionAdapter(
            sectionsData,
            keyByValue(parameterField.parameter, parameterField.items[parameterField.selectedItem])
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        binding.addParameter.isChecked = parameterField.isOn
        enable(binding.recyclerView, parameterField, parameterField.isOn)
        binding.addParameter.setOnCheckedChangeListener { _, isChecked ->
            enable(binding.recyclerView, parameterField, isChecked)
        }
        binding.recyclerView.addItemDecoration(DividerItemDecoration(activity, 1))

        return binding.root
    }

    private fun enable(recyclerView: RecyclerView, field: ParameterField, enable: Boolean) {
        recyclerView.alpha = grayedOutAlpha(enable)
        (recyclerView.adapter as SelectionAdapter).enabled = enable
        recyclerView.suppressLayout(!enable)
        field.isOn = enable

        DirectResultsActivity.instance!!.binding.footerCodeDefinition.text =
            DirectResultsActivity.instance!!.functionText()
    }
}
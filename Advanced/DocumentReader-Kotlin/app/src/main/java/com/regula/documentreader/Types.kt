package com.regula.documentreader

import android.graphics.Bitmap
import android.view.View
import java.io.Serializable

class OnClickListenerSerializable(val function: (View?) -> Unit) : View.OnClickListener,
    Serializable {
    override fun onClick(v: View?) {
        function(v)
    }
}

abstract class BSItemAbstract(
    val title: String
)

class BSItem(
    title: String,
    var value: String = title
) : BSItemAbstract(title), Serializable {
    var selected: Boolean = false

    constructor(title: String, value: Int) : this(title, value.toString())
}

class HelpBig(
    title: String,
    val image: Int
) : BSItemAbstract(title), Serializable

abstract class Base(
    val title: String
)

open class Section(
    title: String,
    val helpTag: Int = 0
) : Base(title)

class AuthSection(
    title: String,
    val helpTag: Int,
    var checkStatus: Int
) : Base (title)

class Scan(
    title: String,
    var actionType: Int = ACTION_TYPE_SCANNER,
    var resetFunctionality: Boolean = true,
    var customize: () -> Unit = {}
) : Base(title) {
    companion object {
        const val ACTION_TYPE_SCANNER = 0
        const val ACTION_TYPE_GALLERY = 1
        const val ACTION_TYPE_CUSTOM = 2
        const val ACTION_TYPE_MANUAL_MULTIPAGE_MODE = 3
    }
}

class Switch(
    title: String,
    val get: () -> Boolean?,
    val set: (input: Boolean) -> Unit,
    val enabled: () -> Boolean? = { true },
) : Base(title)

class Stepper(
    title: String,
    val valueUnits: String,
    val get: () -> Int?,
    val set: (input: Int) -> Unit,
    val enabled: () -> Boolean? = { true },
    val step: Int = 1,
    val min: Int = -1,
    val addMinusOne: Boolean = false
) : Base(title)

class BS(
    title: String,
    val bsTitle: String,
    val items: ArrayList<BSItem>,
    val get: () -> String?,
    val set: (input: String) -> Unit
) : Base(title), Serializable {
    private val valueTitleMap = mutableMapOf<String, String>()

    init {
        items.forEach { valueTitleMap[it.value] = it.title }
    }

    fun getFromMap() = valueTitleMap[get()]
}

class BSMulti(
    title: String,
    val bsTitle: String,
    val items: ArrayList<BSItem>,
    val get: () -> MutableList<String>,
    val set: (input: List<String>) -> Unit
) : Base(title), Serializable {
    private val valueTitleMap = mutableMapOf<String, String>()

    init {
        items.forEach { valueTitleMap[it.value] = it.title }
    }

    fun getFromMap(): MutableList<String>{
        val result = mutableListOf<String>()
        get().sorted().forEach { result.add(valueTitleMap[it]!!) }
        return result
    }
}

class InputInt(
    title: String,
    val get: () -> Int?,
    val set: (input: Int?) -> Unit
) : Base(title)

class InputDouble(
    title: String,
    val get: () -> Double?,
    val set: (input: Double?) -> Unit
) : Base(title)

class InputString(
    title: String,
    val get: () -> String,
    val set: (input: String) -> Unit
) : Base(title)

class TextResult(
    title: String,
    var value: String,
    var lcid: String,
    var pageIndex: Int,
    var color: Int
) : Base(title)

class Image(
    title: String,
    var value: Bitmap,
) : Base(title)
class ImagePair(
    title: String,
    var error: String?,
    var image: Bitmap?,
    var imageEtalon: Bitmap?,
    var status: Int?,
) : Base(title)

class Status(
    title: String,
    var value: Int,
) : Base(title) {
    companion object {
        const val SUCCESS = 0
        const val FAIL = 1
        const val UNDEFINED = 2
    }
}

class Direct(
    title: String,
    var presented: Boolean,
    val type: Int
) : Base(title)

class Attribute(
    var name: String,
    var value: String? = null,
    var lcid: Int? = null,
    var pageIndex: Int? = null,
    var valid: Int? = null,
    var source: Int? = null,
    @Transient
    var image: Bitmap? = null,
    var imageEtalon: Bitmap? = null,
    var equality: Boolean = true,
    var rfidStatus: Int? = null,
    var checkResult: Int? = null,
) : Serializable {
    override fun equals(other: Any?) = (other is Attribute) && name == other.name
    override fun hashCode() = name.hashCode()
}

open class GroupedAttributes(
    var type: String = "",
    var items: MutableList<Attribute> = mutableListOf(),
    var comparisonLHS: Int? = null,
    var comparisonRHS: Int? = null
) : Serializable

class AuthGroupedAttributes(
    type: String = "",
    items: MutableList<Attribute> = mutableListOf(),
    var checkStatus: Int = 0,
    comparisonLHS: Int? = null,
    comparisonRHS: Int? = null
) : GroupedAttributes (type, items, comparisonLHS, comparisonRHS)

class ParameterField(
    val title: String,
    var isOn: Boolean,
    val parameter: Int,
    var selectedItem: Int = 0,
    var items: List<Int> = listOf(),
    var presentedItems: List<Int> = listOf()
) : Serializable {
    companion object {
        const val fieldType = 0
        const val lcid = 1
        const val sourceType = 2
        const val original = 3
        val fieldType_lcid_sourceType_original = arrayOf(fieldType, lcid, sourceType, original)
        val fieldType_sourceType_original = arrayOf(fieldType, sourceType, original)
        val fieldType_lcid_sourceType = arrayOf(fieldType, lcid, sourceType)
        val fieldType_sourceType = arrayOf(fieldType, sourceType)
        val fieldType_original = arrayOf(fieldType, original)
        val fieldType_lcid = arrayOf(fieldType, lcid)
    }
}
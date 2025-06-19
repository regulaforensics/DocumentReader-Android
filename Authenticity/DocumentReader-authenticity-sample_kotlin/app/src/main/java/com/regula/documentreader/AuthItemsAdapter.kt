package com.regula.documentreader

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.regula.documentreader.api.results.authenticity.DocumentReaderAuthenticityElement
import com.regula.documentreader.api.results.authenticity.DocumentReaderIdentResult
import com.regula.documentreader.api.results.authenticity.DocumentReaderOCRSecurityTextResult
import com.regula.documentreader.api.results.authenticity.DocumentReaderPhotoIdentResult
import com.regula.documentreader.api.results.authenticity.DocumentReaderSecurityFeatureCheck
import com.regula.documentreader.api.results.authenticity.DocumentReaderUvFiberElement
import com.regula.documentreader.databinding.ElementIdentResultBinding
import com.regula.documentreader.databinding.ElementOcrSecurityTextResultBinding
import com.regula.documentreader.databinding.ElementPhotoIdentResultBinding
import com.regula.documentreader.databinding.ElementSecurityFeautureCheckBinding
import com.regula.documentreader.databinding.ElementUvFibersBinding
import com.regula.documentreader.databinding.ItemViewBinding

class AuthItemsAdapter(
    private val context: Context,
    private val elements: MutableList<DocumentReaderAuthenticityElement>?
) :
    RecyclerView.Adapter<AuthItemsAdapter.AuthViewHolder>() {

    override fun getItemViewType(position: Int): Int = when (elements?.get(position)) {
        is DocumentReaderIdentResult -> IDENT_RESULT
        is DocumentReaderOCRSecurityTextResult -> OCR_SECURITY_TEXT_RESULT
        is DocumentReaderPhotoIdentResult -> PHOTO_IDENT_RESULT
        is DocumentReaderSecurityFeatureCheck -> SECURITY_FEAUTURE
        is DocumentReaderUvFiberElement -> UV_FIBERS
        else -> 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AuthViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            IDENT_RESULT -> AuthViewHolder.ElementIdentResult(
                ElementIdentResultBinding.inflate(layoutInflater, parent, false)
            )

            OCR_SECURITY_TEXT_RESULT -> AuthViewHolder.ElementOCRSecurityTextResult(
                ElementOcrSecurityTextResultBinding.inflate(layoutInflater, parent, false)
            )

            PHOTO_IDENT_RESULT -> AuthViewHolder.ElementPhotoIdentResult(
                ElementPhotoIdentResultBinding.inflate(layoutInflater, parent, false)
            )

            SECURITY_FEAUTURE -> AuthViewHolder.ElementSecurityFeatureCheck(
                ElementSecurityFeautureCheckBinding.inflate(layoutInflater, parent, false)
            )

            UV_FIBERS -> AuthViewHolder.ElementUvFiberElement(
                ElementUvFibersBinding.inflate(layoutInflater, parent, false)
            )
            else -> AuthViewHolder.SimpleElement(
                ItemViewBinding.inflate(layoutInflater, parent, false)
            )
        }
    }

    override fun getItemCount(): Int {
        return elements!!.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: AuthViewHolder, position: Int) =
        holder.bind(elements!![position])

    sealed class AuthViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val context: Context = view.context

        abstract fun bind(element: DocumentReaderAuthenticityElement?)

        class ElementIdentResult(private val binding: ElementIdentResultBinding) :
            AuthViewHolder(binding.root) {

            @SuppressLint("SetTextI18n")
            override fun bind(element: DocumentReaderAuthenticityElement?) {
                if (element == null) {
                    return
                }
                binding.tvElementName.text =
                    "(${element.elementType}) ${element.getElementTypeName(context)}"
                binding.ivElementStatusImage.setImageResource(Helpers.getStatusImage(element.status))
                binding.tvItemDiagnose.text =
                    "(${element.elementDiagnose}) ${element.getElementDiagnoseName(context)}"

                val expandedElement = element as DocumentReaderIdentResult
                if (expandedElement.etalonImage?.bitmap != null) {
                    binding.ivReference.setImageBitmap(expandedElement.etalonImage?.bitmap)
                } else {
                    binding.ivReference.visibility = View.GONE
                    binding.tvReferenceImageDescription.text = "image not found"
                }
                if (expandedElement.image?.bitmap != null) {
                    binding.ivElement.setImageBitmap(expandedElement.image?.bitmap)
                } else {
                    binding.ivElement.visibility = View.GONE
                    binding.tvImageDescription.text = "image reference not found"
                }
                binding.tvPercentage.text = expandedElement.percentValue.toString() + '%'
                binding.tvArea.text = expandedElement.area?.height.toString() + "/" + expandedElement.area?.width.toString()
                binding.tvLightIndex.text = expandedElement.lightIndex.toString()
            }
        }

        class ElementOCRSecurityTextResult(private val binding: ElementOcrSecurityTextResultBinding) :
            AuthViewHolder(binding.root) {
            override fun bind(element: DocumentReaderAuthenticityElement?) {
                if (element == null) {
                    return
                }
                binding.tvElementName.text =
                    "(${element.elementType}) ${element.getElementTypeName(context)}"
                binding.ivElementStatusImage.setImageResource(Helpers.getStatusImage(element.status))
                binding.tvItemDiagnose.text =
                    "(${element.elementDiagnose}) ${element.getElementDiagnoseName(context)}"

                val expandedElement = element as DocumentReaderOCRSecurityTextResult
                binding.tvOcrResult.text = expandedElement.securityTextResultOCR
                binding.tvOcrEtalon.text = expandedElement.etalonResultOCR
                binding.tvLightTypeResult.text = expandedElement.lightType.toString()
                binding.tvEtalonLightType.text = expandedElement.etalonLightType.toString()
                binding.tvEtalonFieldType.text = expandedElement.etalonFieldType.toString()
                binding.tvEtalonResultType.text = expandedElement.etalonResultType.toString()
                binding.tvReserved1.text = expandedElement.reserved1.toString()
                binding.tvReserved2.text = expandedElement.reserved2.toString()
            }

        }

        class ElementPhotoIdentResult(private val binding: ElementPhotoIdentResultBinding) :
            AuthViewHolder(binding.root) {
            override fun bind(element: DocumentReaderAuthenticityElement?) {
                if (element == null) {
                    return
                }
                binding.tvElementName.text =
                    "(${element.elementType}) ${element.getElementTypeName(context)}"
                binding.ivElementStatusImage.setImageResource(Helpers.getStatusImage(element.status))
                binding.tvItemDiagnose.text =
                    "(${element.elementDiagnose}) ${element.getElementDiagnoseName(context)}"

                val expandedElement = element as DocumentReaderPhotoIdentResult
                binding.tvResult.text = expandedElement.result.toString()
                binding.tvArea.text = expandedElement.area?.height.toString() + "/" + expandedElement.area?.width.toString()
                binding.tvLightIndex.text = expandedElement.lightIndex.toString()

                if (expandedElement.resultImages[0].bitmap != null) {
                    binding.ivResult.setImageBitmap(expandedElement.resultImages[0].bitmap)
                } else {
                    binding.ivResult.visibility = View.GONE
                    binding.tvImageDescription.text = "image not found"
                }
                if (expandedElement.sourceImage[0].bitmap != null) {
                    binding.ivSource.setImageBitmap(expandedElement.sourceImage[0].bitmap)
                } else {
                    binding.ivSource.visibility = View.GONE
                    binding.tvReferenceImageDescription.text = "image reference not found"
                }

                binding.tvReserved1.text = expandedElement.reserved1.toString()
                binding.tvReserved2.text = expandedElement.reserved2.toString()
                binding.tvReserved3.text = expandedElement.reserved3.toString()
            }
        }

        class ElementSecurityFeatureCheck(private val binding: ElementSecurityFeautureCheckBinding) :
            AuthViewHolder(binding.root) {
            override fun bind(element: DocumentReaderAuthenticityElement?) {
                if (element == null) {
                    return
                }
                binding.tvElementName.text =
                    "(${element.elementType}) ${element.getElementTypeName(context)}"
                binding.ivElementStatusImage.setImageResource(Helpers.getStatusImage(element.status))
                binding.tvItemDiagnose.text =
                    "(${element.elementDiagnose}) ${element.getElementDiagnoseName(context)}"

                val expandedElement = element as DocumentReaderSecurityFeatureCheck

                if (expandedElement.elementRect != null){
                    binding.tvElementRectDescription.visibility = View.VISIBLE
                    binding.tvElementRect.visibility = View.VISIBLE
                    binding.tvElementRect.text = "H:${expandedElement.elementRect!!.height} / W:${expandedElement.elementRect!!.width}"
                }
            }
        }

        class ElementUvFiberElement(private val binding: ElementUvFibersBinding) :
            AuthViewHolder(binding.root) {
            override fun bind(element: DocumentReaderAuthenticityElement?) {
                if (element == null) {
                    return
                }
                binding.tvElementName.text =
                    "(${element.elementType}) ${element.getElementTypeName(context)}"
                binding.ivElementStatusImage.setImageResource(Helpers.getStatusImage(element.status))
                binding.tvItemDiagnose.text =
                    "(${element.elementDiagnose}) ${element.getElementDiagnoseName(context)}"

                val expandedElement = element as DocumentReaderUvFiberElement
                var actualValuesText = ""

                if (expandedElement.width != null){
                    binding.layoutWidth.visibility = View.VISIBLE
                    binding.tvWidth.visibility = View.VISIBLE
                    binding.tvWidthValue.visibility = View.VISIBLE
                    for (i in expandedElement.width){
                        actualValuesText += "$i\n"
                    }
                    binding.tvWidthValue.text = actualValuesText
                }
                if (expandedElement.length != null){
                    binding.layoutLength.visibility = View.VISIBLE
                    binding.tvLength.visibility = View.VISIBLE
                    binding.tvLengthValue.visibility = View.VISIBLE
                    for (i in expandedElement.length){
                        actualValuesText += "$i\n"
                    }
                    binding.tvLengthValue.text = actualValuesText
                }
                if (expandedElement.area != null){
                    binding.layoutArea.visibility = View.VISIBLE
                    binding.tvArea.visibility = View.VISIBLE
                    binding.tvAreaValue.visibility = View.VISIBLE

                    for (i in expandedElement.area){
                        actualValuesText += "$i\n"
                    }
                    binding.tvAreaValue.text = actualValuesText
                }
                if (expandedElement.colorValues != null){
                    binding.layoutColorValues.visibility = View.VISIBLE
                    binding.tvColorValues.visibility = View.VISIBLE
                    binding.tvColorValuesValue.visibility = View.VISIBLE

                    for (i in expandedElement.colorValues){
                        actualValuesText += "$i\n"
                    }
                    binding.tvColorValuesValue.text = actualValuesText
                }

                for (i in expandedElement.rectArray){
                    actualValuesText += "$i\n"
                }
                binding.tvReactArrayValue.text = actualValuesText

                binding.tvRectCountValue.text = expandedElement.rectCount.toString()
                binding.tvExpectedCountValue.text = expandedElement.expectedCount.toString()
            }
        }
        class SimpleElement(private val binding: ItemViewBinding) :
            AuthViewHolder(binding.root) {
            override fun bind(element: DocumentReaderAuthenticityElement?) {
                if (element == null) {
                    return
                }
                binding.tvElementName.text =
                    "(${element.elementType}) ${element.getElementTypeName(context)}"
                binding.ivElementStatusImage.setImageResource(Helpers.getStatusImage(element.status))
                binding.tvItemDiagnose.text =
                    "(${element.elementDiagnose}) ${element.getElementDiagnoseName(context)}"
            }

        }
    }

    companion object {
        private const val IDENT_RESULT = 0
        private const val OCR_SECURITY_TEXT_RESULT = 1
        private const val PHOTO_IDENT_RESULT = 2
        private const val SECURITY_FEAUTURE = 3
        private const val UV_FIBERS = 4
    }
}
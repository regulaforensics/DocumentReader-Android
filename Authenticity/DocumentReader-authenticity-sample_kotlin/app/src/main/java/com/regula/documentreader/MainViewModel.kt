package com.regula.documentreader

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.completions.IDocumentReaderCompletion
import com.regula.documentreader.api.completions.IDocumentReaderInitCompletion
import com.regula.documentreader.api.completions.rfid.IRfidReaderCompletion
import com.regula.documentreader.api.config.RecognizeConfig
import com.regula.documentreader.api.config.ScannerConfig
import com.regula.documentreader.api.enums.DocReaderAction
import com.regula.documentreader.api.enums.Scenario
import com.regula.documentreader.api.errors.DocReaderRfidException
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.params.AuthenticityParams
import com.regula.documentreader.api.params.DocReaderConfig
import com.regula.documentreader.api.params.ImageInputData
import com.regula.documentreader.api.params.LivenessParams
import com.regula.documentreader.api.results.DocumentReaderNotification
import com.regula.documentreader.api.results.DocumentReaderResults
import com.regula.documentreader.api.results.DocumentReaderScenario
import com.regula.documentreader.util.Utils

class MainViewModel(val documentReader: DocumentReader): ViewModel()  {

    companion object {
        const val DATABASE_ID = "FullAuth"
    }

    val initLiveData = MutableLiveData<DocumentReaderException?>()
    val showScannerSuccessCompletion = MutableLiveData<DocumentReaderResults?>()
    val showScannerErrorCompletion = MutableLiveData<DocumentReaderException?>()
    val showScannerCancelCompletion = MutableLiveData<DocumentReaderResults?>()
    private var rfidReading: Boolean = false
    @SuppressLint("StaticFieldLeak")
    private var context: Context? = null

    fun init(context: Context) {
        val license = Utils.getLicense(context) ?: return
        this.context = context
        val config = DocReaderConfig(license)
        config.setLicenseUpdate(true)

        documentReader.initializeReader(
            context,
            config,
            IDocumentReaderInitCompletion { result: Boolean, error: DocumentReaderException? ->
                if (!result) { //Initialization was not successful
                    initLiveData.value = error
                    return@IDocumentReaderInitCompletion
                }
                setupCustomization(context)
                setupFunctionality(context)
                setupProcessParams(context)

                initLiveData.value = null
            })
    }

    private fun setupCustomization(context: Context) {
        //set the default settings you want
    }

    private fun setupFunctionality(context: Context) {
        //set the default settings you want
    }

    private fun setupProcessParams(context: Context) {
        //set the default settings you want
    }

    fun updateRfidReading(rfidReading: Boolean){
        this.rfidReading = rfidReading
    }

    fun showScanner(context: Context) {
        if (!documentReader.isReady) return

        val scannerConfig = ScannerConfig.Builder(Scenario.SCENARIO_FULL_AUTH).build()
        documentReader.startScanner(context, scannerConfig, completion)
    }

    fun recognize(context: Context, bitmap: Bitmap) {
        val recognizeConfig = RecognizeConfig.Builder(Scenario.SCENARIO_FULL_AUTH).setBitmap(bitmap).build()
        documentReader.recognize(recognizeConfig, completion)
    }

    fun getAvailableScenarios(): MutableList<DocumentReaderScenario> {
        return DocumentReader.Instance().availableScenarios
    }

    fun setupMultipage(value: Boolean){
        documentReader.processParams().multipageProcessing = value
    }

    fun setupAlreadyCropped(value: Boolean){
        documentReader.processParams().alreadyCropped = value
    }

    fun setupAuthenticityCheckImagePatterns(value: Boolean?){
        documentReader.processParams().authenticityParams?.checkImagePatterns = value
    }

    fun initAuth(){
        documentReader.processParams().authenticityParams = AuthenticityParams.defaultParams()
    }

    fun setupAuthenticityCheckPhotoEmbedding(value: Boolean?){
        documentReader.processParams().authenticityParams?.checkPhotoEmbedding = value
    }

    fun setupAuthenticityCheckBarcodeFormat(value: Boolean?){
        documentReader.processParams().authenticityParams?.checkBarcodeFormat = value
    }

    fun setupAuthenticityCheckPhotoComparison(value: Boolean?){
        documentReader.processParams().authenticityParams?.checkPhotoComparison = value
    }

    fun setupAuthenticityCheckUVLuminiscence(value: Boolean?){
        documentReader.processParams().authenticityParams?.checkUVLuminiscence = value
    }

    fun setupAuthenticityCheckFibers(value: Boolean?){
        documentReader.processParams().authenticityParams?.checkFibers = value
    }

    fun setupAuthenticityCheckExtMRZ(value: Boolean?){
        documentReader.processParams().authenticityParams?.checkExtMRZ = value
    }

    fun setupAuthenticityCheckExtOCR(value: Boolean?){
        documentReader.processParams().authenticityParams?.checkExtOCR = value
    }

    fun setupAuthenticityCheckIRB900(value: Boolean?){
        documentReader.processParams().authenticityParams?.checkIRB900 = value
    }

    fun setupAuthenticityCheckIRVisibility(value: Boolean?){
        documentReader.processParams().authenticityParams?.checkIRVisibility = value
    }

    fun setupAuthenticityCheckIPI(value: Boolean?){
        documentReader.processParams().authenticityParams?.checkIPI = value
    }

    fun setupAuthenticityCheckSecurityText(value: Boolean?){
        documentReader.processParams().authenticityParams?.checkSecurityText = value
    }

    fun setupAuthenticityUseLiveness(value: Boolean?){
        documentReader.processParams().authenticityParams?.useLivenessCheck = value
    }

    fun setupAuthenticityLivenessCheckHolo(value: Boolean?){
        documentReader.processParams().authenticityParams?.livenessParams?.checkHolo = value
    }

    fun setupAuthenticityLivenessCheckEd(value: Boolean?){
        documentReader.processParams().authenticityParams?.livenessParams?.checkED = value
    }

    fun setupAuthenticityLivenessCheckOvi(value: Boolean?){
        documentReader.processParams().authenticityParams?.livenessParams?.checkOVI = value
    }

    fun setupAuthenticityLivenessCheckMli(value: Boolean?){
        documentReader.processParams().authenticityParams?.livenessParams?.checkMLI = value
    }
    fun setupAuthenticityLivenessCheckDynaprint(value: Boolean?){
        documentReader.processParams().authenticityParams?.livenessParams?.checkDynaprint = value
    }
    fun setupAuthenticityLivenessCheckBlackAndWhiteCopy(value: Boolean?){
        documentReader.processParams().authenticityParams?.livenessParams?.checkBlackAndWhiteCopy = value
    }
    fun setupAuthenticityLivenessCheckGeometry(value: Boolean?){
        documentReader.processParams().authenticityParams?.livenessParams?.checkGeometry = value
    }

    fun getActualLivenessParams(): LivenessParams? {
        if (documentReader.processParams().authenticityParams?.livenessParams == null){
            documentReader.processParams().authenticityParams?.livenessParams = LivenessParams.defaultParams()
        }
        return documentReader.processParams().authenticityParams?.livenessParams
    }

    fun getUselivenessCheckState(): Boolean?{
        return documentReader.processParams().authenticityParams?.useLivenessCheck
    }

    fun recognizeSerialImages(vararg imageInputData: ImageInputData, context: Context, dialogView: View) {
        Helpers.showDialog(context, "Processing images", dialogView = dialogView)
        val recognizeConfig = RecognizeConfig.Builder(Scenario.SCENARIO_FULL_AUTH).setImageInputData(imageInputData).build()
        DocumentReader.Instance().recognize(recognizeConfig, completion)
    }

    private val completion =
        IDocumentReaderCompletion { action, results, error ->
            //processing is finished, all results are ready
            if (action == DocReaderAction.COMPLETE || action == DocReaderAction.TIMEOUT) {
                if (rfidReading && results?.chipPage != 0 && DocumentReader.Instance().isRFIDAvailableForUse) {

                    documentReader.startRFIDReader(context!!, object : IRfidReaderCompletion() {
                        override fun onChipDetected() {
                            Log.d("Rfid", "Chip detected")
                        }

                        override fun onProgress(notification: DocumentReaderNotification) {
                            //rfid progress notification if you want
                        }

                        override fun onRetryReadChip(exception: DocReaderRfidException) {
                            Log.d("Rfid", "Retry with error: " + exception.errorCode)
                        }

                        override fun onCompleted(
                            rfidAction: Int,
                            results_RFIDReader: DocumentReaderResults?,
                            error: DocumentReaderException?
                        ) {
                            if (rfidAction == DocReaderAction.COMPLETE
                                || rfidAction == DocReaderAction.ERROR
                                || rfidAction == DocReaderAction.CANCEL
                            ) {
                                showScannerSuccessCompletion.value = results_RFIDReader!!
                                return
                            }
                        }
                    })
                } else showScannerSuccessCompletion.value = results
            } else {
                //something happened before all results were ready
                if (action == DocReaderAction.CANCEL) {
                    showScannerCancelCompletion.value = results
                    Log.d("ViewModel", "Scanning was cancelled")
                } else if (action == DocReaderAction.ERROR) {
                    showScannerErrorCompletion.value = error
                    Log.d("ViewModel", "Error:$error")
                }
            }
        }
}
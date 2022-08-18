package com.regula.rfid_certificates_runtime_kotlin

import android.content.res.AssetManager
import com.regula.documentreader.api.enums.PKDResourceType
import com.regula.documentreader.api.params.rfid.PKDCertificate
import java.io.IOException
import java.io.InputStream

object CertificatesUtil {

    fun getRfidTaCertificate(
        am: AssetManager,
        certificateName: String,
        certificateKey: String
    ): PKDCertificate {
        val certificate = PKDCertificate()
        certificate.resourceType = PKDResourceType.CERTIFICATE_TA
        certificate.binaryData = getBinaryData(am, certificateName)
        certificate.privateKey = getBinaryData(am, certificateKey)
        return certificate
    }

    private fun getBinaryData(
        am: AssetManager,
        fileName: String
    ): ByteArray? {
        try {
            val filePath = "Regula/certificates_ta"
            val licInput: InputStream = am.open("$filePath/$fileName")
            val available: Int = licInput.available()
            val binaryData = ByteArray(available)
            licInput.read(binaryData)
            return binaryData
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }
}
package com.regula.certificates_default_kotlin

import android.content.res.AssetManager
import com.regula.documentreader.api.enums.PKDResourceType
import com.regula.documentreader.api.params.rfid.PKDCertificate
import java.io.IOException
import java.io.InputStream
import java.util.*


object CertificatesUtil {
    fun getRfidCertificate(am: AssetManager): List<PKDCertificate> {
        val pkdCertificatesList: MutableList<PKDCertificate> = ArrayList()
        val licInput: InputStream = am.open("Regula/certificates/PKDML.000259.ldif")
        val available: Int = licInput.available()
        val binaryData = ByteArray(available)
        licInput.read(binaryData)

        val certificate = PKDCertificate(binaryData, PKDResourceType.LDIF)
        pkdCertificatesList.add(certificate)

        val cert1 = getRfidTaCertificate(
            am,
            "FRCVCA_CC.cvCert",
            "FRCVCA_CC.pkcs8"
        )

        val cert2 = getRfidTaCertificate(
            am,
            "FRDV_CC.cvCert",
            "FRDV_CC.pkcs8"
        )

        val cert3 = getRfidTaCertificate(
            am,
            "FRIS_CC.cvCert",
            "FRIS_CC.pkcs8"
        )

        pkdCertificatesList.add(cert1)
        pkdCertificatesList.add(cert2)
        pkdCertificatesList.add(cert3)

        return pkdCertificatesList
    }

    fun getRfidTaCertificate(
        am: AssetManager,
        certificateName: String,
        certificateKey: String
    ): PKDCertificate {
        val certificate = PKDCertificate()
        certificate.resourceType = PKDResourceType.CERTIFICATE_TA
        getBinaryData(am, certificateName)?.let {
            certificate.binaryData = it
        }
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
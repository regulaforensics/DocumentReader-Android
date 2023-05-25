package com.regula.certificates_default_kotlin

import android.content.res.AssetManager
import com.regula.documentreader.api.enums.PKDResourceType
import com.regula.documentreader.api.params.rfid.PKDCertificate
import java.io.InputStream
import java.util.*


object CertificatesUtil {
    fun getRfidCertificate(am: AssetManager): List<PKDCertificate> {
        val pkdCertificatesList: MutableList<PKDCertificate> = ArrayList()
        val licInput: InputStream = am.open("Regula/certificates/PKDML.000209.ldif")
        val available: Int = licInput.available()
        val binaryData = ByteArray(available)
        licInput.read(binaryData)

        val certificate = PKDCertificate(binaryData, PKDResourceType.LDIF)
        pkdCertificatesList.add(certificate)
        return pkdCertificatesList
    }
}
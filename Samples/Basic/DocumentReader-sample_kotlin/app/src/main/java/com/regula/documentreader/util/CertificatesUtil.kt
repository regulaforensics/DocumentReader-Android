package com.regula.documentreader.util

import android.content.res.AssetManager
import com.regula.documentreader.api.enums.PKDResourceType
import com.regula.documentreader.api.params.rfid.PKDCertificate
import java.io.IOException
import java.io.InputStream
import java.util.*

object CertificatesUtil {

    fun getRfidCertificates(am: AssetManager, certificatesDir: String): List<PKDCertificate> {
        val pkdCertificatesList: MutableList<PKDCertificate> = ArrayList()
        try {
            val list = am.list(certificatesDir)
            if (list != null && list.isNotEmpty()) {
                for (file in list) {
                    val findExtension = file.split("\\.").toTypedArray()
                    var pkdResourceType = 0
                    if (findExtension.isNotEmpty()) {
                        pkdResourceType =
                            PKDResourceType.getType(findExtension[findExtension.size - 1].toLowerCase())
                    }
                    val licInput = am.open("$certificatesDir/$file")
                    val available = licInput.available()
                    val binaryData = ByteArray(available)
                    licInput.read(binaryData)
                    val certificate = PKDCertificate(binaryData, pkdResourceType)
                    pkdCertificatesList.add(certificate)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return pkdCertificatesList

    }

    fun getRfidTACertificates(am: AssetManager): List<PKDCertificate>? {
        val pkdCertificatesList: MutableList<PKDCertificate> = ArrayList()
        val certificatesDir = "Regula/certificates_ta"
        try {
            val filesCertMap: MutableMap<String, MutableList<String>> = HashMap()
            val list = am.list(certificatesDir)
            if (list != null && list.isNotEmpty()) {
                for (file in list) {
                    val findExtension = file.split("\\.").toTypedArray()
                    if (!filesCertMap.containsKey(findExtension[0])) {
                        val certList: MutableList<String> = ArrayList()
                        certList.add(file)
                        filesCertMap[findExtension[0]] = certList
                    } else {
                        filesCertMap[findExtension[0]]!!.add(file)
                    }
                }
            }
            for ((_, value) in filesCertMap) {
                val files = value as List<String>
                val certificate = PKDCertificate()
                for (file in files) {
                    val findExtension = file.split("\\.").toTypedArray()
                    certificate.resourceType = PKDResourceType.CERTIFICATE_TA
                    val licInput: InputStream = am.open("$certificatesDir/$file")
                    val available: Int = licInput.available()
                    val binaryData = ByteArray(available)
                    licInput.read(binaryData)
                    if (findExtension[1] == "cvCert") {
                        certificate.binaryData = binaryData
                    } else {
                        certificate.privateKey = binaryData
                    }
                }
                pkdCertificatesList.add(certificate)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return pkdCertificatesList
    }
}
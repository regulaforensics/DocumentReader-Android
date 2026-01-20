package com.regula.documentreader.util

import android.app.Activity
import android.content.Context
import com.regula.documentreader.R
import com.regula.documentreader.api.enums.PKDResourceType
import com.regula.documentreader.api.params.rfid.PKDCertificate
import java.io.IOException
import java.util.Locale

object Utils {

    fun getLicense(context: Context?): ByteArray? {
        if (context == null) return null
        val licInput = context.resources.openRawResource(R.raw.regula)
        val available: Int = try {
            licInput.available()
        } catch (e: IOException) {
            return null
        }
        val license = ByteArray(available)
        try {
            licInput.read(license)
        } catch (e: IOException) {
            return null
        }
        return license
    }

    fun getRfidCertificates(activity: Activity, certificatesDir: String): List<PKDCertificate> {
        val pkdCertificatesList: MutableList<PKDCertificate> = ArrayList()
        val am = activity.assets
        try {
            val list = am.list(certificatesDir)
            if (list != null && list.size > 0) {
                for (file in list) {
                    val findExtension =
                        file.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    var pkdResourceType = 0
                    if (findExtension.size > 0) {
                        pkdResourceType = PKDResourceType.getType(
                            findExtension[findExtension.size - 1].lowercase(
                                Locale.getDefault()
                            )
                        )
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
}
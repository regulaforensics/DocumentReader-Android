package com.regula.documentreader.util

import android.content.Context
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.util.*

object LicenseUtil {
    fun readFileFromAssets(
        assetPackageName: String,
        fileName: String,
        context: Context
    ): ByteArray? {
        val licInput: InputStream
        try {
            if (context.assets.list(assetPackageName) == null
                || !Arrays.asList(*context.assets.list(assetPackageName)).contains(fileName)
            ) {
                Log.e("FileUtil", "asset: $fileName is absent")
                return null
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        try {
            licInput = context.assets.open("$assetPackageName/$fileName")
            val available = licInput.available()
            if (available == 0) return null
            val license = ByteArray(available)
            licInput.read(license)
            licInput.close()
            return license
        } catch (e: Exception) {
            e.message
        }
        return null
    }
}
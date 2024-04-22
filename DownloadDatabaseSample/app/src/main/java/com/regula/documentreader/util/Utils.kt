package com.regula.documentreader.util

import android.content.Context
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.CharacterIterator
import java.text.StringCharacterIterator


object Utils {

    fun getLicense(context: Context?): ByteArray? {
        if (context == null) return null

        val inputStream: InputStream = context.assets.open("regula.license")

        val buffer = ByteArray(8192)
        var bytesRead: Int
        val output = ByteArrayOutputStream()
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            output.write(buffer, 0, bytesRead)
        }

        return output.toByteArray()
    }

    fun humanReadableByteCountSI(bytes: Long): String? {
        var bytes = bytes
        if (-1000 < bytes && bytes < 1000) {
            return "$bytes B"
        }
        val ci: CharacterIterator = StringCharacterIterator("kMGTPE")
        while (bytes <= -999950 || bytes >= 999950) {
            bytes /= 1000
            ci.next()
        }
        return String.format("%.1f %cB", bytes / 1000.0, ci.current())
    }
}
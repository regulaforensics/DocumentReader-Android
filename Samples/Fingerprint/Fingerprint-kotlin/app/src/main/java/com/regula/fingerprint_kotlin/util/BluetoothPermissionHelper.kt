package com.regula.fingerprint_kotlin.util

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.regula.fingerprint_kotlin.util.PermissionsUtil.BLE_PERMISSIONS


object BluetoothPermissionHelper {


    @RequiresApi(api = Build.VERSION_CODES.S)
    val ANDROID_12_BLE_PERMISSIONS = arrayOf<String>(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )

    fun isPermissionsGranted(activity: Activity?): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ContextCompat.checkSelfPermission(
            activity!!,
            ANDROID_12_BLE_PERMISSIONS[0]
        ) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
            activity,
            ANDROID_12_BLE_PERMISSIONS[1]
        ) == PackageManager.PERMISSION_GRANTED else ContextCompat.checkSelfPermission(
            activity!!,
            BLE_PERMISSIONS[0]
        ) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
            activity,
            BLE_PERMISSIONS[1]
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestBlePermissions(activity: Activity?, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ActivityCompat.requestPermissions(
            activity!!,
            ANDROID_12_BLE_PERMISSIONS,
            requestCode
        ) else ActivityCompat.requestPermissions(
            activity!!, BLE_PERMISSIONS, requestCode
        )
    }
}
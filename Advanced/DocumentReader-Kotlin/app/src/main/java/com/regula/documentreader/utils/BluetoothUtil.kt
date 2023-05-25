package com.regula.documentreader.util

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.provider.Settings
import com.regula.documentreader.util.BluetoothPermissionHelper.isBlePermissionDenied
import com.regula.documentreader.util.BluetoothPermissionHelper.isBluetoothEnabled
import com.regula.documentreader.util.BluetoothPermissionHelper.isLocationServiceEnabled


object BluetoothUtil {
    const val REQUEST_ENABLE_LOCATION = 196
    const val REQUEST_ENABLE_BT = 197
    const val PERMISSIONS_ACCESS_COARSE_LOCATION = 198

    fun requestEnableBle(activity: Activity?) {
        if (activity == null) return
        val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        activity.startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
    }

    fun requestEnableLocationService(activity: Activity?) {
        if (activity == null) return
        val myIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        activity.startActivityForResult(myIntent, REQUEST_ENABLE_LOCATION)
    }

    fun isPermissionsGranted(activity: Activity?): Boolean {
        if (!isBluetoothEnabled(activity)) {
            requestEnableBle(activity)
            return false
        }
        if (!isLocationServiceEnabled(activity)) {
            requestEnableLocationService(activity)
            return false
        }
        if (isBlePermissionDenied(activity)) {
            activity?.let { PermissionsHelper.requestLocationPermission(it) }
            return false
        }
        return true
    }
}
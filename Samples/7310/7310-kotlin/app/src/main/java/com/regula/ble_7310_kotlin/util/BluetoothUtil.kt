package com.regula.ble_7310_kotlin.util

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import com.regula.common.utils.PermissionUtil
import com.regula.documentreader.api.internal.permission.BluetoothPermissionHelper.*
import com.regula.documentreader.api.internal.permission.BluetoothSettingsHelper.isBluetoothEnabled
import com.regula.documentreader.api.internal.permission.BluetoothSettingsHelper.isLocationServiceEnabled


object BluetoothUtil {
    const val REQUEST_ENABLE_LOCATION = 196
    const val REQUEST_ENABLE_BT = 197
    const val PERMISSIONS_ACCESS_COARSE_LOCATION = 198

    private fun requestEnableBle(activity: Activity?) {
        if (activity == null) return
        val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        activity.startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
    }

    private fun requestEnableLocationService(activity: Activity?) {
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
            activity?.let { requestLocationPermission(it) }
            return false
        }
        return true
    }

    private fun isBlePermissionDenied(activity: Activity?): Boolean {
        return if (activity == null) false else PermissionUtil.isPermissionsDenied(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) || !PermissionUtil.isPermissionGranted(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private fun requestLocationPermission(activity: Activity) {
        when (getPermissionStatus(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
            DENIED -> ActivityCompat.requestPermissions(
                activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                BLE_ACCESS_PERMISSION
            )
            BLOCKED_OR_NEVER_ASKED -> AlertDialog.Builder(activity)
                .setTitle("Permissions denied")
                .setMessage("Permissions denied for app. Open settings to provide permissions.")
                .setNegativeButton("cancel", null)
                .setPositiveButton(
                    "Settings"
                ) { _, _ ->
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri: Uri = Uri.fromParts("package", activity.packageName, null)
                    intent.data = uri
                    activity.startActivity(intent)
                }
                .create()
                .show()
            GRANTED -> {
                TODO()
            }
        }
    }
}
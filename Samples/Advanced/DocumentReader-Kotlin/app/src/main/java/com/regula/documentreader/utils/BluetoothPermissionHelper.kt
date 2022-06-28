package com.regula.documentreader.util

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.provider.Settings
import com.regula.common.utils.PermissionUtil


object BluetoothPermissionHelper {
    const val INTENT_REQUEST_ENABLE_LOCATION = 196
    const val INTENT_REQUEST_ENABLE_BLUETOOTH = 197
    fun isBluetoothEnabled(activity: Activity?): Boolean {
        if (activity == null) return false
        val bluetoothManager =
            activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                ?: return false
        val adapter = bluetoothManager.adapter
        return adapter != null && adapter.isEnabled
    }

    fun isLocationServiceEnabled(activity: Activity?): Boolean {
        if (activity == null) return false
        val locationManager =
            activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var gps_enabled = false
        var network_enabled = false
        try {
            gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (ignored: Exception) {
        }
        try {
            network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (ignored: Exception) {
        }
        return gps_enabled || network_enabled
    }

    fun isBlePermissionDenied(activity: Activity?): Boolean {
        return if (activity == null) false else PermissionUtil.isPermissionsDenied(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
                || !PermissionUtil.isPermissionGranted(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    fun requestEnableBluetooth(activity: Activity?) {
        if (activity == null) return
        val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        activity.startActivityForResult(enableIntent, INTENT_REQUEST_ENABLE_BLUETOOTH)
    }

    fun requestEnableLocationService(activity: Activity?) {
        if (activity == null) return
        val myIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        activity.startActivityForResult(myIntent, INTENT_REQUEST_ENABLE_LOCATION)
    }

    fun isBluetoothSupported(activity: Activity?): Boolean {
        return activity?.packageManager?.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
            ?: false
    }
}
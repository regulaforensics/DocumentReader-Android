package com.regula.documentreader.util

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.regula.common.utils.PermissionUtil

object BluetoothPermissionHelper {
    private const val INTENT_REQUEST_ENABLE_LOCATION = 196
    private const val INTENT_REQUEST_ENABLE_BLUETOOTH = 197
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
        ) || !PermissionUtil.isPermissionGranted(
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

    val BLE_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    @RequiresApi(api = Build.VERSION_CODES.S)
    val ANDROID_12_BLE_PERMISSIONS = arrayOf(
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
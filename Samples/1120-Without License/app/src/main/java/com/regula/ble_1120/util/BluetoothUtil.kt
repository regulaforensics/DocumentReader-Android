package com.regula.ble_1120.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class BluetoothUtil {
    companion object {
        val INTENT_REQUEST_ENABLE_LOCATION = 196
        val INTENT_REQUEST_ENABLE_BLUETOOTH = 197
    }


    fun isBluetoothEnabled(activity: Activity?): Boolean {
        return if (activity == null) {
            false
        } else {
            val bluetoothManager =
                activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            run {
                val adapter = bluetoothManager.adapter
                adapter != null && adapter.isEnabled
            }
        }
    }

    fun isLocationServiceEnabled(activity: Activity?): Boolean {
        return if (activity == null) {
            false
        } else {
            val locationManager =
                activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            var gps_enabled = false
            var network_enabled = false
            run {
                try {
                    gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                } catch (var6: Exception) {
                }
                try {
                    network_enabled =
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                } catch (var5: Exception) {
                }
                gps_enabled || network_enabled
            }
        }
    }

    fun isPermissionDenied(activity: Activity?, permission: String?): Boolean {
        return if (activity == null) {
            false
        } else {
            com.regula.common.utils.PermissionUtil.isPermissionsDenied(
                activity,
                permission!!
            ) || !com.regula.common.utils.PermissionUtil.isPermissionGranted(
                activity,
                permission
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun requestEnableBluetooth(activity: Activity?) {
        if (activity != null) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activity.startActivityForResult(enableIntent, INTENT_REQUEST_ENABLE_BLUETOOTH)
        }
    }

    fun requestEnableLocationService(activity: Activity?) {
        if (activity != null) {
            val myIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            activity.startActivityForResult(myIntent, INTENT_REQUEST_ENABLE_LOCATION)
        }
    }

    fun isBluetoothSettingsReady(activity: Activity?): Boolean {
        return if (!isBluetoothEnabled(activity)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && isPermissionDenied(activity, Manifest.permission.BLUETOOTH_CONNECT)
            ) {
                ActivityCompat.requestPermissions(
                    activity!!, arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    PermissionUtil.PERMISSIONS_BLE_ACCESS
                )
                false
            } else {
                requestEnableBluetooth(activity)
                false
            }
        } else if (!isLocationServiceEnabled(activity)) {
            requestEnableLocationService(activity)
            false
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(
                        activity!!,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        activity, arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                        PermissionUtil.PERMISSIONS_BLE_ACCESS
                    )
                    return false
                }
                if (ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        activity, arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                        PermissionUtil.PERMISSIONS_BLE_ACCESS
                    )
                    return false
                }
            } else if (ContextCompat.checkSelfPermission(
                    activity!!,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PermissionUtil.PERMISSIONS_BLE_ACCESS
                )
                return false
            }
            true
        }
    }
}
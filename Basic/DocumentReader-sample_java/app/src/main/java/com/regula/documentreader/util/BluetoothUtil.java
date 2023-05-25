package com.regula.documentreader.util;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.provider.Settings;

public class BluetoothUtil {

    public static final int REQUEST_ENABLE_LOCATION = 196;
    public static final int REQUEST_ENABLE_BT = 197;
    public static final int PERMISSIONS_ACCESS_COARSE_LOCATION = 198;

    public static void requestEnableBle(Activity activity) {
        if (activity == null)
            return;

        final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    }

    public static void requestEnableLocationService(Activity activity) {
        if (activity == null)
            return;

        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        activity.startActivityForResult(myIntent, REQUEST_ENABLE_LOCATION);
    }


    public static boolean isPermissionsGranted(Activity activity) {
        if (!BluetoothPermissionHelper.isBluetoothEnabled(activity)) {
            requestEnableBle(activity);
            return false;
        }

        if (!BluetoothPermissionHelper.isLocationServiceEnabled(activity)) {
            requestEnableLocationService(activity);
            return false;
        }

        if (BluetoothPermissionHelper.isBlePermissionDenied(activity)) {
            PermissionsHelper.requestLocationPermission(activity);
            return false;
        }

        return true;
    }
}

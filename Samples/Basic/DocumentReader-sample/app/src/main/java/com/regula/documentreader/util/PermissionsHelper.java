package com.regula.documentreader.util;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Settings;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.regula.common.utils.PermissionUtil;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class PermissionsHelper {

    public static final int BLE_ACCESS_PERMISSION = 198;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({GRANTED, DENIED, BLOCKED_OR_NEVER_ASKED})
    public @interface PermissionStatus {
    }

    public static final int GRANTED = 0;
    public static final int DENIED = 1;
    public static final int BLOCKED_OR_NEVER_ASKED = 2;

    public static void requestLocationPermission(@NonNull final Activity activity) {
        if (activity == null)
            return;

        switch (getPermissionStatus(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
            case DENIED:
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        BLE_ACCESS_PERMISSION);
                break;
            case BLOCKED_OR_NEVER_ASKED:
                new AlertDialog.Builder(activity)
                        .setTitle("Permissions denied")
                        .setMessage("Permissions denied for app. Open settings to provide permissions.")
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                                intent.setData(uri);
                                activity.startActivity(intent);
                            }
                        })
                        .create()
                        .show();
                break;
        }
    }

    @PermissionStatus
    public static int getPermissionStatus(Activity activity, String androidPermissionName) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, androidPermissionName)) {
            return DENIED;
        } else if (!PermissionUtil.isPermissionGranted(activity, androidPermissionName)) {
            return PermissionsHelper.neverAskAgainSelected(activity, androidPermissionName) ? BLOCKED_OR_NEVER_ASKED : DENIED;
        }
        return GRANTED;
    }

    public static boolean neverAskAgainSelected(final Activity activity, final String permission) {
        final boolean prevShouldShowStatus = getRatinaleDisplayStatus(activity, permission);
        boolean currShouldShowStatus = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            currShouldShowStatus = activity.shouldShowRequestPermissionRationale(permission);
        }
        return prevShouldShowStatus != currShouldShowStatus;
    }

    public static void setShouldShowStatus(final Context context, final String permission) {
        SharedPreferences genPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = genPrefs.edit();
        editor.putBoolean(permission, true);
        editor.apply();
    }

    public static boolean getRatinaleDisplayStatus(final Context context, final String permission) {
        SharedPreferences genPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        return genPrefs.getBoolean(permission, false);
    }
}


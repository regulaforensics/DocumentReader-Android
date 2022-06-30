package com.regula.documentreader.util
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build
import android.preference.PreferenceManager
import android.provider.Settings;

import androidx.annotation.IntDef;
import androidx.core.app.ActivityCompat;

import com.regula.common.utils.PermissionUtil;
import com.regula.documentreader.R

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

object PermissionsHelper {
    const val BLE_ACCESS_PERMISSION = 198
    const val GRANTED = 0
    const val DENIED = 1
    const val BLOCKED_OR_NEVER_ASKED = 2
    fun requestLocationPermission(activity: Activity) {
        if (activity == null) return
        when (getPermissionStatus(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
            DENIED -> ActivityCompat.requestPermissions(
                activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                BLE_ACCESS_PERMISSION
            )
            BLOCKED_OR_NEVER_ASKED -> AlertDialog.Builder(activity)
                .setTitle("Permissions denied")
                .setMessage("Permissions denied for app. Open settings to provide permissions.")
                .setNegativeButton("cancel", null)
                .setPositiveButton("Settings"
                ) { dialogInterface, i ->
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri: Uri = Uri.fromParts("package", activity.packageName, null)
                    intent.data = uri
                    activity.startActivity(intent)
                }
                .create()
                .show()
        }
    }

    @PermissionStatus
    fun getPermissionStatus(activity: Activity, androidPermissionName: String?): Int {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                androidPermissionName!!
            )
        ) {
            return DENIED
        } else if (!PermissionUtil.isPermissionGranted(activity, androidPermissionName)) {
            return if (neverAskAgainSelected(
                    activity,
                    androidPermissionName
                )
            ) BLOCKED_OR_NEVER_ASKED else DENIED
        }
        return GRANTED
    }

    fun neverAskAgainSelected(activity: Activity, permission: String?): Boolean {
        val prevShouldShowStatus = getRatinaleDisplayStatus(activity, permission)
        var currShouldShowStatus = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            currShouldShowStatus = activity.shouldShowRequestPermissionRationale(permission!!)
        }
        return prevShouldShowStatus != currShouldShowStatus
    }

    fun setShouldShowStatus(context: Context?, permission: String?) {
        val genPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = genPrefs.edit()
        editor.putBoolean(permission, true)
        editor.apply()
    }

    fun getRatinaleDisplayStatus(context: Context?, permission: String?): Boolean {
        val genPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        return genPrefs.getBoolean(permission, false)
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(GRANTED, DENIED, BLOCKED_OR_NEVER_ASKED)
    annotation class PermissionStatus
}

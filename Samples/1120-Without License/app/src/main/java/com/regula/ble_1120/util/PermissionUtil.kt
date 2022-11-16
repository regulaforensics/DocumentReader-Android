package com.regula.ble_1120.util

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings

class PermissionUtil {

    companion object {
        const val PERMISSIONS_BLE_ACCESS = 1

        fun respondToPermissionRequest(context: Activity,
                                       permission: String,
                                       grantResults: IntArray,
                                       permissionGrantedFunc: () -> Unit,
                                       permissionRejectedFunc: (() -> Unit)?) {

            if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                permissionGrantedFunc()
                return
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    !context.shouldShowRequestPermissionRationale(permission)) {
                AlertDialog.Builder(context)
                    .setTitle("Permissions denied")
                    .setMessage("Permissions denied for app. Open settings to provide permissions.")
                    .setNegativeButton("cancel", null)
                    .setPositiveButton(
                        "Settings"
                    ) { _, _ ->
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri: Uri = Uri.fromParts("package", context.packageName, null)
                        intent.data = uri
                        context.startActivity(intent)
                    }
                    .create()
                    .show()

            }
            permissionRejectedFunc?.invoke()
        }
    }
}